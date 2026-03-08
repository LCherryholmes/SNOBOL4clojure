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
              snobol-return! snobol-freturn! snobol-nreturn!
              <FUNS> <FDEFS>]]
            [SNOBOL4clojure.functions :refer
             [ASCII CHAR DATE TIME REMDR INTEGER REAL STRING SIZE TRIM DUPL REVERSE LPAD RPAD REPLACE SUBSTR
              ITEM PROTOTYPE CONVERT COPY FIELD SORT RSORT DATA]]
            [SNOBOL4clojure.match     :refer [MATCH SEARCH FULLMATCH]]
            [SNOBOL4clojure.patterns  :refer
             [ANY BREAK BREAKX NOTANY SPAN ARBNO FENCE
              ABORT BAL CONJ
              LEN POS RPOS RTAB TAB FAIL]]
            [SNOBOL4clojure.emitter   :refer [emitter]]
            [SNOBOL4clojure.grammar   :refer [parse-expression]]
            [SNOBOL4clojure.trace     :refer [fire-value! fire-call! fire-return! trace! stoptr!]])
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
(eval (primitive 'LLE    ε   ε scvt     #(list '<=   (list 'compare %1 %2) 0)))
(eval (primitive 'LLT    ε nil scvt     #(list '<    (list 'compare %1 %2) 0)))
(eval (primitive 'LGE    ε   ε scvt     #(list '>=   (list 'compare %1 %2) 0)))
(eval (primitive 'LGT    ε nil scvt     #(list '>    (list 'compare %1 %2) 0)))
(eval (primitive 'IDENT  ε   ε identity #(list 'equal %1 %2)))
(eval (primitive 'DIFFER ε nil identity #(list 'not= %1 %2)))

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
    TAB     (TAB    (first args))
    RTAB    (RTAB   (first args))
    ANY     (ANY    (first args))
    BREAK   (BREAK  (first args))
    BREAKX  (BREAKX (first args))
    NOTANY  (NOTANY (first args))
    SPAN    (SPAN   (first args))
    FENCE   (if (seq args) (FENCE (first args)) (FENCE))
    ARBNO   (ARBNO  (first args))
    ABORT   ABORT
    BAL     BAL
    CONJ    (CONJ   (first args) (second args))
    EQ      (EQ     (first args) (second args))
    NE      (NE     (first args) (second args))
    LE      (LE     (first args) (second args))
    LT      (LT     (first args) (second args))
    GE      (GE     (first args) (second args))
    GT      (GT     (first args) (second args))
    FAIL    FAIL
    ;; Arithmetic — coerce to SNOBOL numeric type (integer if both integer, else real)
    +       (let [ns (map num args)]
              (if (every? integer? ns)
                (long (apply clojure.core/+' ns))
                (apply clojure.core/+ ns)))
    -       (let [ns (map num args)]
              (if (every? integer? ns)
                (long (apply clojure.core/-' ns))
                (apply clojure.core/- ns)))
    *       (let [ns (map num args)]
              (if (every? integer? ns)
                (long (apply clojure.core/*' ns))
                (apply clojure.core/* ns)))
    tilde   (let [v (first args)]       ; ~ negates success/failure
              (if (nil? v) ε nil))
    /       (let [ns (map num args)
                  d  (last ns)]
              (if (zero? d)
                (throw (ex-info "Division by zero" {:snobol/error 14}))
                (if (every? integer? ns)
                  (apply quot ns)
                  (apply clojure.core// ns))))
    ?       (let [[s p] args] (SEARCH (str s) p))
    =       (let [[N r] args]
              ;; nil replacement = sub-expression failure → statement fails
              (when-not (nil? r)
              (cond
                ;; subscript assignment: (= (A k...) val) — mutate TABLE, ARRAY, or PDD
                (and (list? N) (>= (count N) 2))
                (let [[container-sym & ks] N
                      raw-container ($$ container-sym)
                      ;; NAME indirect reference — dereference to the actual array/table
                      container (if (instance? NAME raw-container)
                                  ($$ (symbol (str (.n raw-container))))
                                  raw-container)]
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
                (let [out-val (if (and (list? r) (clojure.core/= (first r) 'SEQ))
                                (apply str (map #(if (nil? %) "" (str %)) (rest r)))
                                r)]
                  (when (clojure.core/= N 'OUTPUT)   (println out-val))
                  (when (clojure.core/= N 'TERMINAL) (binding [*out* *err*] (println out-val)))
                  out-val)
                :else
                (let [v (if (and (list? r) (clojure.core/= (first r) 'SEQ))
                          (apply str (map #(if (nil? %) "" (str %)) (rest r)))
                          r)]
                  (snobol-set! N v)
                  (fire-value! N v)
                  v))))
    ?=      (let [[n p R] args
                  subject (str (or ($$ n) ""))
                  pat     p                       ; already evaluated by EVAL! dispatch
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
                              (let [s (-> after-rp clojure.string/trim
                                          (clojure.string/replace #"^," ""))]
                                (filterv (complement clojure.string/blank?)
                                         (mapv clojure.string/trim
                                               (clojure.string/split s #",")))))
                  f-sym     (symbol fname)
                  ;; Entry point: explicit .label arg, or keyword from fname (uppercased to match label table)
                  entry     (if (and (>= (count args) 2) entry-arg)
                              ;; entry-arg may be a NAME (symbol) from (.label) notation
                              (if (symbol? entry-arg)
                                (keyword (clojure.string/upper-case (str (name entry-arg))))
                                (keyword (clojure.string/upper-case (str entry-arg))))
                              (keyword (clojure.string/upper-case fname)))
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
                          ;; CALL trace
                          (fire-call! fname call-args)
                          ;; Run from entry label; catch RETURN/FRETURN/NRETURN
                          (let [run-fn  (ns-resolve 'SNOBOL4clojure.runtime 'RUN)
                                outcome (try
                                          (when run-fn ((var-get run-fn) entry))
                                          :return   ; normal fall-through = return
                                          (catch clojure.lang.ExceptionInfo e
                                            (get (ex-data e) :snobol/signal :return)))]
                            ;; Collect result before restoring
                            (let [result ($$ f-sym)]
                              ;; RETURN trace
                              (fire-return! fname result outcome)
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
                (swap! <FUNS>  assoc (clojure.string/upper-case fname) the-fn)
                (swap! <FDEFS> assoc (clojure.string/upper-case fname)
                               {:params params :locals locals})
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
    ;; String functions missing from original dispatch
    CHAR      (CHAR      (first args))
    char      (CHAR      (first args))
    SUBSTR    (SUBSTR    (first args) (second args) (nth args 2))
    substr    (SUBSTR    (first args) (second args) (nth args 2))
    DATE      (DATE)
    date      (DATE)
    TIME      (TIME)
    time      (TIME)
    ;; LPAD/RPAD with optional 3rd fill-char arg (functions.clj handles both arities)
    LPAD      (apply LPAD args)
    lpad      (apply LPAD args)
    RPAD      (apply RPAD args)
    rpad      (apply RPAD args)
    ;; String comparisons (lexicographic) — were defined by primitive macro but missing here
    LEQ       (LEQ    (first args) (second args))
    leq       (LEQ    (first args) (second args))
    LNE       (LNE    (first args) (second args))
    lne       (LNE    (first args) (second args))
    LLE       (LLE    (first args) (second args))
    lle       (LLE    (first args) (second args))
    LLT       (LLT    (first args) (second args))
    llt       (LLT    (first args) (second args))
    LGE       (LGE    (first args) (second args))
    lge       (LGE    (first args) (second args))
    LGT       (LGT    (first args) (second args))
    lgt       (LGT    (first args) (second args))
    ;; ARG(fname, n) — nth parameter name of function fname (1-based)
    ARG       (let [[fname n] args
                    meta (get @<FDEFS> (clojure.string/upper-case (str fname)))]
                (when meta (nth (:params meta) (dec (long n)) nil)))
    arg       (apply INVOKE 'ARG args)
    ;; LOCAL(fname, n) — nth local variable name of function fname (1-based)
    LOCAL     (let [[fname n] args
                    meta (get @<FDEFS> (clojure.string/upper-case (str fname)))]
                (when meta (nth (:locals meta) (dec (long n)) nil)))
    local     (apply INVOKE 'LOCAL args)
    ;; CODE(src) — compile and execute a SNOBOL4 source fragment in the current env.
    ;; Returns nil on success; :F branch taken on compile/runtime error.
    ;; Uses resolve to avoid circular dependency (runtime → operators → compiler).
    ;; Sprint 25F.
    CODE      (let [src     (str (first args))
                    compile (ns-resolve 'SNOBOL4clojure.compiler 'CODE)
                    run     (ns-resolve 'SNOBOL4clojure.runtime  'RUN)]
                (when-not (and compile run)
                  (throw (ex-info "CODE: compiler/runtime not loaded"
                                  {:snobol/signal :error})))
                (try
                  (run (compile src))
                  nil
                  (catch clojure.lang.ExceptionInfo e
                    (case (get (ex-data e) :snobol/signal)
                      :end nil
                      (throw e)))
                  (catch Exception e
                    (throw (ex-info (.getMessage e) {:snobol/signal :error})))))
    code      (apply INVOKE 'CODE args)
    quote   ($$ (second op))
            (let [raw-f ($$ op)
                  ;; NAME indirect reference — dereference to actual array/table
                  f (if (instance? NAME raw-f)
                      ($$ (symbol (str (.n raw-f))))
                      raw-f)
                  ;; When f is not callable (e.g. result-slot cleared to ε during
                  ;; recursive execution), fall back to the functions registry.
                  f (if (fn? f) f
                      (get @<FUNS> (clojure.string/upper-case (str op)) f))]
              (cond
                (table? f) (table-get f (first args))   ; TABLE subscript read
                (array? f) (array-get f (vec args))     ; ARRAY subscript read
                (fn? f)    (apply f args)
                :else      ε))))

;; ── APPLY ─────────────────────────────────────────────────────────────────────
(defn APPLY
  "APPLY(fname, arg1, ...) — call a named function by string."
  [fname & fargs]
  (let [raw-f ($$ (symbol (str fname)))
        f-fn  (if (fn? raw-f) raw-f
                  (get @<FUNS> (clojure.string/upper-case (str fname))))]
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
      (vector? E)  (let [evaled (map EVAL! E)]
                    ;; nil in any SEQ element = sub-expression failed → whole SEQ fails
                    (if (some nil? evaled)
                      nil
                      (apply list 'SEQ evaled)))
      (list? E)
      (let [[op & parms] E]
        (cond
          (equal op '.)     (let [[P N]   parms] (INVOKE '. (EVAL! P) N))
          (equal op '$)     (let [[P N]   parms] (INVOKE '$ (EVAL! P) N))
          (equal op '=)     (let [[N R]   parms
                                  ;; If N is a subscript call (container key...),
                                  ;; evaluate the keys — EXCEPT for PDD field setters
                                  ;; where the first key must remain a symbol so
                                  ;; snobol-set! can rebind the instance variable.
                                  N' (if (and (list? N) (>= (count N) 2))
                                       (let [container-sym (first N)
                                             container-val  ($$ container-sym)]
                                         (if (fn? container-val)
                                           ;; PDD setter: keep keys as raw symbols
                                           N
                                           ;; TABLE/ARRAY: evaluate keys
                                           (apply list container-sym (map EVAL! (rest N)))))
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
          ;; Unary * = deferred pattern: do NOT pre-evaluate the arg.
          ;; The thunk must re-read the variable at match time so that
          ;; SPAN(*B) / ANY(*B) / NOTANY(*B) see the current value of B,
          ;; not the value B held when the pattern was first assigned.
          (and (equal op '*) (clojure.core/= (count parms) 1))
          (let [sym (first parms)]
            (list 'DEFER! (fn [] (EVAL! sym))))
          true (let [args (apply vector (map EVAL! parms))]
                 (apply INVOKE op args))))
      true "Yikes! What is E?")))

(defn EVAL [X]
  (cond
    (string? X) (EVAL! (first (emitter (parse-expression X))))
    true        (EVAL! X)))
