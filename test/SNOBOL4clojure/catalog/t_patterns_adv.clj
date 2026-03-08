(ns SNOBOL4clojure.catalog.t_patterns_adv
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_patterns_adv))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_alt_hello
  "'hello' ('he' | 'wo') captures 'he'"
  (prog
    "        'hello' ('he' | 'wo') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "he" ($$ (quote C))))
)

(deftest worm_alt_world
  "'world' ('he' | 'wo') captures 'wo'"
  (prog
    "        'world' ('he' | 'wo') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "wo" ($$ (quote C))))
)

(deftest worm_alt_xyz
  "'xyz' ('he' | 'wo') fails"
  (prog
    "        'xyz' ('he' | 'wo') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_alt_abc
  "'abc' ('a' | 'b' | 'c') captures 'a'"
  (prog
    "        'abc' ('a' | 'b' | 'c') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "a" ($$ (quote C))))
)

(deftest worm_alt_bcd
  "'bcd' ('a' | 'b' | 'c') captures 'b'"
  (prog
    "        'bcd' ('a' | 'b' | 'c') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "b" ($$ (quote C))))
)

(deftest worm_alt_cde
  "'cde' ('a' | 'b' | 'c') captures 'c'"
  (prog
    "        'cde' ('a' | 'b' | 'c') . C :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
    (is (= "c" ($$ (quote C))))
)

(deftest worm_alt_xyz_1
  "'xyz' ('a' | 'b' | 'c') fails"
  (prog
    "        'xyz' ('a' | 'b' | 'c') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_arb_h_arb_o_in_hello
  "'h' ARB . C 'o' in 'hello' captures 'ell'"
  (prog
    "        'hello' 'h' ARB . C 'o'"
    "end"
  )
    (is (= "ell" ($$ (quote C))))
)

(deftest worm_arb_a_arb_e_in_abcde
  "'a' ARB . C 'e' in 'abcde' captures 'bcd'"
  (prog
    "        'abcde' 'a' ARB . C 'e'"
    "end"
  )
    (is (= "bcd" ($$ (quote C))))
)

(deftest worm_arb_a_arb_a_in_abba
  "'a' ARB . C 'a' in 'abba' captures ''"
  (prog
    "        'abba' 'a' ARB . C 'a'"
    "end"
  )
    (is (= "" ($$ (quote C))))
)

(deftest worm_arb_he_arb_o_in_hello
  "'he' ARB . C 'o' in 'hello' captures 'll'"
  (prog
    "        'hello' 'he' ARB . C 'o'"
    "end"
  )
    (is (= "ll" ($$ (quote C))))
)

