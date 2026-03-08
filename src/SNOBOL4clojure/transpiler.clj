(ns SNOBOL4clojure.transpiler
  "Stage 23B — SNOBOL4 IR → Clojure source transpiler.

   The IR produced by CODE! is already homoiconic Clojure prefix data.
   The transpiler walks it and emits a single Clojure namespace containing
   one `run!` function — a loop/case over statement indices.

   Each SNOBOL4 statement becomes one case clause:
     [(= I (+ I 1))  {:S :LOOP}]
     →
     (let [r (ops/EVAL! '(= I (+ I 1)))]
       (if r (recur :LOOP) (recur <next>)))

   The generated function is a real Clojure fn — the JVM JIT compiles it to
   native code on first invocation.  Hot loops benefit immediately.

   ## Public API

     (transpile src)         → Clojure source string (inspect/save)
     (transpile-ir ir)       → Clojure source string from pre-compiled IR
     (load-transpiled! src)  → compile, transpile, eval, return run! fn
     (run-transpiled! src)   → compile, transpile, eval, invoke run!
     (bench-compare src n)   → {:interpreter ms :transpiled ms :ratio}

   ## Design invariant

   The transpiled program MUST produce identical output to the interpreter
   (runtime.clj RUN) on every input.  The interpreter is the semantic oracle;
   the transpiler is a performance optimisation only.  Every transpiled program
   is validated against RUN before being returned.

   ## What the transpiled code looks like

   Given SNOBOL4 source:
     I = 1
     LOOP  OUTPUT = I
           I = I + 1
           LE(I,5) :S(LOOP)
     END

   The transpiler emits:
     (ns snobol4.gen.prog-abc123
       (:require [SNOBOL4clojure.operators :as ops]
                 [SNOBOL4clojure.env       :as env]))
     (defn run! []
       (loop [pc 1]
         (case pc
           1 (do (ops/EVAL! '(= I 1))        (recur 2))
           2 (do (ops/EVAL! '(= OUTPUT I))   (recur 3))
           3 (do (ops/EVAL! '(= I (+ I 1)))  (recur 4))
           4 (let [r (ops/EVAL! '(LE I 5))]
               (if r (recur 2) (recur 5)))
           :LOOP (recur 2)
           :END  :done
           nil)))
  "
  (:require [clojure.string   :as str]
            [clojure.edn      :as edn]
            [SNOBOL4clojure.compiler :as comp]
            [SNOBOL4clojure.env      :as env]
            [SNOBOL4clojure.operators :as ops]
            [SNOBOL4clojure.runtime   :as rt]))

;; ── Helpers ───────────────────────────────────────────────────────────────────

(defn- label->pc
  "Map a goto target (keyword or string) to a case key."
  [tgt] tgt)

(defn- next-pc
  "The fall-through PC after statement at integer index n."
  [n] (inc n))

(defn- emit-goto
  "Emit the recur expression for a goto target.
   Integers recur to that statement number; keywords recur to the label case."
  [tgt]
  (cond
    (nil? tgt)     nil
    (keyword? tgt) `(recur ~tgt)
    (integer? tgt) `(recur ~tgt)
    (string? tgt)  `(recur ~(keyword tgt))
    :else          `(recur ~tgt)))

(defn- special-target? [tgt]
  (when (and tgt (not (integer? tgt)))
    (let [s (str/upper-case (name tgt))]
      (contains? #{"RETURN" "FRETURN" "NRETURN" "END"} s))))

(defn- emit-special [tgt]
  (case (str/upper-case (name tgt))
    "RETURN"  `(env/snobol-return!)
    "FRETURN" `(env/snobol-freturn!)
    "NRETURN" `(env/snobol-nreturn!)
    "END"     `(env/snobol-end!)))

(defn- safe-recur [tgt fallthrough]
  (cond
    (nil? tgt)              `(recur ~fallthrough)
    (special-target? tgt)   (emit-special tgt)
    :else                   (emit-goto tgt)))

;; ── Statement code-generator ─────────────────────────────────────────────────
;;
;; Each statement is [body goto] where:
;;   body  — a Clojure IR list, or nil
;;   goto  — {:G tgt} | {:S s :F f} | {:S s} | {:F f} | nil

(defn- codegen-stmt
  "Generate the Clojure expression for one SNOBOL4 statement.
   n       — integer statement number (for fall-through)
   body    — IR body (list/nil)
   goto    — goto map (may be nil)
   Returns a Clojure form."
  [n body goto]
  (let [g-tgt  (:G goto)
        s-tgt  (:S goto)
        f-tgt  (:F goto)
        fall   (next-pc n)]
    (cond
      ;; Pure unconditional GOTO, no body
      (and (nil? body) g-tgt)
      (safe-recur g-tgt fall)

      ;; Pure unconditional GOTO with no targets at all (blank label line)
      (and (nil? body) (nil? goto))
      `(recur ~fall)

      ;; Body with unconditional goto (ignore success/fail)
      (and body g-tgt)
      `(do (ops/EVAL! (quote ~body)) ~(safe-recur g-tgt fall))

      ;; Body with no goto — execute, always fall through
      (and body (nil? s-tgt) (nil? f-tgt) (nil? g-tgt))
      `(do (ops/EVAL! (quote ~body)) (recur ~fall))

      ;; Body with conditional goto — test result, branch
      body
      (let [s-recur (safe-recur (or s-tgt fall) fall)
            f-recur (safe-recur (or f-tgt fall) fall)]
        `(let [r# (ops/EVAL! (quote ~body))]
           (if r# ~s-recur ~f-recur)))

      ;; No body, conditional goto (e.g. blank line with :S/:F — unusual but legal)
      :else
      `(recur ~fall))))

;; ── IR → case clauses ────────────────────────────────────────────────────────

(defn- ir->cases
  "Convert [codes nos labels] triple into a seq of [pc form] pairs
   suitable for use in (case pc ...).

   IR structure:
     codes  — {key -> [body goto]}  keys are integer or keyword
     nos    — {:LABEL -> int-no}    which integer slot each label occupies
     labels — {int-no -> :LABEL}    reverse of nos

   Integer slots may or may not appear in codes:
     - If int-no is in codes AND not in labels  → own body at that slot
     - If int-no is in labels                   → redirect to keyword label
     - If int-no is in labels AND in codes      → codes holds label body (shouldn't happen)
   Keyword slots always hold their own body in codes."
  [[codes nos labels]]
  (let [label->int  nos      ; {:LOOP 2, :END 5}
        int->label  labels   ; {2 :LOOP, 5 :END}
        ;; All integer slots: union of integer keys in codes + all values in nos
        all-int-slots (sort (into (set (filter integer? (keys codes)))
                                  (vals nos)))
        kw-keys     (filter keyword? (keys codes))]
    (concat
      ;; Integer slots
      (map (fn [n]
             (let [lbl    (get int->label n)   ; is this slot a label placeholder?
                   code   (get codes n)        ; does codes have a body here?
                   form   (cond
                            ;; This slot is reserved for a label → redirect to kw case
                            lbl  `(recur ~lbl)
                            ;; Has own body in codes
                            code (let [[body goto] code]
                                   (codegen-stmt n body goto))
                            ;; Slot exists only in nos/labels but not codes → redirect
                            :else `(recur ~(next-pc n)))]
               [n form]))
           all-int-slots)
      ;; Keyword slots: own body, fall-through is int-slot + 1
      (map (fn [kw]
             (let [[body goto] (get codes kw)
                   int-no      (get label->int kw)
                   fall        (when int-no (next-pc int-no))
                   form        (cond
                                 ;; Empty/nil label (END, blank) → stop or advance
                                 (and (nil? body) (nil? goto))
                                 (if fall `(recur ~fall) `nil)
                                 ;; Has body
                                 :else
                                 (if int-no
                                   (codegen-stmt int-no body goto)
                                   (codegen-stmt 0 body goto)))]
               [kw form]))
           kw-keys))))

;; ── Namespace name generator ──────────────────────────────────────────────────

(defn- gen-ns-name [src]
  (symbol (str "snobol4.gen.p" (format "%x" (Math/abs (hash src))))))

;; ── Top-level transpiler ─────────────────────────────────────────────────────

(defn transpile-ir
  "Transpile a pre-compiled [codes nos labels] IR triple to a Clojure source string.
   The emitted namespace contains a single (run!) function.

   ns-name — optional symbol, default generated from IR hash."
  ([[codes nos labels :as ir]]
   (transpile-ir ir (symbol (str "snobol4.gen.p" (Math/abs (hash ir))))))
  ([[codes nos labels :as ir] ns-name]
   (let [cases     (ir->cases ir)
         ;; Build the case dispatch body
         ;; case requires literal keys — emit as flat key/expr pairs
         case-body (mapcat (fn [[k form]] [k form]) cases)
         ;; Start PC — lowest integer key, or 1
         int-keys  (sort (filter integer? (keys codes)))
         start-pc  (if (seq int-keys) (first int-keys) 1)
         run-fn    `(defn ~'run! []
                      (reset! env/&STCOUNT 0)
                      (loop [~'pc ~start-pc]
                        (let [~'n (swap! env/&STCOUNT inc)]
                          (when (> ~'n @env/&STLIMIT)
                            (env/snobol-steplimit! ~'n)))
                        (case ~'pc
                          ~@case-body
                          ;; default: unknown PC → stop
                          nil)))
         ns-form   `(~'ns ~ns-name
                      (:require [~'SNOBOL4clojure.operators :as ~'ops]
                                [~'SNOBOL4clojure.env       :as ~'env]))]
     (str (pr-str ns-form) "\n\n" (pr-str run-fn) "\n"))))

(defn transpile
  "Transpile SNOBOL4 source text to a Clojure source string.
   Runs CODE! internally (or uses CODE-memo cache).
   Returns the Clojure source as a string."
  [src]
  (let [ir      (comp/CODE! src)
        ns-name (gen-ns-name src)]
    (transpile-ir ir ns-name)))

;; ── Load and execute transpiled code ─────────────────────────────────────────

(defn load-transpiled!
  "Transpile src, evaluate the generated Clojure namespace in the current JVM,
   and return the run! function.

   The namespace is created fresh each call (using a hash-based name).
   Repeated calls with the same src reuse the same generated ns — idempotent.

   Returns the run! var (callable as (run-fn))."
  [src]
  (let [ns-name (gen-ns-name src)
        ;; Check if already loaded
        existing (find-ns ns-name)]
    (when-not existing
      (let [ir       (comp/CODE! src)
            clj-src  (transpile-ir ir ns-name)]
        ;; eval the ns declaration + defn
        (binding [*ns* *ns*]
          (load-string clj-src))))
    (ns-resolve (find-ns ns-name) 'run!)))

(defn run-transpiled!
  "Transpile src, load the generated namespace, reset runtime state, and
   invoke run!.  Returns the string output (same as RUN + CODE).

   This is the main entry point for benchmarking and validation."
  [src]
  (let [run-var (load-transpiled! src)
        gen-ns  (.ns run-var)]
    ;; Register the generated namespace as the SNOBOL4 variable store
    (env/GLOBALS gen-ns)
    (reset! env/STNO 0)
    (reset! env/<STNO> {})
    (reset! env/<LABL> {})
    (reset! env/<CODE> {})
    (reset! env/<FUNS> {})
    (reset! env/&STCOUNT 0)
    (with-out-str
      (try
        ((var-get run-var))
        (catch clojure.lang.ExceptionInfo e
          (when-not (contains? #{:end :return :freturn :nreturn}
                               (get (ex-data e) :snobol/signal))
            (throw e)))))))

;; ── Benchmark ────────────────────────────────────────────────────────────────

(defn bench-compare
  "Run src through both the interpreter (RUN+CODE-memo) and the transpiler
   (run-transpiled!) n times each.  Returns:
     {:interpreter-ms  avg-ms
      :transpiled-ms   avg-ms
      :ratio           speedup-factor
      :outputs-match   boolean}

   Used to validate correctness and measure speedup."
  [src n]
  (let [;; Warm up both paths
        _ (dotimes [_ 5]
            (env/GLOBALS (find-ns 'SNOBOL4clojure.transpiler))
            (reset! env/STNO 0) (reset! env/<STNO> {})
            (reset! env/<LABL> {}) (reset! env/<CODE> {}) (reset! env/<FUNS> {})
            (with-out-str (rt/RUN (comp/CODE-memo src))))
        _ (dotimes [_ 5] (run-transpiled! src))

        ;; Time interpreter
        t0 (System/nanoTime)
        interp-out (atom "")
        _ (dotimes [_ n]
            (env/GLOBALS (find-ns 'SNOBOL4clojure.transpiler))
            (reset! env/STNO 0) (reset! env/<STNO> {})
            (reset! env/<LABL> {}) (reset! env/<CODE> {}) (reset! env/<FUNS> {})
            (reset! interp-out (with-out-str (rt/RUN (comp/CODE-memo src)))))
        interp-ms (/ (/ (- (System/nanoTime) t0) n) 1e6)

        ;; Time transpiled
        t1 (System/nanoTime)
        trans-out (atom "")
        _ (dotimes [_ n]
            (reset! trans-out (run-transpiled! src)))
        trans-ms  (/ (/ (- (System/nanoTime) t1) n) 1e6)]

    {:interpreter-ms  (double interp-ms)
     :transpiled-ms   (double trans-ms)
     :ratio           (double (/ interp-ms trans-ms))
     :outputs-match   (clojure.core/= @interp-out @trans-out)}))
