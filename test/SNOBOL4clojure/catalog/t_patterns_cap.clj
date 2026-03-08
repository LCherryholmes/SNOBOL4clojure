(ns SNOBOL4clojure.catalog.t_patterns_cap
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_patterns_cap))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_capture_dot_cap_from_hello_wo
  "Capture with . : world on 'hello world'"
  (prog
    "        'hello world' world . C"
    "end"
  )
    (is (= "world" ($$ (quote C))))
)

(deftest worm_capture_dot_cap_from_abcde
  "Capture with . : LEN(3) on 'abcde'"
  (prog
    "        'abcde' LEN(3) . C"
    "end"
  )
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_capture_dot_cap_from_abcde_1
  "Capture with . : SPAN('abc') on 'abcde'"
  (prog
    "        'abcde' SPAN('abc') . C"
    "end"
  )
    (is (= "abc" ($$ (quote C))))
)

(deftest worm_capture_dot_cap_from_hello
  "Capture with . : ANY('aeiou') on 'hello'"
  (prog
    "        'hello' ANY('aeiou') . C"
    "end"
  )
    (is (= "e" ($$ (quote C))))
)

(deftest worm_capture_dot_cap_from_12345
  "Capture with . : SPAN('0123456789') on '12345'"
  (prog
    "        '12345' SPAN('0123456789') . C"
    "end"
  )
    (is (= "12345" ($$ (quote C))))
)

(deftest worm_replace_world_on_hello_wo
  "'hello world' 'world' = 'SNOBOL' => 'hello SNOBOL'"
  (prog
    "        S = 'hello world'"
    "        S 'world' = 'SNOBOL'"
    "end"
  )
    (is (= "hello SNOBOL" ($$ (quote S))))
)

(deftest worm_replace_a_on_aaa
  "'aaa' 'a' = 'b' => 'baa'"
  (prog
    "        S = 'aaa'"
    "        S 'a' = 'b'"
    "end"
  )
    (is (= "baa" ($$ (quote S))))
)

(deftest worm_replace_ANYaei_on_hello
  "'hello' ANY('aeiou') = 'X' => 'hXllo'"
  (prog
    "        S = 'hello'"
    "        S ANY('aeiou') = 'X'"
    "end"
  )
    (is (= "hXllo" ($$ (quote S))))
)

(deftest worm_replace_LEN1_on_abc
  "'abc' LEN(1) = 'X' => 'Xbc'"
  (prog
    "        S = 'abc'"
    "        S LEN(1) = 'X'"
    "end"
  )
    (is (= "Xbc" ($$ (quote S))))
)

(deftest worm_replace_xyz_on_hello
  "'hello' 'xyz' = 'Q' => 'hello'"
  (prog
    "        S = 'hello'"
    "        S 'xyz' = 'Q'"
    "end"
  )
    (is (= "hello" ($$ (quote S))))
)

