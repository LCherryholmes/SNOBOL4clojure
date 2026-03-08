(ns SNOBOL4clojure.catalog.t_array
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_array))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_array_set_get_1
  "ARRAY(1) set and get element 1"
  (prog
    "        A = ARRAY(1)"
    "        A<1> = 10"
    "        R = A<1>"
    "end"
  )
    (is (= 10 ($$ (quote R))))
)

(deftest worm_array_set_get_2
  "ARRAY(2) set and get element 2"
  (prog
    "        A = ARRAY(2)"
    "        A<2> = 20"
    "        R = A<2>"
    "end"
  )
    (is (= 20 ($$ (quote R))))
)

(deftest worm_array_set_get_3
  "ARRAY(3) set and get element 3"
  (prog
    "        A = ARRAY(3)"
    "        A<3> = 30"
    "        R = A<3>"
    "end"
  )
    (is (= 30 ($$ (quote R))))
)

(deftest worm_array_set_get_5
  "ARRAY(5) set and get element 5"
  (prog
    "        A = ARRAY(5)"
    "        A<5> = 50"
    "        R = A<5>"
    "end"
  )
    (is (= 50 ($$ (quote R))))
)

(deftest worm_array_set_get_10
  "ARRAY(10) set and get element 10"
  (prog
    "        A = ARRAY(10)"
    "        A<10> = 100"
    "        R = A<10>"
    "end"
  )
    (is (= 100 ($$ (quote R))))
)

(deftest worm_array_default_empty
  "ARRAY unset element is empty string"
  (prog
    "        A = ARRAY(5)"
    "        R = A<3>"
    "end"
  )
    (is (= "" ($$ (quote R))))
)

(deftest worm_array_loop_fill
  "Fill array in loop, read back"
  (prog
    "        A = ARRAY(5)"
    "        I = 1"
    "LOOP    A<I> = I * I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "        R = A<1> A<4> A<5>"
    "end"
  )
    (is (= "1165" ($$ (quote R))))
)

