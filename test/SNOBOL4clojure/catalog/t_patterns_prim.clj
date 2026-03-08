(ns SNOBOL4clojure.catalog.t_patterns_prim
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_patterns_prim))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_len_0_on_abcde
  "LEN(0) on 'abcde' captures ''"
  (prog
    "        'abcde' POS(0) LEN(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_len_1_on_abcde
  "LEN(1) on 'abcde' captures 'a'"
  (prog
    "        'abcde' POS(0) LEN(1) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "a" ($$ (quote C))))
)

(deftest worm_len_2_on_abcde
  "LEN(2) on 'abcde' captures 'ab'"
  (prog
    "        'abcde' POS(0) LEN(2) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "ab" ($$ (quote C))))
)

(deftest worm_len_3_on_abcde
  "LEN(3) on 'abcde' captures 'abc'"
  (prog
    "        'abcde' POS(0) LEN(3) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_len_5_on_abcde
  "LEN(5) on 'abcde' captures 'abcde'"
  (prog
    "        'abcde' POS(0) LEN(5) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abcde" ($$ (quote C))))
)

(deftest worm_len_6_on_abcde
  "LEN(6) on 'abcde' fails"
  (prog
    "        'abcde' POS(0) LEN(6) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_len_0_on_empty
  "LEN(0) on '' captures ''"
  (prog
    "        '' POS(0) LEN(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_len_1_on_empty
  "LEN(1) on '' fails"
  (prog
    "        '' POS(0) LEN(1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_any_aeiou_on_hello
  "ANY('aeiou') on 'hello' captures 'e'"
  (prog
    "        'hello' ANY('aeiou') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "e" ($$ (quote C))))
)

(deftest worm_any_aeiou_on_aeiou
  "ANY('aeiou') on 'aeiou' captures 'a'"
  (prog
    "        'aeiou' ANY('aeiou') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "a" ($$ (quote C))))
)

(deftest worm_any_aeiou_on_bcdfg
  "ANY('aeiou') on 'bcdfg' fails"
  (prog
    "        'bcdfg' ANY('aeiou') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_any_aeiou_on_empty
  "ANY('aeiou') on '' fails"
  (prog
    "        '' ANY('aeiou') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_any_a_on_aaa
  "ANY('a') on 'aaa' captures 'a'"
  (prog
    "        'aaa' ANY('a') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "a" ($$ (quote C))))
)

(deftest worm_any_abc_on_xyz
  "ANY('abc') on 'xyz' fails"
  (prog
    "        'xyz' ANY('abc') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_any_xyz_on_xyzabc
  "ANY('xyz') on 'xyzabc' captures 'x'"
  (prog
    "        'xyzabc' ANY('xyz') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "x" ($$ (quote C))))
)

(deftest worm_notany_aeiou_on_hello
  "NOTANY('aeiou') on 'hello' captures 'h'"
  (prog
    "        'hello' NOTANY('aeiou') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "h" ($$ (quote C))))
)

(deftest worm_notany_aeiou_on_aeiou
  "NOTANY('aeiou') on 'aeiou' fails"
  (prog
    "        'aeiou' NOTANY('aeiou') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_notany_aeiou_on_empty
  "NOTANY('aeiou') on '' fails"
  (prog
    "        '' NOTANY('aeiou') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_notany_abc_on_xyz
  "NOTANY('abc') on 'xyz' captures 'x'"
  (prog
    "        'xyz' NOTANY('abc') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "x" ($$ (quote C))))
)

(deftest worm_notany_xyz_on_xyz
  "NOTANY('xyz') on 'xyz' fails"
  (prog
    "        'xyz' NOTANY('xyz') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_notany_abc_on_abcdef
  "NOTANY('abc') on 'abcdef' fails"
  (prog
    "        'abcdef' NOTANY('abc') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_span_abc_on_abcdef
  "SPAN('abc') on 'abcdef' captures 'abc'"
  (prog
    "        'abcdef' SPAN('abc') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_span_abc_on_aaabbb
  "SPAN('abc') on 'aaabbb' captures 'aaabbb'"
  (prog
    "        'aaabbb' SPAN('abc') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "aaabbb" ($$ (quote C))))
)

(deftest worm_span_abc_on_xyz
  "SPAN('abc') on 'xyz' fails"
  (prog
    "        'xyz' SPAN('abc') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_span_abc_on_empty
  "SPAN('abc') on '' fails"
  (prog
    "        '' SPAN('abc') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_span_a_on_aaa
  "SPAN('a') on 'aaa' captures 'aaa'"
  (prog
    "        'aaa' SPAN('a') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "aaa" ($$ (quote C))))
)

(deftest worm_span_abc_on_abcxyz
  "SPAN('abc') on 'abcxyz' captures 'abc'"
  (prog
    "        'abcxyz' SPAN('abc') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_span_0123_on_123abc
  "SPAN('0123456789') on '123abc' captures '123'"
  (prog
    "        '123abc' SPAN('0123456789') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "123" ($$ (quote C))))
)

(deftest worm_span_0123_on_abc
  "SPAN('0123456789') on 'abc' fails"
  (prog
    "        'abc' SPAN('0123456789') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_break_xyz_on_abcxyz
  "BREAK('xyz') on 'abcxyz' captures 'abc'"
  (prog
    "        'abcxyz' BREAK('xyz') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_break_xyz_on_xyzabc
  "BREAK('xyz') on 'xyzabc' captures ''"
  (prog
    "        'xyzabc' BREAK('xyz') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_break_xyz_on_abcdef
  "BREAK('xyz') on 'abcdef' fails"
  (prog
    "        'abcdef' BREAK('xyz') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_break_xyz_on_empty
  "BREAK('xyz') on '' captures ''"
  (prog
    "        '' BREAK('xyz') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_break_a_on_bbbab
  "BREAK('a') on 'bbbab' captures 'bbb'"
  (prog
    "        'bbbab' BREAK('a') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "bbb" ($$ (quote C))))
)

(deftest worm_break__on_hello
  "BREAK('!') on 'hello!' captures 'hello'"
  (prog
    "        'hello!' BREAK('!') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "hello" ($$ (quote C))))
)

(deftest worm_break_0123_on_abc123
  "BREAK('0123456789') on 'abc123' captures 'abc'"
  (prog
    "        'abc123' BREAK('0123456789') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_pos_0_on_abc
  "POS(0) LEN(1) on 'abc' captures 'a'"
  (prog
    "        'abc' POS(0) LEN(1) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "a" ($$ (quote C))))
)

(deftest worm_pos_1_on_abc
  "POS(1) LEN(1) on 'abc' captures 'b'"
  (prog
    "        'abc' POS(1) LEN(1) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "b" ($$ (quote C))))
)

(deftest worm_pos_2_on_abc
  "POS(2) LEN(1) on 'abc' captures 'c'"
  (prog
    "        'abc' POS(2) LEN(1) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "c" ($$ (quote C))))
)

(deftest worm_pos_3_on_abc
  "POS(3) LEN(1) on 'abc' fails"
  (prog
    "        'abc' POS(3) LEN(1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_pos_0_on_empty
  "POS(0) LEN(1) on '' fails"
  (prog
    "        '' POS(0) LEN(1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_rpos_0_on_abc
  "LEN(3) RPOS(0) on 'abc'"
  (prog
    "        'abc' POS(0) LEN(3) RPOS(0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_rpos_1_on_abc
  "LEN(2) RPOS(1) on 'abc'"
  (prog
    "        'abc' POS(0) LEN(2) RPOS(1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_rpos_3_on_abc
  "LEN(0) RPOS(3) on 'abc'"
  (prog
    "        'abc' POS(0) LEN(0) RPOS(3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_rpos_4_on_abc
  "RPOS(4) on 'abc' fails"
  (prog
    "        'abc' POS(0) LEN(0) RPOS(4) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_tab_0_on_abcde
  "TAB(0) on 'abcde' captures ''"
  (prog
    "        'abcde' TAB(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_tab_1_on_abcde
  "TAB(1) on 'abcde' captures 'a'"
  (prog
    "        'abcde' TAB(1) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "a" ($$ (quote C))))
)

(deftest worm_tab_2_on_abcde
  "TAB(2) on 'abcde' captures 'ab'"
  (prog
    "        'abcde' TAB(2) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "ab" ($$ (quote C))))
)

(deftest worm_tab_3_on_abcde
  "TAB(3) on 'abcde' captures 'abc'"
  (prog
    "        'abcde' TAB(3) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_tab_5_on_abcde
  "TAB(5) on 'abcde' captures 'abcde'"
  (prog
    "        'abcde' TAB(5) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abcde" ($$ (quote C))))
)

(deftest worm_tab_6_on_abcde
  "TAB(6) on 'abcde' fails"
  (prog
    "        'abcde' TAB(6) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_tab_0_on_empty
  "TAB(0) on '' captures ''"
  (prog
    "        '' TAB(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_tab_1_on_empty
  "TAB(1) on '' fails"
  (prog
    "        '' TAB(1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_rtab_0_on_abcde
  "RTAB(0) on 'abcde' captures 'abcde'"
  (prog
    "        'abcde' RTAB(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abcde" ($$ (quote C))))
)

(deftest worm_rtab_1_on_abcde
  "RTAB(1) on 'abcde' captures 'abcd'"
  (prog
    "        'abcde' RTAB(1) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abcd" ($$ (quote C))))
)

(deftest worm_rtab_2_on_abcde
  "RTAB(2) on 'abcde' captures 'abc'"
  (prog
    "        'abcde' RTAB(2) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_rtab_5_on_abcde
  "RTAB(5) on 'abcde' captures ''"
  (prog
    "        'abcde' RTAB(5) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_rtab_6_on_abcde
  "RTAB(6) on 'abcde' fails"
  (prog
    "        'abcde' RTAB(6) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_rtab_0_on_empty
  "RTAB(0) on '' captures ''"
  (prog
    "        '' RTAB(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "" ($$ (quote C))))
)

(deftest worm_rem_after_ab_on_abcde
  "REM after 'ab' on 'abcde' captures 'cde'"
  (prog
    "        'abcde' 'ab' REM . C"
    "end"
  )
    (is (= "cde" ($$ (quote C))))
)

(deftest worm_rem_after_nothing_on_abcde
  "REM after '' on 'abcde' captures 'abcde'"
  (prog
    "        'abcde' '' REM . C"
    "end"
  )
    (is (= "abcde" ($$ (quote C))))
)

(deftest worm_rem_after_abcde_on_abcde
  "REM after 'abcde' on 'abcde' captures ''"
  (prog
    "        'abcde' 'abcde' REM . C"
    "end"
  )
    (is (= "" ($$ (quote C))))
)

(deftest worm_rem_after_hel_on_hello
  "REM after 'hel' on 'hello' captures 'lo'"
  (prog
    "        'hello' 'hel' REM . C"
    "end"
  )
    (is (= "lo" ($$ (quote C))))
)

(deftest worm_rem_after_x_on_x
  "REM after 'x' on 'x' captures ''"
  (prog
    "        'x' 'x' REM . C"
    "end"
  )
    (is (= "" ($$ (quote C))))
)

(deftest worm_litpat_ell_in_hello
  "'ell' in 'hello' => yes"
  (prog
    "        'hello' 'ell' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_litpat_hello_in_hello
  "'hello' in 'hello' => yes"
  (prog
    "        'hello' 'hello' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_litpat_xyz_in_hello
  "'xyz' in 'hello' => no"
  (prog
    "        'hello' 'xyz' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_litpat_empty_in_hello
  "'' in 'hello' => yes"
  (prog
    "        'hello' '' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_litpat_empty_in_empty
  "'' in '' => yes"
  (prog
    "        '' '' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_litpat_a_in_empty
  "'a' in '' => no"
  (prog
    "        '' 'a' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_litpat_abc_in_abcabc
  "'abc' in 'abcabc' => yes"
  (prog
    "        'abcabc' 'abc' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_litpat_HELL_in_HELLO
  "'HELL' in 'HELLO' => yes"
  (prog
    "        'HELLO' 'HELL' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_litpat_hell_in_HELLO
  "'hell' in 'HELLO' => no"
  (prog
    "        'HELLO' 'hell' :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_table_set_get
  "TABLE set and get single key"
  (prog
    "        T = TABLE()"
    "        T<'k'> = 'v'"
    "        R = T<'k'>"
    "end"
  )
    (is (= "v" ($$ (quote R))))
)

(deftest worm_table_integer_key
  "TABLE with integer key"
  (prog
    "        T = TABLE()"
    "        T<1> = 42"
    "        R = T<1>"
    "end"
  )
    (is (= 42 ($$ (quote R))))
)

(deftest worm_table_missing_key
  "TABLE missing key returns empty string"
  (prog
    "        T = TABLE()"
    "        R = T<'missing'>"
    "end"
  )
    (is (= "" ($$ (quote R))))
)

(deftest worm_table_overwrite
  "TABLE overwrite key"
  (prog
    "        T = TABLE()"
    "        T<'x'> = 'first'"
    "        T<'x'> = 'second'"
    "        R = T<'x'>"
    "end"
  )
    (is (= "second" ($$ (quote R))))
)

(deftest worm_table_multiple
  "TABLE multiple distinct keys"
  (prog
    "        T = TABLE()"
    "        T<'a'> = 1"
    "        T<'b'> = 2"
    "        T<'c'> = 3"
    "        R = T<'a'> T<'b'> T<'c'>"
    "end"
  )
    (is (= "123" ($$ (quote R))))
)

