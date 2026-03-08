(ns SNOBOL4clojure.catalog.t_loops
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_loops))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_loop_count_1
  "Count loop 1..1 concatenated = "1""
  (prog
    "        I = 1"
    "        S = ''"
    "LOOP    S = S I"
    "        I = I + 1"
    "        LE(I,1) :S(LOOP)"
    "end"
  )
    (is (= "1" ($$ (quote S))))
)

(deftest worm_loop_count_2
  "Count loop 1..2 concatenated = "12""
  (prog
    "        I = 1"
    "        S = ''"
    "LOOP    S = S I"
    "        I = I + 1"
    "        LE(I,2) :S(LOOP)"
    "end"
  )
    (is (= "12" ($$ (quote S))))
)

(deftest worm_loop_count_3
  "Count loop 1..3 concatenated = "123""
  (prog
    "        I = 1"
    "        S = ''"
    "LOOP    S = S I"
    "        I = I + 1"
    "        LE(I,3) :S(LOOP)"
    "end"
  )
    (is (= "123" ($$ (quote S))))
)

(deftest worm_loop_count_5
  "Count loop 1..5 concatenated = "12345""
  (prog
    "        I = 1"
    "        S = ''"
    "LOOP    S = S I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end"
  )
    (is (= "12345" ($$ (quote S))))
)

(deftest worm_loop_count_10
  "Count loop 1..10 concatenated = "12345678910""
  (prog
    "        I = 1"
    "        S = ''"
    "LOOP    S = S I"
    "        I = I + 1"
    "        LE(I,10) :S(LOOP)"
    "end"
  )
    (is (= "12345678910" ($$ (quote S))))
)

(deftest worm_loop_sum_1
  "Sum 1..1 = 1"
  (prog
    "        I = 1"
    "        SUM = 0"
    "LOOP    SUM = SUM + I"
    "        I = I + 1"
    "        LE(I,1) :S(LOOP)"
    "end"
  )
    (is (= 1 ($$ (quote SUM))))
)

(deftest worm_loop_sum_3
  "Sum 1..3 = 6"
  (prog
    "        I = 1"
    "        SUM = 0"
    "LOOP    SUM = SUM + I"
    "        I = I + 1"
    "        LE(I,3) :S(LOOP)"
    "end"
  )
    (is (= 6 ($$ (quote SUM))))
)

(deftest worm_loop_sum_5
  "Sum 1..5 = 15"
  (prog
    "        I = 1"
    "        SUM = 0"
    "LOOP    SUM = SUM + I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end"
  )
    (is (= 15 ($$ (quote SUM))))
)

