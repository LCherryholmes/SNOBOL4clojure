(ns SNOBOL4clojure.operators
  ;; SNOBOL4 operator definitions and the EVAL/EVAL!/INVOKE evaluator.
  ;; Operators shadow clojure.core: ?, =, |, $, ., +, -, *, /, %, !
  (:import  [SNOBOL4clojure.env NAME])
  (:require [clojure.tools.trace      :refer :all]
            [SNOBOL4clojure.env       :refer
             [ε η equal not-equal Σ+ subtract multiply divide
              ncvt scvt num $$ out reference snobol-set!
              table? table-get table-set
              array? array-get array-set array-prototype
              snobol-return! snobol-freturn! snobol-nreturn!]]
            [SNOBOL4clojure.functions :refer
             [ASCII REMDR INTEGER REAL STRING SIZE TRIM DUPL REVERSE LPAD RPAD REPLACE
              ITEM PROTOTYPE CONVERT COPY FIELD SORT RSORT DATA]]
            [SNOBOL4clojure.match     :refer [MATCH SEARCH FULLMATCH]]
            [SNOBOL4clojure.patterns  :refer
             [ANY BREAK BREAKX NOTANY SPAN ARBNO FENCE
              LEN POS RPOS RTAB TAB FAIL]]
            [SNOBOL4clojure.emitter   :refer [emitter]]
            [SNOBOL4clojure.grammar   :refer [parse-expression]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Numeric conversion macros ─────────────────────────────────────────────────
(defmacro numcvt [x] `(ncvt ~x))
(defmacro uneval [x] `(if (list? ~x) ~x (list 'identity ~x)))

(declare APPLY)

;; ── Operator helpers ──────────────────────────────────────────────────────────
(defn annihilate    [_x]        nil)
(defn match-replace [_n _s _p]  nil)
(defn keyword-value [_n]        nil)
(defn $=            [_p _n])
(defn .=            [_p _n])
(defn lie           [p]        `(if (nil? ~p) ε nil))
(defn x-2           [op x y]    (list op x y))
(defn x-n           [op x y Ω]  (apply list (conj Ω y x op)))
(defn n-1           [op x]      (list op (numcvt x)))
(defn n-2           [op x y]    (list op (numcvt x) (numcvt y)))
(defn n-n           [op x y Ω]  (apply list op (map ncvt (conj Ω y x))))

;; ── Comparison primitives (generated via eval) ────────────────────────────────
(defn- primitive [func default missing cvt condition]
  (list 'defn func
    (list []            missing)
    (list ['x]          (list 'if (condition (cvt 'x) default) ε))
    (list ['x 'y '& '_] (list 'if (condition (cvt 'x) (cvt 'y)) ε))))

(eval (primitive 'EQ     0   ε ncvt     #(list 'equal %1 %2)))
(eval (primitive 'NE     0 nil ncvt     #(list 'not=  %1 %2)))
(eval (primitive 'LE     0   ε ncvt     #(list '<=    %1 %2)))
(eval (primitive 'LT     0 nil ncvt     #(list '<     %1 %2)))
(eval (primitive 'GE     0   ε ncvt     #(list '>=    %1 %2)))
(eval (primitive 'GT     0 nil ncvt     #(list '>     %1 %2)))
(eval (primitive 'LEQ    ε   ε scvt     #(list 'equal %1 %2)))
(eval (primitive 'LNE    ε nil scvt     #(list 'not=  %1 %2)))
(eval (primitive 'LLE    ε   ε scvt     #(list '<=    %1 %2)))
(eval (primitive 'LLT    ε nil scvt     #(list '<     %1 %2)))
(eval (primitive 'LGE    ε   ε scvt     #(list '>=    %1 %2)))
(eval (primitive 'LGT    ε nil scvt     #(list '>     %1 %2)))
(eval (primitive 'IDENT  ε   ε identity #(list 'identical? %1 %2)))
(eval (primitive 'DIFFER ε nil identity #(list 'not (list 'identical? %1 %2))))

;; ── SNOBOL4 operators ─────────────────────────────────────────────────────────
(defn =     ([_x]       η)                         ; unary  — programmable
            ([n x]      (list '= n x)))             ; binary — assignment
(defn ?     ([x]        (annihilate x))             ; unary  — interrogation
            ([s p]      (SEARCH (str s) p))         ; binary — match, returns [start end] or nil
            ([n s p]    (match-replace n s p)))     ; ternary — match+replace
(defn ?=    ([n s p]    (match-replace n s p)))
(defn &     ([_n]       nil)                     ; unary  — keyword
            ([_x _y]    η))                      ; binary — programmable
(defn at    ([n]        (list 'CURSOR-IMM! n))   ; unary  — cursor assign
            ([_x _y]    η))
(defn +     ([x]        (n-1 Σ+ x))
            ([x y]      (n-2 Σ+ x y))
            ([x y & zs] (n-n Σ+ x y zs)))
(defn -     ([x]        (n-1 subtract x))
            ([x y]      (n-2 subtract x y))
            ([x y & zs] (n-n subtract x y zs)))
(defn sharp ([_x]       η)
            ([_x _y]    η))
(defn /     ([_x]       η)
            ([x y]      (n-2 divide x y)))
(defn *     ([x]        (list 'DEFER! (fn [] x)))    ; unary  — defer eval
            ([x y]      (n-2 multiply x y))
            ([x y & zs] (n-n multiply x y zs)))
(defn %     ([_x]       η)
            ([_x _y]    η))
(defn !     ([_x]       η)
            ([x y]      (n-2 'Math/pow x y)))
(defn **    ([x y]      (n-2 'Math/pow x y)))
(defn $     ([n]        ($$ n))                       ; unary  — indirection
            ([x y]      (x-2 'CAPTURE-IMM x y))       ; binary — immediate capture
            ([x y & zs] (x-n 'CAPTURE-IMM x y zs)))
(defn .     ([x]        (NAME. x))                    ; unary  — name
            ([x y]      (x-2 'CAPTURE-COND x y))      ; binary — conditional capture
            ([x y & zs] (x-n 'CAPTURE-COND x y zs)))
(defn tilde ([x]        (list 'ALT x ε))              ; unary  — optional (~P = P | ε)
            ([_x _y]    η))
(defn |     ([_x]       η)                       ; unary  — programmable
            ([x y]      (x-2 'ALT x y))          ; binary — alternation
            ([x y & zs] (x-n 'ALT x y zs)))

;; ── Numeric math macros (generate optional math fns on demand) ───────────────
(defmacro SIN    [] `(defn SIN   [x] (Math/sin  ~(numcvt 'x))))
(defmacro COS    [] `(defn COS   [x] (Math/cos  ~(numcvt 'x))))
(defmacro TAN    [] `(defn TAN   [x] (Math/tan  ~(numcvt 'x))))
(defmacro ASIN   [] `(defn ASIN  [x] (Math/asin ~(numcvt 'x))))
(defmacro ACOS   [] `(defn ACOS  [x] (Math/acos ~(numcvt 'x))))
(defmacro ATAN   [] `(defn ATAN  [x] (Math/atan ~(numcvt 'x))))
(defmacro EXP    [] `(defn EXP   [x] (Math/exp  ~(numcvt 'x))))
(defmacro LN     [] `(defn LN    [x] (Math/log  ~(numcvt 'x))))
(defmacro SQRT   [] `(defn SQRT  [x] (Math/sqrt ~(numcvt 'x))))
(defmacro CHOP   [] `(defn CHOP  [x]
                       (let [_x ~(numcvt 'x)]
                         (if (< _x 0.0) (Math/ceil _x) (Math/floor _x)))))

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
    ;; Arithmetic — coerce to SNOBOL numeric type (integer if both integer, else real)
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
    /       (let [ns  (map num args)
                  d   (last ns)]
              (if (zero? d)
                (throw (ex-info "Division by zero" {:snobol/error 14}))
                (let [result (apply clojure.core// ns)]
                  (if (== (Math/floor result) result)
                    (long result)
                    result))))
    ?       (let [[s p] args] (SEARCH (str s) p))
    =       (let [[N r] args]
              (cond
                ;; subscript assignment: (= (A k...) val) — mutate TABLE, ARRAY, or PDD
                (and (list? N) (>= (count N) 2))
                (let [[container-sym & ks] N
                      container ($$ container-sym)]
                  (cond
                    (table? container)
                    (table-set container (first ks) r)
                    (array? container)
                    (or (array-set container (vec ks) r)
                        (throw (ex-info "ARRAY: subscript out of bounds" {:keys ks})))
                    ;; PDD field setter: accessor-fn(instance-sym) = val
                    ;; The accessor fn called with 2 args returns the updated map;
                    ;; we then rebind the instance variable.
                    (fn? container)
                    (let [inst-sym (first ks)
                          inst     ($$ inst-sym)
                          updated  (container inst r)]
                      (snobol-set! inst-sym updated)
                      r)
                    :else nil))
                ;; normal variable assignment
                (clojure.core/contains? #{'OUTPUT 'TERMINAL 'INPUT} N)
                (let [out-val (if (and (list? r) (= (first r) 'SEQ))
                                (apply str (map #(if (nil? %) "" (str %)) (rest r)))
                                r)]
                  (when (clojure.core/= N 'OUTPUT)   (println out-val))
                  (when (clojure.core/= N 'TERMINAL) (println out-val))
                  out-val)
                :else
                (do (snobol-set! N r) r)))
    ?=      (let [[n p R] args
                  subject (str (or ($$ n) ""))
                  pat     (EVAL! p)
                  repl    (str (or (EVAL! R) ""))]
              (when-let [[start end] (SEARCH subject pat)]
                (let [result (str (subs subject 0 start)
                                  repl
                                  (subs subject end))]
                  (snobol-set! n result)
                  result)))
    DEFINE  (let [[proto entry-arg] args
                  ;; Parse: 'fname(p1,p2,...)l1,l2,...'  or  'fname(p1,...)'
                  lp        (.indexOf ^String proto "(")
                  rp        (.indexOf ^String proto ")")
                  fname     (if (>= lp 0) (.substring ^String proto 0 lp) proto)
                  inner     (if (and (>= lp 0) (>= rp 0))
                              (.substring ^String proto (inc lp) rp) "")
                  after-rp  (if (>= rp 0) (.substring ^String proto (inc rp)) "")
                  params    (if (clojure.string/blank? inner) []
                              (mapv clojure.string/trim
                                    (clojure.string/split inner #",")))
                  locals    (if (clojure.string/blank? after-rp) []
                              (let [s (clojure.string/trim after-rp)]
                                (if (clojure.string/blank? s) []
                                  (mapv clojure.string/trim
                                        (clojure.string/split s #",")))))
                  f-sym     (symbol fname)
                  ;; Entry point: explicit .label arg, or keyword from fname
                  entry     (if (and (>= (count args) 2) entry-arg)
                              ;; entry-arg may be a NAME (symbol) from (.label) notation
                              (if (symbol? entry-arg)
                                (keyword (str (name entry-arg)))
                                (keyword (str entry-arg)))
                              (keyword fname))
                  all-saved (into params locals)]
              (letfn [(the-fn [& call-args]
                        ;; Save current values of params + locals
                        (let [saved (zipmap all-saved (map #($$ (symbol %)) all-saved))]
                          ;; Bind parameters to call arguments
                          (doseq [i (range (count params))]
                            (snobol-set! (symbol (params i))
                                         (nth call-args i ε)))
                          ;; Clear locals
                          (doseq [l locals]
                            (snobol-set! (symbol l) ε))
                          ;; Clear the result slot
                          (snobol-set! f-sym ε)
                          ;; Run from entry label; catch RETURN/FRETURN/NRETURN
                          (let [run-fn  (ns-resolve 'SNOBOL4clojure.runtime 'RUN)
                                outcome (try
                                          (when run-fn ((var-get run-fn) entry))
                                          :return   ; normal fall-through = return
                                          (catch clojure.lang.ExceptionInfo e
                                            (get (ex-data e) :snobol/signal :return)))]
                            ;; Collect result before restoring
                            (let [result ($$ f-sym)]
                              ;; Restore saved values
                              (doseq [[k v] saved]
                                (snobol-set! (symbol k) v))
                              ;; Restore fn reference
                              (snobol-set! f-sym the-fn)
                              ;; Dispatch on outcome
                              (case outcome
                                :return  result
                                :freturn nil    ; nil → statement failure → :F branch
                                :nreturn nil
                                result)))))]   ; fall-through = return
                (snobol-set! f-sym the-fn)
                ε))
    define  (apply INVOKE 'DEFINE args)
    APPLY   (apply APPLY (first args) (rest args))
    apply   (apply APPLY (first args) (rest args))
    REPLACE (let [[s1 s2 s3] args] (REPLACE s1 s2 s3))
    TABLE   (apply SNOBOL4clojure.env/TABLE args)
    table   (apply SNOBOL4clojure.env/TABLE args)
    ARRAY   (apply SNOBOL4clojure.env/ARRAY args)
    array   (apply SNOBOL4clojure.env/ARRAY args)
    ASCII   (ASCII  (first args))
    REMDR   (REMDR  (first args) (second args))
    INTEGER (INTEGER (first args))
    REAL    (REAL    (first args))
    STRING  (STRING  (first args))
    SIZE    (SIZE   (first args))
    TRIM    (TRIM   (first args))
    DUPL    (DUPL   (first args) (second args))
    REVERSE (REVERSE (first args))
    LPAD    (LPAD   (first args) (second args))
    RPAD    (RPAD   (first args) (second args))
    ITEM      (ITEM      (first args) (second args))
    item      (ITEM      (first args) (second args))
    PROTOTYPE (PROTOTYPE (first args))
    prototype (PROTOTYPE (first args))
    CONVERT   (CONVERT   (first args) (second args))
    convert   (CONVERT   (first args) (second args))
    COPY      (COPY      (first args))
    copy      (COPY      (first args))
    FIELD     (FIELD     (first args) (second args))
    field     (FIELD     (first args) (second args))
    SORT      (SORT      (first args))
    sort      (SORT      (first args))
    RSORT     (RSORT     (first args))
    rsort     (RSORT     (first args))
    DATA      (DATA      (first args))
    data      (DATA      (first args))
    DATATYPE  (SNOBOL4clojure.env/DATATYPE (first args))
    datatype  (SNOBOL4clojure.env/DATATYPE (first args))
    quote   ($$ (second op))
            (let [f ($$ op)]
              (cond
                (table? f) (table-get f (first args))   ; TABLE subscript read
                (array? f) (array-get f (vec args))     ; ARRAY subscript read
                (fn? f)    (apply f args)
                :else      ε))))

;; ── APPLY ─────────────────────────────────────────────────────────────────────
(defn APPLY
  "APPLY(fname, arg1, ...) — call a named function by string."
  [fname & fargs]
  (let [f-fn ($$ (symbol (str fname)))]
    (when (fn? f-fn)
      (apply f-fn fargs))))

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
          (equal op '=)     (let [[N R]   parms
                                  ;; If N is a subscript call (container key...),
                                  ;; evaluate all keys but not the container symbol
                                  N' (if (and (list? N) (>= (count N) 2))
                                       (apply list (first N) (map EVAL! (rest N)))
                                       N)]
                              (INVOKE '= N' (EVAL! R)))
          (equal op '?=)    (let [[N P R] parms] (INVOKE '?= N (EVAL! P) R))
          (equal op '&)     (let [[N]     parms
                                  kw-sym  (symbol (str "&" N))
                                  ;; &-keywords are defs in env.clj; fall back to user ns
                                  v       (or (when-let [vr (ns-resolve (find-ns 'SNOBOL4clojure.env) kw-sym)]
                                                (var-get vr))
                                              ($$ kw-sym))]
                              (if (instance? clojure.lang.IDeref v) @v v))
          (equal op 'SEQ)   (apply str (map #(let [v (EVAL! %)] (if (nil? v) "" (str v))) parms))
          (equal op 'quote) (first parms)
          true (let [args (apply vector (map EVAL! parms))]
                 (apply INVOKE op args))))
      true "Yikes! What is E?")))

(defn EVAL [X]
  (cond
    (string? X) (EVAL! (first (emitter (parse-expression X))))
    true        (EVAL! X)))
