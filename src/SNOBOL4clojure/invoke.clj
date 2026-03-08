(ns SNOBOL4clojure.invoke
  "SNOBOL4 expression evaluator: INVOKE, EVAL!, EVAL.
  INVOKE dispatches operator symbols to their implementations.
  EVAL! walks a compiled expression tree, evaluating at runtime.
  EVAL parses a source string then calls EVAL!."
  (:require [SNOBOL4clojure.env       :refer
             [ε η equal num $$ snobol-set!]]
            [SNOBOL4clojure.functions :refer
             [ASCII REMDR INTEGER REAL STRING SIZE TRIM DUPL REVERSE LPAD RPAD REPLACE]]
            [SNOBOL4clojure.match-api :refer [SEARCH]]
            [SNOBOL4clojure.operators :refer
             [= ? ?= & at + - sharp / * % ! ** $ . tilde |
              EQ NE LE LT GE GT]]
            [SNOBOL4clojure.patterns  :refer
             [ANY BREAK BREAKX NOTANY SPAN ARBNO FENCE
              LEN POS RPOS RTAB TAB FAIL]]
            [SNOBOL4clojure.emitter   :refer [emitter]]
            [SNOBOL4clojure.grammar   :refer [parse-expression]]
            [SNOBOL4clojure.trace     :refer [trace! stoptr! clear-all-traces!]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── INVOKE ────────────────────────────────────────────────────────────────────
(declare EVAL!)

(defn INVOKE [op & args]
  (case op
    |       (apply | args)
    $       (apply $ args)
    .       (apply . args)
    LEN     (LEN    (first args))
    POS     (POS    (first args))
    RPOS    (RPOS   (first args))
    ANY     (ANY    (first args))
    BREAK   (BREAK  (first args))
    BREAKX  (BREAKX (first args))
    NOTANY  (NOTANY (first args))
    SPAN    (SPAN   (first args))
    FENCE   (if (seq args) (FENCE (first args)) (FENCE))
    EQ      (EQ     (first args) (second args))
    NE      (NE     (first args) (second args))
    LE      (LE     (first args) (second args))
    LT      (LT     (first args) (second args))
    GE      (GE     (first args) (second args))
    GT      (GT     (first args) (second args))
    FAIL    FAIL
    ;; Arithmetic — coerce to SNOBOL numeric type
    +       (let [ns (map num args)]
              (if (every? #(== (Math/floor %) %) ns)
                (long (apply clojure.core/+' ns))
                (apply clojure.core/+ ns)))
    -       (let [ns (map num args)]
              (if (every? #(== (Math/floor %) %) ns)
                (long (apply clojure.core/-' ns))
                (apply clojure.core/- ns)))
    *       (let [ns (map num args)]
              (if (every? #(== (Math/floor %) %) ns)
                (long (apply clojure.core/*' ns))
                (apply clojure.core/* ns)))
    /       (let [ns (map num args)]
              (if (every? integer? ns)
                (apply quot ns)
                (apply clojure.core// ns)))
    ?       (let [[s p] args] (SEARCH (str s) p))
    =       (let [[N r] args]
              (when-not (clojure.core/contains? #{'OUTPUT 'TERMINAL 'INPUT} N)
                (snobol-set! N r))
              (when (clojure.core/= N 'OUTPUT)   (println r))
              (when (clojure.core/= N 'TERMINAL) (println r))
              r)
    ?=      (let [[n _p R] args, r (EVAL! R)]
              (snobol-set! n r) r)
    DEFINE  (let [[proto] args
                  spec    (apply vector (re-seq #"[0-9A-Z_a-z]+" proto))
                  fname   (first spec)
                  params  (subvec spec 1)
                  locals  (subvec spec (inc (count params)) (count spec))
                  f-sym   (symbol fname)
                  entry   (keyword (clojure.string/upper-case fname))]
              (letfn [(the-fn [& call-args]
                        ;; Save locals and params, bind call args
                        (let [saved-params (mapv #($$ (symbol %)) params)
                              saved-locals (mapv #($$ (symbol %)) locals)]
                          (doseq [i (range (count params))]
                            (snobol-set! (symbol (params i))
                                         (nth call-args i ε)))
                          (doseq [l locals]
                            (snobol-set! (symbol l) ε))
                          (snobol-set! f-sym ε)
                          (when-let [run-fn (ns-resolve 'SNOBOL4clojure.runtime 'RUN)]
                            (try ((var-get run-fn) entry)
                                 (catch clojure.lang.ExceptionInfo e
                                   (when-not (= :return (get (ex-data e) :snobol/signal))
                                     (throw e)))))
                          (let [result ($$ f-sym)]
                            ;; Restore saved params and locals
                            (doseq [i (range (count params))]
                              (snobol-set! (symbol (params i)) (saved-params i)))
                            (doseq [i (range (count locals))]
                              (snobol-set! (symbol (locals i)) (saved-locals i)))
                            (snobol-set! f-sym the-fn)
                            result)))]
                (snobol-set! f-sym the-fn)
                ε))
    REPLACE (let [[s1 s2 s3] args] (REPLACE s1 s2 s3))
    ASCII   (ASCII   (first args))
    REMDR   (REMDR   (first args) (second args))
    INTEGER (INTEGER (first args))
    REAL    (REAL    (first args))
    STRING  (STRING  (first args))
    SIZE    (SIZE    (first args))
    TRIM    (TRIM    (first args))
    DUPL    (DUPL    (first args) (second args))
    tilde   (let [v (first args)]       ; ~ negates success/failure
              (if (nil? v) ε nil))
    LPAD    (LPAD    (first args) (second args))
    RPAD    (RPAD    (first args) (second args))
    quote   ($$ (second op))
    ;; ── Trace functions ───────────────────────────────────────────────────────
    TRACE   (let [[name type label user-fn] args]
              (trace! (str name) (str type) label user-fn))
    trace   (let [[name type label user-fn] args]
              (trace! (str name) (str type) label user-fn))
    STOPTR  (let [[name type] args]
              (stoptr! (str name) (str type)))
    stoptr  (let [[name type] args]
              (stoptr! (str name) (str type)))
            (let [f ($$ op)]
              (if (fn? f) (apply f args) ε))))

;; ── EVAL! / EVAL ─────────────────────────────────────────────────────────────
(defn EVAL! [E]
  (when E
    (cond
      (nil? E)     E
      (char? E)    E
      (float? E)   E
      (string? E)  E
      (integer? E) E
      (symbol? E)  ($$ E)
      (vector? E)  (apply list 'SEQ (map EVAL! E))
      (list? E)
      (let [[op & parms] E]
        (cond
          (equal op '.)     (let [[P N]   parms] (INVOKE '. (EVAL! P) N))
          (equal op '$)     (let [[P N]   parms] (INVOKE '$ (EVAL! P) N))
          (equal op '=)     (let [[N R]   parms] (INVOKE '= N (EVAL! R)))
          (equal op '?=)    (let [[N P R] parms] (INVOKE '?= N (EVAL! P) R))
          (equal op '&)     (let [[N]     parms
                                  kw-sym  (symbol (str "&" N))
                                  v       (or (when-let [vr (ns-resolve
                                                              (find-ns 'SNOBOL4clojure.env)
                                                              kw-sym)]
                                                (var-get vr))
                                              ($$ kw-sym))]
                              (if (instance? clojure.lang.IDeref v) @v v))
          (equal op 'quote) (first parms)
          true (let [args (apply vector (map EVAL! parms))]
                 (apply INVOKE op args))))
      true "Yikes! What is E?")))

(defn EVAL [X]
  (cond
    (string? X) (EVAL! (first (emitter (parse-expression X))))
    true        (EVAL! X)))
