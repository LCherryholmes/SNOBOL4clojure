(ns SNOBOL4clojure.vm
  "Stage 23C — Clojure Stack Machine (CSM)

   The interpreter (runtime.clj RUN) dispatches via maps and keyword lookups
   on every statement.  The transpiler (transpiler.clj) compiles to Clojure
   source that the JVM JIT eventually optimises — but each statement still
   calls EVAL! which re-walks the IR list every time.

   The CSM flattens the [codes nos labels] IR into a dense integer-indexed
   bytecode vector and dispatches via a single case expression on an integer
   opcode.  This eliminates:
     - Map lookups on every goto
     - Keyword->integer resolution on every branch
     - Seq walking overhead inside EVAL! for simple IR nodes

   ## Bytecode format

   The bytecode is a Clojure vector of instruction records.  Each instruction
   is a map:

     {:op   <opcode int>
      :body <EVAL! argument, or nil>
      :s    <success jump target int, or nil>
      :f    <failure jump target int, or nil>}

   Opcodes (integer constants):
     0  HALT    — program end (maps to END / fall-off)
     1  EXEC    — eval body, always fall through to pc+1
     2  EXEC-S  — eval body; success → :s, failure → pc+1
     3  EXEC-F  — eval body; success → pc+1, failure → :f
     4  EXEC-SF — eval body; success → :s, failure → :f
     5  JUMP    — unconditional goto :s (body ignored)
     6  SIGNAL  — throw snobol/signal stored in :body

   ## Compilation

     (compile-ir ir)    → bytecode vector
     (compile-src src)  → bytecode vector (uses CODE-memo cache)

   ## Execution

     (run-vm! bc)       → nil (output via *out*)

   ## Public benchmark / validation API

     (bench-compare-vm src n) → {:interpreter-ms :vm-ms :ratio :outputs-match}

   ## Design invariant

   The CSM must produce identical output to the interpreter (runtime.clj RUN)
   on every program.  The interpreter is the semantic oracle; the VM is a
   performance optimisation only.
  "
  (:require [clojure.string          :as str]
            [SNOBOL4clojure.compiler :as comp]
            [SNOBOL4clojure.env      :as env
             :refer [&STCOUNT &STLIMIT snobol-steplimit! snobol-end!
                     snobol-return! snobol-freturn! snobol-nreturn!]]
            [SNOBOL4clojure.operators :refer [EVAL!]]
            [SNOBOL4clojure.runtime   :as rt]))

;; ── Opcode constants ─────────────────────────────────────────────────────────

(def ^:const OP-HALT    0)
(def ^:const OP-EXEC    1)   ; eval body, fall through
(def ^:const OP-EXEC-S  2)   ; eval; success->s, failure->pc+1
(def ^:const OP-EXEC-F  3)   ; eval; success->pc+1, failure->f
(def ^:const OP-EXEC-SF 4)   ; eval; success->s, failure->f
(def ^:const OP-JUMP    5)   ; unconditional goto s
(def ^:const OP-SIGNAL  6)   ; throw snobol/signal in body

;; ── Special-target detection ──────────────────────────────────────────────────

