(ns SNOBOL4clojure.catalog.t-missing
  "Catalog tests for functions that were previously missing from INVOKE dispatch
   or had no dedicated test coverage.
   Covers: CHAR, SUBSTR, DATE, TIME, LEQ/LNE/LLE/LLT/LGE, LPAD/RPAD 3-arg, ARG, LOCAL."
  (:require [clojure.test :refer [deftest is testing]]
            [SNOBOL4clojure.test-helpers :refer [prog]]))

;; ── CHAR ─────────────────────────────────────────────────────────────────────

(deftest char_65
  "CHAR(65) = 'A'"
  (is (= "A\n" (:stdout (prog "
        OUTPUT = CHAR(65)
END")))))

(deftest char_90
  "CHAR(90) = 'Z'"
  (is (= "Z\n" (:stdout (prog "
        OUTPUT = CHAR(90)
END")))))

(deftest char_97
  "CHAR(97) = 'a'"
  (is (= "a\n" (:stdout (prog "
        OUTPUT = CHAR(97)
END")))))

(deftest char_48
  "CHAR(48) = '0'"
  (is (= "0\n" (:stdout (prog "
        OUTPUT = CHAR(48)
END")))))

(deftest char_32
  "CHAR(32) = space"
  (is (= " \n" (:stdout (prog "
        OUTPUT = CHAR(32)
END")))))

;; ── SUBSTR ───────────────────────────────────────────────────────────────────

(deftest substr_1_3
  "SUBSTR('hello',1,3) = 'hel' (1-based, length)"
  (is (= "hel\n" (:stdout (prog "
        OUTPUT = SUBSTR('hello',1,3)
END")))))

(deftest substr_2_3
  "SUBSTR('hello',2,3) = 'ell'"
  (is (= "ell\n" (:stdout (prog "
        OUTPUT = SUBSTR('hello',2,3)
END")))))

(deftest substr_3_1
  "SUBSTR('hello',3,1) = 'l'"
  (is (= "l\n" (:stdout (prog "
        OUTPUT = SUBSTR('hello',3,1)
END")))))

(deftest substr_1_5
  "SUBSTR('hello',1,5) = full string"
  (is (= "hello\n" (:stdout (prog "
        OUTPUT = SUBSTR('hello',1,5)
END")))))

(deftest substr_5_1
  "SUBSTR('hello',5,1) = 'o' (last char)"
  (is (= "o\n" (:stdout (prog "
        OUTPUT = SUBSTR('hello',5,1)
END")))))

(deftest substr_in_loop
  "SUBSTR used in loop to extract chars"
  (is (= "h\ne\nl\nl\no\n" (:stdout (prog "
        S = 'hello'
        I = 1
LOOP    OUTPUT = SUBSTR(S,I,1)
        I = I + 1
        LE(I,SIZE(S)) :S(LOOP)
END")))))

;; ── DATE / TIME ───────────────────────────────────────────────────────────────

(deftest date_returns_nonempty
  "DATE() returns a non-empty string"
  (let [r (prog "
        D = DATE()
        GT(SIZE(D),0) :S(OK)F(FAIL)
OK      OUTPUT = 'ok'
        :(END)
FAIL    OUTPUT = 'fail'
END")]
    (is (= "ok\n" (:stdout r)))))

(deftest time_returns_number
  "TIME() returns a positive integer"
  (let [r (prog "
        T = TIME()
        GT(T,0) :S(OK)F(FAIL)
OK      OUTPUT = 'ok'
        :(END)
FAIL    OUTPUT = 'fail'
END")]
    (is (= "ok\n" (:stdout r)))))

;; ── LEQ (string equal) ───────────────────────────────────────────────────────

(deftest leq_equal_succeeds
  "LEQ('abc','abc') succeeds"
  (is (= "yes\n" (:stdout (prog "
        LEQ('abc','abc') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest leq_unequal_fails
  "LEQ('abc','xyz') fails"
  (is (= "no\n" (:stdout (prog "
        LEQ('abc','xyz') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest leq_empty_empty
  "LEQ('','') succeeds"
  (is (= "yes\n" (:stdout (prog "
        LEQ('','') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

;; ── LNE (string not-equal) ───────────────────────────────────────────────────

(deftest lne_unequal_succeeds
  "LNE('abc','xyz') succeeds"
  (is (= "yes\n" (:stdout (prog "
        LNE('abc','xyz') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest lne_equal_fails
  "LNE('abc','abc') fails"
  (is (= "no\n" (:stdout (prog "
        LNE('abc','abc') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

;; ── LLT (string less-than) ───────────────────────────────────────────────────

(deftest llt_a_b_succeeds
  "LLT('a','b') succeeds (a < b lexicographically)"
  (is (= "yes\n" (:stdout (prog "
        LLT('a','b') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest llt_b_a_fails
  "LLT('b','a') fails"
  (is (= "no\n" (:stdout (prog "
        LLT('b','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest llt_equal_fails
  "LLT('a','a') fails"
  (is (= "no\n" (:stdout (prog "
        LLT('a','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

;; ── LLE (string less-or-equal) ───────────────────────────────────────────────

(deftest lle_a_b_succeeds
  "LLE('a','b') succeeds"
  (is (= "yes\n" (:stdout (prog "
        LLE('a','b') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest lle_equal_succeeds
  "LLE('a','a') succeeds"
  (is (= "yes\n" (:stdout (prog "
        LLE('a','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest lle_b_a_fails
  "LLE('b','a') fails"
  (is (= "no\n" (:stdout (prog "
        LLE('b','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

;; ── LGE (string greater-or-equal) ────────────────────────────────────────────

(deftest lge_b_a_succeeds
  "LGE('b','a') succeeds"
  (is (= "yes\n" (:stdout (prog "
        LGE('b','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest lge_equal_succeeds
  "LGE('a','a') succeeds"
  (is (= "yes\n" (:stdout (prog "
        LGE('a','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest lge_a_b_fails
  "LGE('a','b') fails"
  (is (= "no\n" (:stdout (prog "
        LGE('a','b') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

;; ── LPAD / RPAD (3-arg fill char) ────────────────────────────────────────────

(deftest lpad_3arg_dash
  "LPAD('hi',6,'-') = '----hi'"
  (is (= "----hi\n" (:stdout (prog "
        OUTPUT = LPAD('hi',6,'-')
END")))))

(deftest rpad_3arg_dash
  "RPAD('hi',6,'-') = 'hi----'"
  (is (= "hi----\n" (:stdout (prog "
        OUTPUT = RPAD('hi',6,'-')
END")))))

(deftest lpad_3arg_zero
  "LPAD('hi',6,'0') = '0000hi'"
  (is (= "0000hi\n" (:stdout (prog "
        OUTPUT = LPAD('hi',6,'0')
END")))))

(deftest rpad_3arg_dot
  "RPAD('ok',5,'.') = 'ok...'"
  (is (= "ok...\n" (:stdout (prog "
        OUTPUT = RPAD('ok',5,'.')
END")))))

(deftest lpad_3arg_short
  "LPAD when width <= length returns original"
  (is (= "hello\n" (:stdout (prog "
        OUTPUT = LPAD('hello',3,'-')
END")))))

(deftest rpad_3arg_short
  "RPAD when width <= length returns original"
  (is (= "hello\n" (:stdout (prog "
        OUTPUT = RPAD('hello',3,'-')
END")))))

;; ── ARG ──────────────────────────────────────────────────────────────────────

(deftest arg_first_param
  "ARG('F',1) returns name of first parameter"
  (is (= "X\n" (:stdout (prog "
        DEFINE('F(X,Y)')
F       OUTPUT = ARG('F',1)
        :(RETURN)
        F('a','b')
END")))))

(deftest arg_second_param
  "ARG('F',2) returns name of second parameter"
  (is (= "Y\n" (:stdout (prog "
        DEFINE('F(X,Y)')
F       OUTPUT = ARG('F',2)
        :(RETURN)
        F('a','b')
END")))))

(deftest arg_out_of_range
  "ARG('F',99) fails (returns nil → statement failure)"
  (is (= "no\n" (:stdout (prog "
        DEFINE('F(X)')
F       ARG('F',99) :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(RETURN)
NO      OUTPUT = 'no'
        :(RETURN)
        F('a')
END")))))

(deftest arg_single_param
  "ARG with single-param function"
  (is (= "N\n" (:stdout (prog "
        DEFINE('DOUBLE(N)')
DOUBLE  OUTPUT = ARG('DOUBLE',1)
        :(RETURN)
        DOUBLE(5)
END")))))

;; ── LOCAL ────────────────────────────────────────────────────────────────────

(deftest local_first
  "LOCAL('F',1) returns name of first local variable"
  (is (= "L1\n" (:stdout (prog "
        DEFINE('F(X)L1,L2')
F       OUTPUT = LOCAL('F',1)
        :(RETURN)
        F('a')
END")))))

(deftest local_second
  "LOCAL('F',2) returns name of second local variable"
  (is (= "L2\n" (:stdout (prog "
        DEFINE('F(X)L1,L2')
F       OUTPUT = LOCAL('F',2)
        :(RETURN)
        F('a')
END")))))

(deftest local_out_of_range
  "LOCAL('F',99) fails when out of range"
  (is (= "no\n" (:stdout (prog "
        DEFINE('F(X)L1')
F       LOCAL('F',99) :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(RETURN)
NO      OUTPUT = 'no'
        :(RETURN)
        F('a')
END")))))

(deftest local_no_locals
  "LOCAL on function with no locals fails"
  (is (= "no\n" (:stdout (prog "
        DEFINE('F(X)')
F       LOCAL('F',1) :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(RETURN)
NO      OUTPUT = 'no'
        :(RETURN)
        F('a')
END")))))

;; ── LGT (string greater-than) ─────────────────────────────────────────────────

(deftest lgt_succeeds
  "LGT('b','a') succeeds — 'b' > 'a' lexicographically"
  (is (= "yes\n" (:stdout (prog "
        LGT('b','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest lgt_fails_equal
  "LGT('a','a') fails — equal strings are not strictly greater"
  (is (= "no\n" (:stdout (prog "
        LGT('a','a') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))

(deftest lgt_fails_less
  "LGT('a','b') fails — 'a' < 'b' lexicographically"
  (is (= "no\n" (:stdout (prog "
        LGT('a','b') :S(YES)F(NO)
YES     OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END")))))
