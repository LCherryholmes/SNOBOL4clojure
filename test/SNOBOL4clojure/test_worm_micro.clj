(ns SNOBOL4clojure.test-worm-micro
  "Length-tiered micro test suite — I am the worm generator.
   Generated methodically by growing program complexity one character at a time.

   Fixture variables (pre-seeded each test via use-fixtures):
     I=5  J=10  K=3  L=7  M=0  N=100
     S='hello'  T='world'  X='abc'  Y='xyz'  Z=''
     P=LEN(3)   Q=ANY('aeiou')   R=SPAN('abc')

   Organisation by length tier:
     Tier 0 — trivial/empty (0–5 chars body)
     Tier 1 — single statement (6–20 chars)
     Tier 2 — two/three statements (21–40 chars)
     Tier 3 — branching, patterns, capture (41–64 chars)
     Tier 4 — loops, multi-feature integration
     Tier 5 — oracle: output-driven, verified against CSNOBOL4
  "
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-steplimit]]))

(GLOBALS *ns*)

(defn- seed-fixtures! []
  (snobol-set! 'I 5)
  (snobol-set! 'J 10)
  (snobol-set! 'K 3)
  (snobol-set! 'L 7)
  (snobol-set! 'M 0)
  (snobol-set! 'N 100)
  (snobol-set! 'S "hello")
  (snobol-set! 'T "world")
  (snobol-set! 'X "abc")
  (snobol-set! 'Y "xyz")
  (snobol-set! 'Z ""))

(use-fixtures :each
  (fn [f]
    (GLOBALS (find-ns 'SNOBOL4clojure.test-worm-micro))
    (seed-fixtures!)
    (f)))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 0 — Trivial / empty (0–5 chars body)
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t0_empty_program
  "Empty program — just END"
  (prog "end")
  (is true))

(deftest micro_t0_end_only
  "END label — program terminates immediately"
  (prog "END"))

(deftest micro_t0_blank_body
  "Statement with blank body — no-op label line"
  (prog
    "L1"
    "        :(END)"
    "end")
  (is true))

(deftest micro_t0_comment_only
  "Comment line — asterisk in column 1"
  (prog
    "* this is a comment"
    "end")
  (is true))

(deftest micro_t0_uninit_is_null
  "Uninitialized variable is null string"
  (prog "end")
  (is (clojure.core/= "" ($$ 'UNINIT_VAR))))

(deftest micro_t0_uninit_size_zero
  "SIZE of uninitialized variable is 0"
  (prog
    "        RESULT = SIZE(NEVER_SET)"
    "end")
  (is (clojure.core/= 0 ($$ 'RESULT))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 1 — Single statement (6–20 chars body)
;; ─────────────────────────────────────────────────────────────────────────────

;; Integer assignment — all fixtures
(deftest micro_t1_assign_int_0
  (prog "        I = 0" "end")
  (is (clojure.core/= 0 ($$ 'I))))

(deftest micro_t1_assign_int_1
  (prog "        I = 1" "end")
  (is (clojure.core/= 1 ($$ 'I))))

(deftest micro_t1_assign_int_neg
  (prog "        I = -1" "end")
  (is (clojure.core/= -1 ($$ 'I))))

(deftest micro_t1_assign_int_large
  (prog "        N = 999" "end")
  (is (clojure.core/= 999 ($$ 'N))))

(deftest micro_t1_assign_int_zero_expr
  (prog "        M = 0" "end")
  (is (clojure.core/= 0 ($$ 'M))))

;; String assignment — null and literals
(deftest micro_t1_assign_null_string
  "Assign null string via empty RHS"
  (prog "        S =" "end")
  (is (clojure.core/= "" ($$ 'S))))

(deftest micro_t1_assign_empty_quotes
  "Assign empty string via empty quotes"
  (prog "        S = ''" "end")
  (is (clojure.core/= "" ($$ 'S))))

(deftest micro_t1_assign_string_lit
  (prog "        S = 'hi'" "end")
  (is (clojure.core/= "hi" ($$ 'S))))

(deftest micro_t1_assign_string_spaces
  (prog "        S = 'a b'" "end")
  (is (clojure.core/= "a b" ($$ 'S))))

(deftest micro_t1_assign_string_digits
  (prog "        S = '123'" "end")
  (is (clojure.core/= "123" ($$ 'S))))

;; Real assignment
(deftest micro_t1_assign_real
  (prog "        A = 1.5" "end")
  (is (clojure.core/= 1.5 ($$ 'A))))

(deftest micro_t1_assign_real_zero
  (prog "        A = 0.0" "end")
  (is (clojure.core/= 0.0 ($$ 'A))))

(deftest micro_t1_assign_real_neg
  (prog "        A = -2.5" "end")
  (is (clojure.core/= -2.5 ($$ 'A))))

;; Copy assignment
(deftest micro_t1_assign_var_to_var
  (prog "        J = I" "end")
  (is (clojure.core/= 5 ($$ 'J))))    ; I=5 from fixture

(deftest micro_t1_assign_str_to_str
  (prog "        T = S" "end")
  (is (clojure.core/= "hello" ($$ 'T))))   ; S='hello' from fixture

;; Arithmetic — single op
(deftest micro_t1_add_lits
  (prog "        I = 3 + 4" "end")
  (is (clojure.core/= 7 ($$ 'I))))

(deftest micro_t1_sub_lits
  (prog "        I = 10 - 3" "end")
  (is (clojure.core/= 7 ($$ 'I))))

(deftest micro_t1_mul_lits
  (prog "        I = 4 * 5" "end")
  (is (clojure.core/= 20 ($$ 'I))))

(deftest micro_t1_div_lits
  (prog "        I = 10 / 2" "end")
  (is (clojure.core/= 5 ($$ 'I))))

(deftest micro_t1_div_truncates
  (prog "        I = 7 / 2" "end")
  (is (clojure.core/= 3 ($$ 'I))))

(deftest micro_t1_div_neg_truncates
  (prog "        I = -7 / 2" "end")
  (is (clojure.core/= -3 ($$ 'I))))

(deftest micro_t1_add_var_lit
  (prog "        J = I + 1" "end")
  (is (clojure.core/= 6 ($$ 'J))))   ; I=5

(deftest micro_t1_sub_var_lit
  (prog "        J = I - 2" "end")
  (is (clojure.core/= 3 ($$ 'J))))   ; I=5

(deftest micro_t1_mul_var_lit
  (prog "        J = I * 3" "end")
  (is (clojure.core/= 15 ($$ 'J))))  ; I=5

(deftest micro_t1_add_two_vars
  (prog "        K = I + J" "end")
  (is (clojure.core/= 15 ($$ 'K))))  ; I=5 J=10

;; REMDR
(deftest micro_t1_remdr_basic
  (prog "        I = REMDR(7,3)" "end")
  (is (clojure.core/= 1 ($$ 'I))))

(deftest micro_t1_remdr_zero
  (prog "        I = REMDR(6,3)" "end")
  (is (clojure.core/= 0 ($$ 'I))))

;; SIZE
(deftest micro_t1_size_literal
  (prog "        I = SIZE('hello')" "end")
  (is (clojure.core/= 5 ($$ 'I))))

(deftest micro_t1_size_empty
  (prog "        I = SIZE('')" "end")
  (is (clojure.core/= 0 ($$ 'I))))

(deftest micro_t1_size_var
  (prog "        I = SIZE(S)" "end")
  (is (clojure.core/= 5 ($$ 'I))))   ; S='hello'

;; DATATYPE
(deftest micro_t1_datatype_int
  (prog "        S = DATATYPE(I)" "end")
  (is (clojure.core/= "INTEGER" ($$ 'S))))

(deftest micro_t1_datatype_str
  (prog "        S = DATATYPE(T)" "end")
  (is (clojure.core/= "STRING" ($$ 'S))))   ; T='world'

(deftest micro_t1_datatype_real
  (prog "        A = 1.5" "        S = DATATYPE(A)" "end")
  (is (clojure.core/= "REAL" ($$ 'S))))

;; Unary minus
(deftest micro_t1_unary_minus_lit
  (prog "        I = -5" "end")
  (is (clojure.core/= -5 ($$ 'I))))

(deftest micro_t1_unary_minus_var
  (prog "        J = -I" "end")
  (is (clojure.core/= -5 ($$ 'J))))  ; I=5

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 1b — Left-to-right evaluation (no operator precedence)
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t1_ltr_add_mul
  "2 + 3 * 4 = 14 (* binds tighter than + per v311.sil prec table)"
  (prog "        I = 2 + 3 * 4" "end")
  (is (clojure.core/= 14 ($$ 'I))))

(deftest micro_t1_ltr_mul_add
  "2 * 3 + 4 = (2*3)+4 = 10"
  (prog "        I = 2 * 3 + 4" "end")
  (is (clojure.core/= 10 ($$ 'I))))

(deftest micro_t1_ltr_sub_sub
  "10 - 3 - 2 = (10-3)-2 = 5"
  (prog "        I = 10 - 3 - 2" "end")
  (is (clojure.core/= 5 ($$ 'I))))

(deftest micro_t1_ltr_div_div
  "8 / 2 / 2 = (8/2)/2 = 2"
  (prog "        I = 8 / 2 / 2" "end")
  (is (clojure.core/= 2 ($$ 'I))))

(deftest micro_t1_ltr_three_ops
  "1 + 2 + 3 + 4 = 10"
  (prog "        I = 1 + 2 + 3 + 4" "end")
  (is (clojure.core/= 10 ($$ 'I))))

(deftest micro_t1_ltr_parens_override
  "2 * (3 + 4) = 14 via parens — but SNOBOL4 parens are for precedence override"
  (prog "        I = 2 * (3 + 4)" "end")
  (is (clojure.core/= 14 ($$ 'I))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 1c — String concatenation
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t1_concat_two_lits
  (prog "        S = 'foo' 'bar'" "end")
  (is (clojure.core/= "foobar" ($$ 'S))))

(deftest micro_t1_concat_three_lits
  (prog "        S = 'a' 'b' 'c'" "end")
  (is (clojure.core/= "abc" ($$ 'S))))

(deftest micro_t1_concat_var_lit
  (prog "        T = S ' world'" "end")
  (is (clojure.core/= "hello world" ($$ 'T))))  ; S='hello'

(deftest micro_t1_concat_lit_var
  (prog "        T = 'say: ' S" "end")
  (is (clojure.core/= "say: hello" ($$ 'T))))

(deftest micro_t1_concat_two_vars
  (prog "        Z = S T" "end")
  (is (clojure.core/= "helloworld" ($$ 'Z))))   ; S='hello' T='world'

(deftest micro_t1_concat_with_space
  (prog "        Z = S ' ' T" "end")
  (is (clojure.core/= "hello world" ($$ 'Z))))

(deftest micro_t1_concat_empty_left
  (prog "        Z = '' S" "end")
  (is (clojure.core/= "hello" ($$ 'Z))))

(deftest micro_t1_concat_empty_right
  (prog "        Z = S ''" "end")
  (is (clojure.core/= "hello" ($$ 'Z))))

(deftest micro_t1_concat_int_str
  "Integer coerced to string in concat"
  (prog "        Z = I ' items'" "end")
  (is (clojure.core/= "5 items" ($$ 'Z))))   ; I=5

(deftest micro_t1_size_of_concat
  (prog
    "        Z = S ' ' T"
    "        I = SIZE(Z)"
    "end")
  (is (clojure.core/= 11 ($$ 'I))))  ; "hello world" = 11

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 1d — String functions (single call)
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t1_reverse
  (prog "        T = REVERSE('hello')" "end")
  (is (clojure.core/= "olleh" ($$ 'T))))

(deftest micro_t1_reverse_empty
  (prog "        T = REVERSE('')" "end")
  (is (clojure.core/= "" ($$ 'T))))

(deftest micro_t1_reverse_single
  (prog "        T = REVERSE('x')" "end")
  (is (clojure.core/= "x" ($$ 'T))))

(deftest micro_t1_reverse_var
  (prog "        T = REVERSE(S)" "end")
  (is (clojure.core/= "olleh" ($$ 'T))))   ; S='hello'

(deftest micro_t1_dupl_basic
  (prog "        S = DUPL('ab',3)" "end")
  (is (clojure.core/= "ababab" ($$ 'S))))

(deftest micro_t1_dupl_zero
  (prog "        S = DUPL('ab',0)" "end")
  (is (clojure.core/= "" ($$ 'S))))

(deftest micro_t1_dupl_one
  (prog "        S = DUPL('x',1)" "end")
  (is (clojure.core/= "x" ($$ 'S))))

(deftest micro_t1_trim_trailing
  "TRIM removes trailing blanks only"
  (prog "        S = TRIM('hello   ')" "end")
  (is (clojure.core/= "hello" ($$ 'S))))

(deftest micro_t1_trim_leading_preserved
  "TRIM does NOT remove leading blanks"
  (prog "        S = TRIM('  hi  ')" "end")
  (is (clojure.core/= "  hi" ($$ 'S))))

(deftest micro_t1_trim_no_spaces
  (prog "        S = TRIM('abc')" "end")
  (is (clojure.core/= "abc" ($$ 'S))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 1e — Integer / Real / String coercion
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t1_integer_from_str
  (prog "        I = INTEGER('42')" "end")
  (is (clojure.core/= 42 ($$ 'I))))

(deftest micro_t1_integer_from_int
  (prog "        I = INTEGER(7)" "end")
  (is (clojure.core/= 7 ($$ 'I))))

(deftest micro_t1_string_from_int
  (prog "        S = STRING(42)" "end")
  (is (clojure.core/= "42" ($$ 'S))))

(deftest micro_t1_string_from_str
  (prog "        S = STRING('hi')" "end")
  (is (clojure.core/= "hi" ($$ 'S))))

(deftest micro_t1_coerce_str_arith
  "String '10' + 5 = 15 via implicit coercion"
  (prog "        I = '10' + 5" "end")
  (is (clojure.core/= 15 ($$ 'I))))

(deftest micro_t1_coerce_int_as_str_size
  "INTEGER coerced to string for SIZE"
  (prog "        I = SIZE(42)" "end")
  (is (clojure.core/= 2 ($$ 'I))))  ; "42" has 2 chars

;; ASCII / CHAR
(deftest micro_t1_ascii_a
  (prog "        I = ASCII('a')" "end")
  (is (clojure.core/= 97 ($$ 'I))))

(deftest micro_t1_ascii_A
  (prog "        I = ASCII('A')" "end")
  (is (clojure.core/= 65 ($$ 'I))))

(deftest micro_t1_char_65
  (prog "        S = CHAR(65)" "end")
  (is (clojure.core/= "A" ($$ 'S))))

(deftest micro_t1_char_97
  (prog "        S = CHAR(97)" "end")
  (is (clojure.core/= "a" ($$ 'S))))

(deftest micro_t1_ascii_char_roundtrip
  (prog
    "        I = ASCII('Z')"
    "        S = CHAR(I)"
    "end")
  (is (clojure.core/= "Z" ($$ 'S))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 2 — Comparisons and branching (21–40 chars)
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t2_eq_true
  (prog
    "        EQ(5,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_eq_false
  (prog
    "        EQ(5,6) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_ne_true
  (prog
    "        NE(5,6) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_ne_false
  (prog
    "        NE(5,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_lt_true
  (prog
    "        LT(3,7) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_lt_false
  (prog
    "        LT(7,3) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_gt_true
  (prog
    "        GT(10,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_gt_false
  (prog
    "        GT(5,10) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_le_equal
  (prog
    "        LE(5,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_le_less
  (prog
    "        LE(4,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_le_greater
  (prog
    "        LE(6,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_ge_equal
  (prog
    "        GE(5,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_ge_greater
  (prog
    "        GE(6,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_ge_less
  (prog
    "        GE(4,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

;; String comparisons
(deftest micro_t2_ident_true
  (prog
    "        IDENT('abc','abc') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_ident_false
  (prog
    "        IDENT('abc','xyz') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_ident_null_null
  (prog
    "        IDENT('','') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_differ_true
  (prog
    "        DIFFER('abc','xyz') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_differ_false
  (prog
    "        DIFFER('abc','abc') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_lgt_true
  "LGT: left is lexically greater than right"
  (prog
    "        LGT('b','a') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_lgt_false
  (prog
    "        LGT('a','b') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_lgt_equal
  (prog
    "        LGT('abc','abc') :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

;; Comparison with vars from fixture
(deftest micro_t2_cmp_fixtures_i_lt_j
  (prog
    "        LT(I,J) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))    ; I=5 J=10

(deftest micro_t2_cmp_fixtures_gt_k
  (prog
    "        GT(I,K) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))    ; I=5 K=3

;; Negation ~
(deftest micro_t2_negate_eq_true
  "~EQ(5,5) fails, so F branch taken"
  (prog
    "        ~EQ(5,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_negate_eq_false
  "~EQ(5,6) succeeds (negation of failure), so S branch taken"
  (prog
    "        ~EQ(5,6) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_negate_lt
  "~LT(10,5) — LT fails, negation succeeds"
  (prog
    "        ~LT(10,5) :S(YES)F(NO)"
    "YES     S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 2b — Unconditional and conditional goto
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t2_goto_unconditional
  (prog
    "        :(SKIP)"
    "        S = 'wrong'"
    "SKIP    S = 'right'"
    "end")
  (is (clojure.core/= "right" ($$ 'S))))

(deftest micro_t2_goto_success_branch
  (prog
    "        EQ(1,1) :S(YES)"
    "        S = 'no'"
    "        :(DONE)"
    "YES     S = 'yes'"
    "DONE    end")
  (is (clojure.core/= "yes" ($$ 'S))))

(deftest micro_t2_goto_fail_branch
  (prog
    "        EQ(1,2) :F(NO)"
    "        S = 'yes'"
    "        :(DONE)"
    "NO      S = 'no'"
    "DONE    end")
  (is (clojure.core/= "no" ($$ 'S))))

(deftest micro_t2_goto_sf_both
  (prog
    "        GT(I,J) :S(BIG)F(SMALL)"
    "BIG     S = 'big'"
    "        :(DONE)"
    "SMALL   S = 'small'"
    "DONE    end")
  (is (clojure.core/= "small" ($$ 'S))))    ; I=5 J=10

(deftest micro_t2_goto_fall_through
  "No goto — falls through to next statement"
  (prog
    "        I = 1"
    "        J = 2"
    "        K = I + J"
    "end")
  (is (clojure.core/= 3 ($$ 'K))))

(deftest micro_t2_end_label_terminates
  "Goto END terminates program"
  (prog
    "        S = 'before'"
    "        :(END)"
    "        S = 'after'"
    "end")
  (is (clojure.core/= "before" ($$ 'S))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 3 — Pattern matching primitives
;; ─────────────────────────────────────────────────────────────────────────────

;; LEN
(deftest micro_t3_len_0
  (prog
    "        'hello' LEN(0) . T"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

(deftest micro_t3_len_1
  (prog
    "        'hello' LEN(1) . T"
    "end")
  (is (clojure.core/= "h" ($$ 'T))))

(deftest micro_t3_len_3
  (prog
    "        'hello' LEN(3) . T"
    "end")
  (is (clojure.core/= "hel" ($$ 'T))))

(deftest micro_t3_len_all
  (prog
    "        'hello' LEN(5) . T"
    "end")
  (is (clojure.core/= "hello" ($$ 'T))))

(deftest micro_t3_len_too_long_fails
  (prog
    "        'hi' LEN(10) :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

(deftest micro_t3_len_from_int_var
  (prog
    "        'hello' LEN(K) . T"   ; K=3
    "end")
  (is (clojure.core/= "hel" ($$ 'T))))

;; POS
(deftest micro_t3_pos_0
  (prog
    "        'hello' POS(0) LEN(2) . T"
    "end")
  (is (clojure.core/= "he" ($$ 'T))))

(deftest micro_t3_pos_2
  (prog
    "        'hello' POS(2) LEN(2) . T"
    "end")
  (is (clojure.core/= "ll" ($$ 'T))))

(deftest micro_t3_pos_end
  "POS at end — matches zero-width, captures empty string"
  (prog
    "        'abc' POS(3) LEN(0) . T"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

(deftest micro_t3_pos_fails_when_past_end
  (prog
    "        'abc' POS(5) :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

;; RPOS
(deftest micro_t3_rpos_0
  "RPOS(0) matches at the very end"
  (prog
    "        'hello' RTAB(0) . T RPOS(0)"
    "end")
  (is (clojure.core/= "hello" ($$ 'T))))

(deftest micro_t3_rpos_1
  "RPOS(1) matches one from end"
  (prog
    "        'hello' RPOS(1) LEN(1) . T"
    "end")
  (is (clojure.core/= "o" ($$ 'T))))

(deftest micro_t3_rpos_2
  (prog
    "        'hello' RPOS(2) LEN(2) . T"
    "end")
  (is (clojure.core/= "lo" ($$ 'T))))

;; TAB / RTAB
(deftest micro_t3_tab_3
  (prog
    "        'hello' TAB(3) . T"
    "end")
  (is (clojure.core/= "hel" ($$ 'T))))

(deftest micro_t3_tab_0
  (prog
    "        'hello' TAB(0) . T"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

(deftest micro_t3_rtab_0
  (prog
    "        'hello' RTAB(0) . T"
    "end")
  (is (clojure.core/= "hello" ($$ 'T))))

(deftest micro_t3_rtab_2
  (prog
    "        'hello' RTAB(2) . T"
    "end")
  (is (clojure.core/= "hel" ($$ 'T))))

;; ANY / NOTANY
(deftest micro_t3_any_hit
  (prog
    "        'hello' ANY('aeiou') . T"
    "end")
  (is (clojure.core/= "e" ($$ 'T))))

(deftest micro_t3_any_first_char
  "ANY matches first char when it's in set"
  (prog
    "        'abc' ANY('abc') . T"
    "end")
  (is (clojure.core/= "a" ($$ 'T))))

(deftest micro_t3_any_miss
  (prog
    "        'xyz' ANY('aeiou') :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

(deftest micro_t3_notany_hit
  (prog
    "        'hello' NOTANY('aeiou') . T"
    "end")
  (is (clojure.core/= "h" ($$ 'T))))

(deftest micro_t3_notany_miss
  (prog
    "        'aeiou' NOTANY('aeiou') :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

;; SPAN / BREAK
(deftest micro_t3_span_all
  (prog
    "        'abc' SPAN('abc') . T"
    "end")
  (is (clojure.core/= "abc" ($$ 'T))))

(deftest micro_t3_span_partial
  (prog
    "        'abcxyz' SPAN('abc') . T"
    "end")
  (is (clojure.core/= "abc" ($$ 'T))))

(deftest micro_t3_span_one
  (prog
    "        'abcxyz' SPAN('a') . T"
    "end")
  (is (clojure.core/= "a" ($$ 'T))))

(deftest micro_t3_span_empty_fails
  "SPAN requires at least 1 char match"
  (prog
    "        'xyz' SPAN('abc') :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

(deftest micro_t3_break_basic
  (prog
    "        'abcdef' BREAK('d') . T"
    "end")
  (is (clojure.core/= "abc" ($$ 'T))))

(deftest micro_t3_break_first_char
  (prog
    "        'defgh' BREAK('d') . T"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

(deftest micro_t3_break_not_found_fails
  (prog
    "        'abc' BREAK('z') :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

;; REM
(deftest micro_t3_rem_basic
  (prog
    "        'hello world' 'hello ' REM . T"
    "end")
  (is (clojure.core/= "world" ($$ 'T))))

(deftest micro_t3_rem_all
  (prog
    "        'hello' REM . T"
    "end")
  (is (clojure.core/= "hello" ($$ 'T))))

(deftest micro_t3_rem_empty
  (prog
    "        '' REM . T"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

;; Literal pattern match
(deftest micro_t3_literal_hit
  (prog
    "        'hello world' 'world' :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "hit" ($$ 'S))))

(deftest micro_t3_literal_miss
  (prog
    "        'hello world' 'xyz' :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

(deftest micro_t3_literal_at_start
  (prog
    "        'abcdef' 'abc' :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "hit" ($$ 'S))))

(deftest micro_t3_literal_empty_always_hits
  (prog
    "        'anything' '' :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "hit" ($$ 'S))))

(deftest micro_t3_literal_empty_subject
  (prog
    "        '' '' :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "hit" ($$ 'S))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 3b — Capture operators . and $
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t3_dot_capture_basic
  (prog
    "        'hello' LEN(3) . T"
    "end")
  (is (clojure.core/= "hel" ($$ 'T))))

(deftest micro_t3_dot_not_assigned_on_fail
  "Capture var unchanged when match fails"
  (prog
    "        T = 'initial'"
    "        'abc' 'xyz' . T"
    "end")
  (is (clojure.core/= "initial" ($$ 'T))))

(deftest micro_t3_dollar_immediate_capture
  (prog
    "        'hello' ANY('aeiou') $ T"
    "end")
  (is (clojure.core/= "e" ($$ 'T))))

(deftest micro_t3_dot_mid_pattern
  "Capture middle portion"
  (prog
    "        'hello world' LEN(6) . T LEN(5)"
    "end")
  (is (clojure.core/= "hello " ($$ 'T))))

(deftest micro_t3_two_captures
  (prog
    "        'abcdef' LEN(3) . S LEN(3) . T"
    "end")
  (is (clojure.core/= "abc" ($$ 'S)))
  (is (clojure.core/= "def" ($$ 'T))))

(deftest micro_t3_capture_span
  (prog
    "        'hello world' SPAN('abcdefghijklmnopqrstuvwxyz') . S"
    "end")
  (is (clojure.core/= "hello" ($$ 'S))))

(deftest micro_t3_capture_any
  (prog
    "        'hello' ANY('hH') . T"
    "end")
  (is (clojure.core/= "h" ($$ 'T))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 3c — Pattern replace
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t3_replace_literal
  (prog
    "        S = 'hello world'"
    "        S 'world' = 'there'"
    "end")
  (is (clojure.core/= "hello there" ($$ 'S))))

(deftest micro_t3_replace_first_only
  "Pattern replace replaces first occurrence"
  (prog
    "        S = 'aabbaa'"
    "        S 'aa' = 'XX'"
    "end")
  (is (clojure.core/= "XXbbaa" ($$ 'S))))

(deftest micro_t3_replace_with_empty
  (prog
    "        S = 'hello world'"
    "        S 'world' ="
    "end")
  (is (clojure.core/= "hello " ($$ 'S))))

(deftest micro_t3_replace_with_empty_quotes
  (prog
    "        S = 'hello world'"
    "        S ' world' = ''"
    "end")
  (is (clojure.core/= "hello" ($$ 'S))))

(deftest micro_t3_replace_pattern_any
  (prog
    "        S = 'hello'"
    "        S ANY('aeiou') = '*'"
    "end")
  (is (clojure.core/= "h*llo" ($$ 'S))))

(deftest micro_t3_replace_len
  (prog
    "        S = 'hello'"
    "        S LEN(2) = 'XX'"
    "end")
  (is (clojure.core/= "XXllo" ($$ 'S))))

(deftest micro_t3_replace_no_match_unchanged
  (prog
    "        S = 'hello'"
    "        S 'xyz' = 'replaced'"
    "end")
  (is (clojure.core/= "hello" ($$ 'S))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 3d — Alternation |
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t3_alt_first_branch
  (prog
    "        'cat' ('cat' | 'dog') . T"
    "end")
  (is (clojure.core/= "cat" ($$ 'T))))

(deftest micro_t3_alt_second_branch
  (prog
    "        'dog' ('cat' | 'dog') . T"
    "end")
  (is (clojure.core/= "dog" ($$ 'T))))

(deftest micro_t3_alt_neither_fails
  (prog
    "        'fish' ('cat' | 'dog') :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "miss" ($$ 'S))))

(deftest micro_t3_alt_three_branches
  (prog
    "        'c' ('a' | 'b' | 'c') . T"
    "end")
  (is (clojure.core/= "c" ($$ 'T))))

(deftest micro_t3_alt_with_len
  (prog
    "        'hi' (LEN(1) | LEN(2)) . T"
    "end")
  (is (clojure.core/= "h" ($$ 'T))))  ; first alt matches

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 3e — ARB
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t3_arb_shortest_first
  "ARB matches shortest first (zero chars)"
  (prog
    "        'hello' ARB . T 'h'"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

(deftest micro_t3_arb_before_literal
  "ARB grows to find literal after it"
  (prog
    "        'hello' ARB . T 'lo'"
    "end")
  (is (clojure.core/= "hel" ($$ 'T))))

(deftest micro_t3_arb_full_string
  (prog
    "        'hello' ARB . T RPOS(0)"
    "end")
  (is (clojure.core/= "hello" ($$ 'T))))

(deftest micro_t3_arb_empty_string
  (prog
    "        '' ARB . T"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 4 — Loops (41–64 chars body per iteration)
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t4_loop_count_to_3
  (prog-steplimit 2000 200
    "        I = 0"
    "LOOP    I = I + 1"
    "        LE(I,3) :S(LOOP)"
    "end")
  (is (clojure.core/= 4 ($$ 'I))))

(deftest micro_t4_loop_count_to_5
  (prog-steplimit 2000 500
    "        I = 1"
    "LOOP    I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (clojure.core/= 6 ($$ 'I))))

(deftest micro_t4_loop_sum
  (prog-steplimit 2000 500
    "        I = 1"
    "        S = 0"
    "LOOP    S = S + I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (clojure.core/= 15 ($$ 'S))))   ; 1+2+3+4+5 = 15

(deftest micro_t4_loop_product
  (prog-steplimit 2000 500
    "        I = 1"
    "        P = 1"
    "LOOP    P = P * I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (clojure.core/= 120 ($$ 'P))))  ; 5! = 120

(deftest micro_t4_loop_string_build
  (prog-steplimit 2000 500
    "        I = 0"
    "        S = ''"
    "LOOP    S = S 'x'"
    "        I = I + 1"
    "        LT(I,4) :S(LOOP)"
    "end")
  (is (clojure.core/= "xxxx" ($$ 'S))))

(deftest micro_t4_loop_zero_iters
  "LT(I,5) fails after one body exec; oracle=11 (known: engine may loop)"
  (prog-steplimit 2000 500
    "        I = 10"
    "LOOP    I = I + 1"
    "        LT(I,5) :S(LOOP)"
    "end")
  (is (clojure.core/= 11 ($$ 'I))))

(deftest micro_t4_nested_cond_in_loop
  (prog-steplimit 2000 500
    "        I = 1"
    "        J = 0"
    "LOOP    EQ(REMDR(I,2),0) :S(EVEN)"
    "        J = J + 1"
    "EVEN    I = I + 1"
    "        LE(I,6) :S(LOOP)"
    "end")
  (is (clojure.core/= 3 ($$ 'J))))   ; odds in 1-6: 1,3,5 = 3

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 4b — DEFINE / CALL / RETURN
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t4_define_simple_fn
  (prog
    "        DEFINE('double(N)')"
    "        :(ENDDOUBLE)"
    "DOUBLE  double = N * 2"
    "        :(RETURN)"
    "ENDDOUBLE"
    "        I = double(7)"
    "end")
  (is (clojure.core/= 14 ($$ 'I))))

(deftest micro_t4_define_fn_two_args
  (prog
    "        DEFINE('add(A,B)')"
    "        :(ENDADD)"
    "ADD     add = A + B"
    "        :(RETURN)"
    "ENDADD"
    "        K = add(3,4)"
    "end")
  (is (clojure.core/= 7 ($$ 'K))))

(deftest micro_t4_define_string_fn
  (prog
    "        DEFINE('greet(NAME)')"
    "        :(ENDGREET)"
    "GREET   greet = 'Hello, ' NAME"
    "        :(RETURN)"
    "ENDGREET"
    "        S = greet('world')"
    "end")
  (is (clojure.core/= "Hello, world" ($$ 'S))))

(deftest micro_t4_define_with_local
  (prog
    "        DEFINE('square(N),tmp')"
    "        :(ENDSQ)"
    "SQUARE  tmp = N * N"
    "        square = tmp"
    "        :(RETURN)"
    "ENDSQ"
    "        I = square(6)"
    "end")
  (is (clojure.core/= 36 ($$ 'I))))

(deftest micro_t4_freturn_on_failure
  (prog
    "        DEFINE('safe_div(A,B)')"
    "        :(ENDSD)"
    "SAFE_DIV EQ(B,0) :S(DIVZERO)"
    "        safe_div = A / B"
    "        :(RETURN)"
    "DIVZERO safe_div = -1"
    "        :(FRETURN)"
    "ENDSD"
    "        I = safe_div(10,2)"
    "        J = safe_div(10,0)"
    "end")
  (is (clojure.core/= 5 ($$ 'I)))
  (is (clojure.core/= 10 ($$ 'J))))  ; J stays at fixture 10 — FRETURN makes statement fail


;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 4c — ARBNO
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t4_arbno_zero_reps
  "ARBNO matches zero reps first"
  (prog
    "        'xyz' ARBNO('a') . T 'x'"
    "end")
  (is (clojure.core/= "" ($$ 'T))))

(deftest micro_t4_arbno_forced_reps
  "'x' ARBNO('ab') . T 'x' forces ARBNO to match all abs between xs"
  (prog
    "        'xabababx' 'x' ARBNO('ab') . T 'x'"
    "end")
  (is (clojure.core/= "ababab" ($$ 'T))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 4b — TABLE
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t4_table_basic
  (prog
    "        T1 = TABLE()"
    "        T1<'key'> = 'value'"
    "        S = T1<'key'>"
    "end")
  (is (clojure.core/= "value" ($$ 'S))))

(deftest micro_t4_table_overwrite
  (prog
    "        T1 = TABLE()"
    "        T1<'x'> = 'first'"
    "        T1<'x'> = 'second'"
    "        S = T1<'x'>"
    "end")
  (is (clojure.core/= "second" ($$ 'S))))

(deftest micro_t4_table_missing_key_null
  (prog
    "        T1 = TABLE()"
    "        I = SIZE(T1<'absent'>)"
    "end")
  (is (clojure.core/= 0 ($$ 'I))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 4c — ARRAY
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t4_array_basic
  (prog
    "        A1 = ARRAY(5)"
    "        A1<1> = 'first'"
    "        A1<5> = 'last'"
    "        S = A1<1> ' ' A1<5>"
    "end")
  (is (clojure.core/= "first last" ($$ 'S))))

(deftest micro_t4_array_default_null
  (prog
    "        A1 = ARRAY(3)"
    "        I = SIZE(A1<2>)"
    "end")
  (is (clojure.core/= 0 ($$ 'I))))

(deftest micro_t4_array_loop_fill
  (prog-steplimit 2000 200
    "        A1 = ARRAY(5)"
    "        I = 1"
    "LOOP    A1<I> = I * I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "        S = A1<3>"
    "end")
  (is (clojure.core/= 9 ($$ 'S))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 4d — DEFINE / RETURN / FRETURN
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t4_define_simple_fn
  (prog
    "        DEFINE('double(N)')"
    "        :(ENDDOUBLE)"
    "DOUBLE  double = N * 2"
    "        :(RETURN)"
    "ENDDOUBLE"
    "        I = double(7)"
    "end")
  (is (clojure.core/= 14 ($$ 'I))))

(deftest micro_t4_define_fn_two_args
  (prog
    "        DEFINE('add(A,B)')"
    "        :(ENDADD)"
    "ADD     add = A + B"
    "        :(RETURN)"
    "ENDADD"
    "        K = add(3,4)"
    "end")
  (is (clojure.core/= 7 ($$ 'K))))

(deftest micro_t4_define_string_fn
  (prog
    "        DEFINE('greet(NAME)')"
    "        :(ENDGREET)"
    "GREET   greet = 'Hello, ' NAME"
    "        :(RETURN)"
    "ENDGREET"
    "        S = greet('world')"
    "end")
  (is (clojure.core/= "Hello, world" ($$ 'S))))

(deftest micro_t4_freturn_on_zero_div
  (prog
    "        DEFINE('safe_div(A,B)')"
    "        :(ENDSD)"
    "SAFE_DIV EQ(B,0) :S(DIVZERO)"
    "        safe_div = A / B"
    "        :(RETURN)"
    "DIVZERO safe_div = -1"
    "        :(FRETURN)"
    "ENDSD"
    "        I = safe_div(10,2)"
    "        J = safe_div(10,0)"
    "end")
  (is (clojure.core/= 5 ($$ 'I)))
  (is (clojure.core/= 10 ($$ 'J))))  ; J stays at fixture 10 — FRETURN makes statement fail


;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 5 — Output-verified oracle tests
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t5_output_ltr_arith
  "2 + 3 * 4 = 14 (* higher prec than + per v311.sil)"
  (let [out (:stdout (prog "        OUTPUT = 2 + 3 * 4" "end"))]
    (is (clojure.core/= "14\n" out))))

(deftest micro_t5_output_concat
  (let [out (:stdout (prog "        OUTPUT = 'foo' 'bar'" "end"))]
    (is (clojure.core/= "foobar\n" out))))

(deftest micro_t5_output_multi_line
  (let [out (:stdout (prog
                "        OUTPUT = 'line1'"
                "        OUTPUT = 'line2'"
                "        OUTPUT = 'line3'"
                "end"))]
    (is (clojure.core/= "line1\nline2\nline3\n" out))))

(deftest micro_t5_output_loop_1_to_4
  (let [out (:stdout (prog-steplimit 2000 200
                "        I = 1"
                "LOOP    OUTPUT = I"
                "        I = I + 1"
                "        LE(I,4) :S(LOOP)"
                "end"))]
    (is (clojure.core/= "1\n2\n3\n4\n" out))))

(deftest micro_t5_output_fibonacci
  "F(6) = 8"
  (let [out (:stdout (prog-steplimit 2000 500
                "        A = 0"
                "        B = 1"
                "        I = 2"
                "LOOP    C = A + B"
                "        A = B"
                "        B = C"
                "        I = I + 1"
                "        LE(I,6) :S(LOOP)"
                "        OUTPUT = B"
                "end"))]
    (is (clojure.core/= "8\n" out))))

(deftest micro_t5_output_pattern_capture
  (let [out (:stdout (prog
                "        'hello world' SPAN('abcdefghijklmnopqrstuvwxyz') . S"
                "        OUTPUT = S"
                "end"))]
    (is (clojure.core/= "hello\n" out))))

(deftest micro_t5_output_replace
  (let [out (:stdout (prog
                "        S = 'hello world'"
                "        S 'world' = 'there'"
                "        OUTPUT = S"
                "end"))]
    (is (clojure.core/= "hello there\n" out))))

(deftest micro_t5_output_remdr_loop
  (let [out (:stdout (prog-steplimit 2000 200
                "        I = 1"
                "LOOP    OUTPUT = REMDR(10,I)"
                "        I = I + 1"
                "        LE(I,4) :S(LOOP)"
                "end"))]
    (is (clojure.core/= "0\n0\n1\n2\n" out))))

;; ─────────────────────────────────────────────────────────────────────────────
;; TIER 5b — Edge cases
;; ─────────────────────────────────────────────────────────────────────────────

(deftest micro_t5_int_to_str_in_match
  (prog
    "        I = 42"
    "        I '4' :S(HIT)F(MISS)"
    "HIT     S = 'hit'"
    "        :(DONE)"
    "MISS    S = 'miss'"
    "DONE    end")
  (is (clojure.core/= "hit" ($$ 'S))))

(deftest micro_t5_str_to_int_in_arith
  (prog
    "        S = '5'"
    "        I = S + 3"
    "end")
  (is (clojure.core/= 8 ($$ 'I))))

(deftest micro_t5_null_str_size_zero
  (prog
    "        S ="
    "        I = SIZE(S)"
    "end")
  (is (clojure.core/= 0 ($$ 'I))))

(deftest micro_t5_pattern_var_in_match
  (prog
    "        P = LEN(3)"
    "        'hello' P . T"
    "end")
  (is (clojure.core/= "hel" ($$ 'T))))

(deftest micro_t5_replace_function
  (prog
    "        S = REPLACE('hello','aeiou','AEIOU')"
    "end")
  (is (clojure.core/= "hEllO" ($$ 'S))))

(deftest micro_t5_two_captures_sequential
  (prog
    "        'abcdef' LEN(3) . S LEN(3) . T"
    "end")
  (is (clojure.core/= "abc" ($$ 'S)))
  (is (clojure.core/= "def" ($$ 'T))))

(deftest micro_t5_break_then_any
  (prog
    "        'abcdef' BREAK('d') . S ANY('def') . T"
    "end")
  (is (clojure.core/= "abc" ($$ 'S)))
  (is (clojure.core/= "d" ($$ 'T))))

(deftest micro_t5_pos_tab_window
  (prog
    "        'abcdef' POS(2) TAB(4) . T"
    "end")
  (is (clojure.core/= "cd" ($$ 'T))))

(deftest micro_t5_recursive_factorial
  (prog-steplimit 2000 1000
    "        DEFINE('fact(N)')"
    "        :(ENDFACT)"
    "FACT    EQ(N,0) :S(BASE)"
    "        fact = N * fact(N - 1)"
    "        :(RETURN)"
    "BASE    fact = 1"
    "        :(RETURN)"
    "ENDFACT"
    "        I = fact(5)"
    "end")
  (is (clojure.core/= 120 ($$ 'I))))

(deftest micro_t5_overwrite_chain
  (prog
    "        S = 'first'"
    "        S = 'second'"
    "        S = 'third'"
    "end")
  (is (clojure.core/= "third" ($$ 'S))))

(deftest micro_t5_multi_assign_increment
  (prog
    "        I = 1"
    "        I = I + 1"
    "        I = I + 1"
    "        I = I + 1"
    "end")
  (is (clojure.core/= 4 ($$ 'I))))