(def ^:private special-names
  #{"RETURN" "FRETURN" "NRETURN" "END"})

(defn- special? [tgt]
  (and tgt (not (integer? tgt))
       (contains? special-names (str/upper-case (name tgt)))))

(defn- signal-for [tgt]
  (case (str/upper-case (name tgt))
    "END"     :end
    "RETURN"  :return
    "FRETURN" :freturn
    "NRETURN" :nreturn))

;; ── IR → bytecode compiler ────────────────────────────────────────────────────
;;
;; We make two passes:
;;   Pass 1: assign a bytecode PC to every IR slot (integer or keyword)
;;   Pass 2: emit instructions with resolved integer jump targets

(defn- assign-pcs
  "Pass 1: build a map from IR key (integer or keyword) to bytecode PC.
   Integer slots that are label placeholders map to the same PC as the keyword."
  [[codes nos labels]]
  ;; All integer slots in order
  (let [all-int   (sort (into (set (filter integer? (keys codes)))
                              (vals nos)))
        kw-slots  (filter keyword? (keys codes))]
    ;; Assign each integer slot a unique PC, unless it's a label placeholder
    ;; (in which case it will redirect to the kw's PC — handled in pass 2)
    (loop [slots all-int
           pc    0
           m     {}]
      (if (empty? slots)
        ;; Assign keyword slots PCs equal to their integer slot PCs
        ;; (because the integer slot will redirect there)
        (reduce (fn [m kw]
                  (let [int-no (get nos kw)
                        kw-pc  (if int-no (get m int-no) pc)]
                    (assoc m kw kw-pc)))
                m kw-slots)
        (recur (rest slots) (inc pc) (assoc m (first slots) pc))))))

(defn compile-ir
  "Compile a [codes nos labels] triple to a flat bytecode vector.
   Returns a vector of instruction maps {:op :body :s :f}."
  [[codes nos labels :as ir]]
  (let [pc-map   (assign-pcs ir)
        ;; Total number of integer slots = total instructions before HALT
        all-int  (sort (into (set (filter integer? (keys codes)))
                             (vals nos)))
        halt-pc  (count all-int)

        ;; Resolve a goto target (keyword, integer, special) to a bytecode PC
        resolve-tgt (fn [tgt]
                      (cond
                        (nil? tgt)        nil
                        (special? tgt)    :signal
                        (integer? tgt)    (get pc-map tgt)
                        (keyword? tgt)    (get pc-map tgt)
                        :else             (get pc-map tgt)))

        ;; Build instruction for one IR slot
        make-instr (fn [key]
                     (let [lbl   (get labels key)    ; is this int slot a label placeholder?
                           code  (if lbl
                                   (get codes lbl)   ; get body from keyword slot
                                   (get codes key))  ; own body
                           [raw-body raw-goto] (if code code [nil nil])
                           body  raw-body
                           goto  raw-goto
                           g-tgt (:G goto)
                           s-tgt (:S goto)
                           f-tgt (:F goto)
                           fall  (+ (get pc-map key) 1)

                           ;; Check for END/RETURN signals
                           s-sig (when (special? s-tgt) (signal-for s-tgt))
                           f-sig (when (special? f-tgt) (signal-for f-tgt))
                           g-sig (when (special? g-tgt) (signal-for g-tgt))]
                       (cond
                         ;; Empty label (END etc) — HALT
                         (and (nil? body) (nil? g-tgt) (nil? s-tgt) (nil? f-tgt)
                              (or lbl (nil? (get codes key))))
                         {:op OP-HALT :body nil :s nil :f nil}

                         ;; Unconditional goto with no body
                         (and (nil? body) g-tgt)
                         (if g-sig
                           {:op OP-SIGNAL :body g-sig :s nil :f nil}
                           {:op OP-JUMP :body nil :s (resolve-tgt g-tgt) :f nil})

                         ;; Body with unconditional goto
                         (and body g-tgt)
                         (if g-sig
                           {:op OP-EXEC :body body :s fall :f fall}  ; signal after?
                           {:op OP-JUMP :body body :s (resolve-tgt g-tgt) :f nil})

                         ;; Body with both S and F
                         (and body s-tgt f-tgt)
                         {:op OP-EXEC-SF
                          :body body
                          :s    (if s-sig :signal (resolve-tgt s-tgt))
                          :f    (if f-sig :signal (resolve-tgt f-tgt))
                          :s-sig s-sig
                          :f-sig f-sig}

                         ;; Body with S only
                         (and body s-tgt)
                         {:op OP-EXEC-S
                          :body body
                          :s    (if s-sig :signal (resolve-tgt s-tgt))
                          :f    fall
                          :s-sig s-sig}

                         ;; Body with F only
                         (and body f-tgt)
                         {:op OP-EXEC-F
                          :body body
                          :s    fall
                          :f    (if f-sig :signal (resolve-tgt f-tgt))
                          :f-sig f-sig}

                         ;; Body, no goto
                         body
                         {:op OP-EXEC :body body :s fall :f fall}

                         ;; No body, no goto — skip
                         :else
                         {:op OP-EXEC :body nil :s fall :f fall})))]

    ;; Emit bytecode vector
    (let [instrs (mapv make-instr all-int)]
      (conj instrs {:op OP-HALT :body nil :s nil :f nil}))))

(defn compile-src
  "Compile SNOBOL4 source to bytecode.  Uses CODE-memo for IR caching."
  [src]
  (compile-ir (comp/CODE! src)))

;; ── VM executor ──────────────────────────────────────────────────────────────

(defn run-vm!
  "Execute a bytecode vector produced by compile-ir.
   Side effects: writes to *out* (OUTPUT), mutates SNOBOL4 variables.
   Returns nil.  Throws ExceptionInfo for :step-limit."
  [^clojure.lang.PersistentVector bc]
  (reset! &STCOUNT 0)
  (let [bc-len (count bc)]
    (loop [pc 0]
      (when (< pc bc-len)
        (let [{:keys [op body s f s-sig f-sig]} (bc pc)]
          (let [n (swap! &STCOUNT inc)]
            (when (> n @&STLIMIT)
              (snobol-steplimit! n)))
          (case (int op)
            0 nil  ; HALT
            1      ; EXEC — eval, always advance
            (do (when body (EVAL! body))
                (recur s))
            2      ; EXEC-S — success->s, failure->f(=fall)
            (let [r (when body (EVAL! body))]
              (if r
                (if s-sig (throw (ex-info (name s-sig) {:snobol/signal s-sig}))
                    (recur s))
                (recur f)))
            3      ; EXEC-F — success->s(=fall), failure->f
            (let [r (when body (EVAL! body))]
              (if r
                (recur s)
                (if f-sig (throw (ex-info (name f-sig) {:snobol/signal f-sig}))
                    (recur f))))
            4      ; EXEC-SF — success->s, failure->f
            (let [r (when body (EVAL! body))]
              (if r
                (if s-sig (throw (ex-info (name s-sig) {:snobol/signal s-sig}))
                    (recur s))
                (if f-sig (throw (ex-info (name f-sig) {:snobol/signal f-sig}))
                    (recur f))))
            5      ; JUMP — unconditional goto s (body may have been eval'd)
            (do (when body (EVAL! body))
                (recur (int s)))
            6      ; SIGNAL — throw :snobol/signal
            (throw (ex-info (name body) {:snobol/signal body}))
            ;; default
            nil))))))

(defn run-program!
  "Reset runtime state and run src through the VM.
   Returns captured stdout string."
  [src]
  (env/GLOBALS (find-ns 'SNOBOL4clojure.vm))
  (reset! env/STNO 0)
  (reset! env/<STNO> {})
  (reset! env/<LABL> {})
  (reset! env/<CODE> {})
  (reset! env/<FUNS> {})
  (reset! &STCOUNT 0)
  (let [bc (compile-src src)]
    (with-out-str
      (try
        (run-vm! bc)
        (catch clojure.lang.ExceptionInfo e
          (when-not (contains? #{:end :return :freturn :nreturn}
                               (get (ex-data e) :snobol/signal))
            (throw e)))))))

;; ── Benchmark ────────────────────────────────────────────────────────────────

(defn bench-compare-vm
  "Compare interpreter (RUN+CODE-memo) vs VM (compile-ir+run-vm!) on src.
   Returns {:interpreter-ms :vm-ms :ratio :outputs-match}."
  [src n]
  (let [bc (compile-src src)

        interp-reset! (fn []
                        (env/GLOBALS (find-ns 'SNOBOL4clojure.vm))
                        (reset! env/STNO 0) (reset! env/<STNO> {})
                        (reset! env/<LABL> {}) (reset! env/<CODE> {})
                        (reset! env/<FUNS> {}))

        run-interp (fn []
                     (interp-reset!)
                     (with-out-str
                       (try (rt/RUN (comp/CODE-memo src))
                            (catch clojure.lang.ExceptionInfo e
                              (when-not (contains? #{:end :return :freturn :nreturn}
                                                   (get (ex-data e) :snobol/signal))
                                (throw e))))))
        run-vm     (fn []
                     (interp-reset!)
                     (with-out-str
                       (try (run-vm! bc)
                            (catch clojure.lang.ExceptionInfo e
                              (when-not (contains? #{:end :return :freturn :nreturn}
                                                   (get (ex-data e) :snobol/signal))
                                (throw e))))))]

    ;; Warm up
    (dotimes [_ 20] (run-interp))
    (dotimes [_ 20] (run-vm))

    (let [i-out (atom "") v-out (atom "")
          t0 (System/nanoTime)
          _  (dotimes [_ n] (reset! i-out (run-interp)))
          i-ms (/ (/ (- (System/nanoTime) t0) n) 1e6)
          t1 (System/nanoTime)
          _  (dotimes [_ n] (reset! v-out (run-vm)))
          v-ms (/ (/ (- (System/nanoTime) t1) n) 1e6)]
      {:interpreter-ms (double i-ms)
       :vm-ms          (double v-ms)
       :ratio          (double (/ i-ms v-ms))
       :outputs-match  (clojure.core/= @i-out @v-out)})))
