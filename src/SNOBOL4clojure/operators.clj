(ns SNOBOL4clojure.operators
  ;; SNOBOL4 operator definitions and the EVAL/EVAL!/INVOKE evaluator.
  ;; Operators shadow clojure.core: ?, =, |, $, ., +, -, *, /, %, !
  (:import  [SNOBOL4clojure.env NAME])
  (:require [clojure.tools.trace      :refer :all]
            [SNOBOL4clojure.env       :refer
             [ε η equal not-equal Σ+ subtract multiply divide
              ncvt scvt num $$ out reference snobol-set!]]
            [SNOBOL4clojure.functions :refer
             [ASCII REMDR INTEGER REAL STRING SIZE TRIM DUPL REVERSE LPAD RPAD REPLACE]]
            [SNOBOL4clojure.match-api :refer [SEARCH]]
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
(defn $     ([n]        ($$ n))                       ; unary  — indirection
            ([x y]      (x-2 'CAPTURE-IMM x y))       ; binary — immediate capture
            ([x y & zs] (x-n 'CAPTURE-IMM x y zs)))
(defn .     ([x]        (NAME. x))                    ; unary  — name
            ([x y]      (x-2 'CAPTURE-COND x y))      ; binary — conditional capture
            ([x y & zs] (x-n 'CAPTURE-COND x y zs)))
(defn tilde ([x]        (list 'ALT x ε))           ; unary  — negate
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
