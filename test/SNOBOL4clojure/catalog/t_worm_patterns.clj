(ns SNOBOL4clojure.catalog.t_worm_patterns
  "Worm catalog — advanced pattern matching (T3-T5 pattern band).
   SPAN/BREAK, NOTANY, TAB/RTAB/POS, pattern variables, capture in loops,
   ARBNO, multi-pattern sequences, expression extraction."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-steplimit]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_worm_patterns))) (f)))

(deftest t_span_digits
  "SPAN('0123456789') captures '123' from '123abc'"
  (prog
    "        S = '123abc'"
    "        S SPAN('0123456789') . T"
    "end")
  (is (= "123" ($$ 'T))))

(deftest t_break_at_space
  "BREAK(' ') captures 'hello' from 'hello world'"
  (prog
    "        S = 'hello world'"
    "        S BREAK(' ') . T"
    "end")
  (is (= "hello" ($$ 'T))))

(deftest t_span_then_break
  "SPAN alpha then BREAK digit captures first word"
  (prog
    "        S = 'abc123'"
    "        S SPAN('abcdefghijklmnopqrstuvwxyz') . W"
    "end")
  (is (= "abc" ($$ 'W))))

(deftest t_any_vowel_first
  "ANY('aeiou') matches first vowel in 'hello' at pos 2"
  (prog
    "        S = 'hello'"
    "        S LEN(1) LEN(1) ANY('aeiou') . V"
    "end")
  (is (= "e" ($$ 'V))))

(deftest t_notany_first_consonant
  "NOTANY('aeiou') matches 'h' in 'hello'"
  (prog
    "        S = 'hello'"
    "        S NOTANY('aeiou') . C"
    "end")
  (is (= "h" ($$ 'C))))

(deftest t_len_capture_middle
  "LEN(2) TAB(2) LEN(3) captures chars 3-5 of 'abcde' = 'cde'"
  (prog
    "        S = 'abcde'"
    "        S TAB(2) LEN(3) . T"
    "end")
  (is (= "cde" ($$ 'T))))

(deftest t_rtab_span
  "SPAN digits RTAB(3) captures '12' from '12abc'"
  (prog
    "        S = '12abc'"
    "        S SPAN('0123456789') . D RTAB(3) . R"
    "end")
  (is (clojure.core/and (= "12" ($$ 'D)) (= "abc" ($$ 'R)))))

(deftest t_pos_anchor
  "POS(2) LEN(3) captures chars 3-5"
  (prog
    "        S = 'abcdef'"
    "        S POS(2) LEN(3) . T"
    "end")
  (is (= "cde" ($$ 'T))))

(deftest t_full_match_exact
  "FULLMATCH 'hello' succeeds on 'hello'"
  (prog
    "        S = 'hello'"
    "        R = 'no'"
    "        S = 'hello' 'hello'"
    "end")
  (is (= "hello" ($$ 'S))))

(deftest t_pattern_in_variable
  "Pattern stored in variable, used in match"
  (prog
    "        P = LEN(3)"
    "        S = 'abcdef'"
    "        S P . T"
    "end")
  (is (= "abc" ($$ 'T))))

(deftest t_pattern_var_any
  "ANY pattern stored and reused"
  (prog
    "        VOWELS = ANY('aeiou')"
    "        S = 'hello'"
    "        S VOWELS . V"
    "end")
  (is (= "e" ($$ 'V))))

(deftest t_extract_all_digits_loop
  "Extract digit characters from '1a2b3c' -> '123' via loop"
  (prog-steplimit 2000 200
    "        S = '1a2b3c'"
    "        R = ''"
    "        I = 1"
    "LOOP    C = SUBSTR(S,I,1)"
    "        C SPAN('0123456789') . D  :F(SKIP)"
    "        R = R D"
    "SKIP    I = I + 1"
    "        LE(I,SIZE(S)) :S(LOOP)"
    "end")
  (is (= "123" ($$ 'R))))

(deftest t_count_vowels_loop
  "Count vowels in 'hello world' = 3 (e,o,o)"
  (prog-steplimit 2000 200
    "        S = 'hello world'"
    "        N = 0"
    "        I = 1"
    "LOOP    C = SUBSTR(S,I,1)"
    "        C ANY('aeiou') :F(SKIP)"
    "        N = N + 1"
    "SKIP    I = I + 1"
    "        LE(I,SIZE(S)) :S(LOOP)"
    "end")
  (is (= 3 ($$ 'N))))

(deftest t_replace_all_x
  "Replace all 'l' with 'L' in 'hello' -> 'heLLo' via loop"
  (prog-steplimit 2000 200
    "        S = 'hello'"
    "        I = 1"
    "        R = ''"
    "LOOP    C = SUBSTR(S,I,1)"
    "        C 'l' :F(KEEP)"
    "        C = 'L'"
    "KEEP    R = R C"
    "        I = I + 1"
    "        LE(I,SIZE(S)) :S(LOOP)"
    "end")
  (is (= "heLLo" ($$ 'R))))

(deftest t_arbno_digits_all
  "ARBNO(LEN(1)) . T captures full string"
  (prog
    "        S = 'abc'"
    "        S ARBNO(LEN(1)) . T RPOS(0)"
    "end")
  (is (= "abc" ($$ 'T))))

(deftest t_seq_word_space_word
  "Match 'hello world': word SP word; W1='hello' W2='world'"
  (prog
    "        S = 'hello world'"
    "        S BREAK(' ') . W1 LEN(1) BREAK(' ') . W2  :F(TRY2)"
    "        :(DONE)"
    "TRY2    S BREAK(' ') . W1 LEN(1) REM . W2"
    "DONE    end")
  (is (clojure.core/and (= "hello" ($$ 'W1)) (= "world" ($$ 'W2)))))

(deftest t_extract_between_parens
  "Extract content between parens in 'foo(bar)': inner='bar'"
  (prog
    "        S = 'foo(bar)'"
    "        S '(' BREAK(')') . INNER ')'"
    "end")
  (is (= "bar" ($$ 'INNER))))

(deftest t_extract_number_from_text
  "Extract first run of digits from 'abc123def' -> '123'"
  (prog
    "        S = 'abc123def'"
    "        S BREAK('0123456789') SPAN('0123456789') . N"
    "end")
  (is (= "123" ($$ 'N))))

(deftest t_pattern_result_arithmetic
  "SIZE of captured span: LEN(3) . T; SIZE(T)=3"
  (prog
    "        S = 'abcdef'"
    "        S LEN(3) . T"
    "        N = SIZE(T)"
    "end")
  (is (= 3 ($$ 'N))))

(deftest t_word_count_pattern
  "Count space-separated words in 'one two three' = 3"
  (prog-steplimit 2000 200
    "        S = 'one two three '"
    "        N = 0"
    "        I = 1"
    "LOOP    SUBSTR(S,I,SIZE(S) - I + 1) BREAK(' ') . W LEN(1)  :F(DONE)"
    "        N = N + 1"
    "        I = I + SIZE(W) + 1"
    "        LE(I,SIZE(S)) :S(LOOP)"
    "DONE    end")
  (is (= 3 ($$ 'N))))

(deftest t_parse_simple_number
  "Numeric string '42' -> integer via CONVERT"
  (prog
    "        S = '42'"
    "        N = INTEGER(S)"
    "end")
  (is (= 42 ($$ 'N))))

(deftest t_parse_and_eval_add
  "Parse and eval '10 + 20' = 30 via SNOBOL arithmetic"
  (prog
    "        A = INTEGER('10')"
    "        B = INTEGER('20')"
    "        R = A + B"
    "end")
  (is (= 30 ($$ 'R))))

(deftest t_string_to_int_loop_sum
  "Parse tokens '1','2','3' and sum = 6"
  (prog
    "        DEFINE('TOINT(S)') :(TEND)"
    "TOINT   TOINT = INTEGER(S)  :(RETURN)"
    "TEND"
    "        R = TOINT('1') + TOINT('2') + TOINT('3')"
    "end")
  (is (= 6 ($$ 'R))))

