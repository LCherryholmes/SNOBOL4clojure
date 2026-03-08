(ns SNOBOL4clojure.catalog.t_algorithms
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_algorithms))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_edge_self_concat
  "S = S x — self-concatenation chain"
  (prog
    "        S = 'a'"
    "        S = S 'b'"
    "        S = S 'c'"
    "        S = S 'd'"
    "end"
  )
    (is (= "abcd" ($$ (quote S))))
)

(deftest worm_edge_self_arith
  "I = I + 1 repeated"
  (prog
    "        I = 0"
    "        I = I + 1"
    "        I = I + 1"
    "        I = I + 1"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_edge_overwrite_many
  "Multiple assignments to same var — last wins"
  (prog
    "        I = 1"
    "        I = 2"
    "        I = 3"
    "        I = 4"
    "        I = 5"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_edge_unassigned_is_empty
  "Unassigned variable reads as empty string"
  (prog
    "        R = UNASSIGNED_VAR"
    "end"
  )
    (is (= "" ($$ (quote R))))
)

(deftest worm_edge_unassigned_in_concat
  "Unassigned var in concat is empty"
  (prog
    "        R = 'hello' UNASSIGNED 'world'"
    "end"
  )
    (is (= "helloworld" ($$ (quote R))))
)

(deftest worm_edge_int_string_coerce
  "String "42" used in arithmetic coerces to 42"
  (prog
    "        I = '42' + 1"
    "end"
  )
    (is (= 43 ($$ (quote I))))
)

(deftest worm_edge_int_to_string_concat
  "Integer used in concat coerces to string"
  (prog
    "        S = 'x' 42 'y'"
    "end"
  )
    (is (= "x42y" ($$ (quote S))))
)

(deftest worm_edge_pattern_var_reuse
  "Pattern var used on multiple subjects"
  (prog
    "        P = ANY('aeiou')"
    "        'hello' P . C1"
    "        'world' P . C2"
    "        'xyz'   P . C3 :S(OK)F(MISS)"
    "OK      C3 = 'found'  :(END)"
    "MISS    C3 = 'none'"
    "END"
  )
    (is (= "e" ($$ (quote C1))))
    (is (= "o" ($$ (quote C2))))
    (is (= "none" ($$ (quote C3))))
)

(deftest worm_edge_multiple_captures
  "Multiple captures in one pattern"
  (prog
    "        'hello world' SPAN('abcdefghijklmnopqrstuvwxyz') . W1                      ' '                      SPAN('abcdefghijklmnopqrstuvwxyz') . W2"
    "end"
  )
    (is (= "hello" ($$ (quote W1))))
    (is (= "world" ($$ (quote W2))))
)

(deftest worm_edge_nested_calls
  "Nested function calls: REVERSE(TRIM(s))"
  (prog
    "        R = REVERSE(TRIM('hello   '))"
    "end"
  )
    (is (= "olleh" ($$ (quote R))))
)

(deftest worm_edge_size_of_concat
  "SIZE of concatenated string"
  (prog
    "        S = 'hello' ' ' 'world'"
    "        I = SIZE(S)"
    "end"
  )
    (is (= 11 ($$ (quote I))))
)

