(ns SNOBOL4clojure.operators
  ;; SNOBOL4 operator definitions and the EVAL/EVAL!/INVOKE evaluator.
  ;; Operators shadow clojure.core: ?, =, |, $, ., +, -, *, /, %, !
  (:import  [SNOBOL4clojure.env NAME])
  (:require [clojure.tools.trace      :refer :all]
            [SNOBOL4clojure.env       :refer
             [ε η equal not-equal Σ+ subtract multiply divide
              ncvt scvt num $$ out reference]]
            [SNOBOL4clojure.match     :refer [MATCH SEARCH FULLMATCH REPLACE]]
            [SNOBOL4clojure.patterns  :refer
             [ANY BREAK BREAKX NOTANY SPAN ARBNO FENCE
              LEN POS RPOS RTAB TAB FAIL]]
            [SNOBOL4clojure.emitter   :refer [emitter]]
            [SNOBOL4clojure.grammar   :refer [parse-expression]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Numeric conversion macros ─────────────────────────────────────────────────
(defmacro numcvt [x] `(ncvt ~x))
(defmacro uneval [x] `(if (list? ~x) ~x (list 'identity ~x)))

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
(defn at    ([n]        (list 'cursor n))        ; unary  — cursor assign
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
(defn *     ([x]        (uneval x))              ; unary  — defer eval
            ([x y]      (n-2 multiply x y))
            ([x y & zs] (n-n multiply x y zs)))
(defn %     ([_x]       η)
            ([_x _y]    η))
(defn !     ([_x]       η)
            ([x y]      (n-2 'Math/pow x y)))
(defn **    ([x y]      (n-2 'Math/pow x y)))
(defn $     ([n]        ($$ n))                  ; unary  — indirection
            ([x y]      (x-2 $= x y))
            ([x y & zs] (x-n $= x y zs)))
(defn .     ([x]        (NAME. x))               ; unary  — name
            ([x y]      (x-2 .= x y))
            ([x y & zs] (x-n .= x y zs)))
(defn tilde ([x]        (list 'lie x))           ; unary  — negate
            ([_x _y]    η))
(defn |     ([_x]       η)                       ; unary  — programmable
            ([x y]      (x-2 'ALT x y))          ; binary — alternation
            ([x y & zs] (x-n 'ALT x y zs)))

;; ── Numeric math macros ───────────────────────────────────────────────────────
(defmacro INTEGER [_x])
(defmacro SIN    [] `(defn SIN   [x] (Math/sin  ~(numcvt 'x))))
(defmacro COS    [] `(defn COS   [x] (Math/cos  ~(numcvt 'x))))
(defmacro TAN    [] `(defn TAN   [x] (Math/tan  ~(numcvt 'x))))
(defmacro ASIN   [] `(defn ASIN  [x] (Math/asin ~(numcvt 'x))))
(defmacro ACOS   [] `(defn ACOS  [x] (Math/acos ~(numcvt 'x))))
(defmacro ATAN   [] `(defn ATAN  [x] (Math/atan ~(numcvt 'x))))
(defmacro EXP    [] `(defn EXP   [x] (Math/exp  ~(numcvt 'x))))
(defmacro LN     [] `(defn LN    [x] (Math/log  ~(numcvt 'x))))
(defmacro SQRT   [] `(defn SQRT  [x] (Math/sqrt ~(numcvt 'x))))
(defmacro REMDR  [] `(defn REMDR [x y] (clojure.core/rem ~(numcvt 'x) ~(numcvt 'y))))
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
    FENCE   (first args)
    EQ      (EQ     (first args) (second args))
    NE      (NE     (first args) (second args))
    FAIL    FAIL
    ?       (let [[s p] args] (SEARCH (str s) p))
    =       (let [[N r] args]
              (if-not (list? r)
                (eval (list 'def N r))
                (do
                  (eval (list 'def N))
                  (alter-var-root (trace (reference N)) (fn [_] r))))
              r)
    ?=      (let [[n _p R] args, r (EVAL! R)]
              (eval (trace (list 'def n r))) r)
    DEFINE  (let [[proto] args
                  spec    (apply vector (re-seq #"[0-9A-Z_a-z]+" proto))
                  [n]     spec
                  f       (symbol n)]
              (eval (trace (list 'defn f ['& 'args] ε))) ε)
    REPLACE (let [[s1 s2 s3] args] (REPLACE s1 s2 s3))
    quote   ($$ (second op))
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
          (equal op '&)     (let [[N]     parms] @($$ (symbol (str "&" N))))
          (equal op 'quote) (first parms)
          true (let [args (apply vector (map EVAL! parms))]
                 (apply INVOKE op args))))
      true "Yikes! What is E?")))

(defn EVAL [X]
  (cond
    (string? X) (EVAL! (first (emitter (parse-expression X))))
    true        (EVAL! X)))
