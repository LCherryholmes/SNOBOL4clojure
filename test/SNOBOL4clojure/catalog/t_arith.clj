(ns SNOBOL4clojure.catalog.t_arith
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_arith))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_add_0_0
  "I = 0 + 0 => 0"
  (prog
    "        I = 0 + 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_add_0_1
  "I = 0 + 1 => 1"
  (prog
    "        I = 0 + 1"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_add_0_2
  "I = 0 + 2 => 2"
  (prog
    "        I = 0 + 2"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_add_0_3
  "I = 0 + 3 => 3"
  (prog
    "        I = 0 + 3"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_add_0_4
  "I = 0 + 4 => 4"
  (prog
    "        I = 0 + 4"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_add_0_5
  "I = 0 + 5 => 5"
  (prog
    "        I = 0 + 5"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_add_1_0
  "I = 1 + 0 => 1"
  (prog
    "        I = 1 + 0"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_add_1_1
  "I = 1 + 1 => 2"
  (prog
    "        I = 1 + 1"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_add_1_2
  "I = 1 + 2 => 3"
  (prog
    "        I = 1 + 2"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_add_1_3
  "I = 1 + 3 => 4"
  (prog
    "        I = 1 + 3"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_add_1_4
  "I = 1 + 4 => 5"
  (prog
    "        I = 1 + 4"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_add_1_5
  "I = 1 + 5 => 6"
  (prog
    "        I = 1 + 5"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_add_2_0
  "I = 2 + 0 => 2"
  (prog
    "        I = 2 + 0"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_add_2_1
  "I = 2 + 1 => 3"
  (prog
    "        I = 2 + 1"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_add_2_2
  "I = 2 + 2 => 4"
  (prog
    "        I = 2 + 2"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_add_2_3
  "I = 2 + 3 => 5"
  (prog
    "        I = 2 + 3"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_add_2_4
  "I = 2 + 4 => 6"
  (prog
    "        I = 2 + 4"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_add_2_5
  "I = 2 + 5 => 7"
  (prog
    "        I = 2 + 5"
    "end"
  )
    (is (= 7 ($$ (quote I))))
)

(deftest worm_add_3_0
  "I = 3 + 0 => 3"
  (prog
    "        I = 3 + 0"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_add_3_1
  "I = 3 + 1 => 4"
  (prog
    "        I = 3 + 1"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_add_3_2
  "I = 3 + 2 => 5"
  (prog
    "        I = 3 + 2"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_add_3_3
  "I = 3 + 3 => 6"
  (prog
    "        I = 3 + 3"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_add_3_4
  "I = 3 + 4 => 7"
  (prog
    "        I = 3 + 4"
    "end"
  )
    (is (= 7 ($$ (quote I))))
)

(deftest worm_add_3_5
  "I = 3 + 5 => 8"
  (prog
    "        I = 3 + 5"
    "end"
  )
    (is (= 8 ($$ (quote I))))
)

(deftest worm_add_4_0
  "I = 4 + 0 => 4"
  (prog
    "        I = 4 + 0"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_add_4_1
  "I = 4 + 1 => 5"
  (prog
    "        I = 4 + 1"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_add_4_2
  "I = 4 + 2 => 6"
  (prog
    "        I = 4 + 2"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_add_4_3
  "I = 4 + 3 => 7"
  (prog
    "        I = 4 + 3"
    "end"
  )
    (is (= 7 ($$ (quote I))))
)

(deftest worm_add_4_4
  "I = 4 + 4 => 8"
  (prog
    "        I = 4 + 4"
    "end"
  )
    (is (= 8 ($$ (quote I))))
)

(deftest worm_add_4_5
  "I = 4 + 5 => 9"
  (prog
    "        I = 4 + 5"
    "end"
  )
    (is (= 9 ($$ (quote I))))
)

(deftest worm_add_5_0
  "I = 5 + 0 => 5"
  (prog
    "        I = 5 + 0"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_add_5_1
  "I = 5 + 1 => 6"
  (prog
    "        I = 5 + 1"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_add_5_2
  "I = 5 + 2 => 7"
  (prog
    "        I = 5 + 2"
    "end"
  )
    (is (= 7 ($$ (quote I))))
)

(deftest worm_add_5_3
  "I = 5 + 3 => 8"
  (prog
    "        I = 5 + 3"
    "end"
  )
    (is (= 8 ($$ (quote I))))
)

(deftest worm_add_5_4
  "I = 5 + 4 => 9"
  (prog
    "        I = 5 + 4"
    "end"
  )
    (is (= 9 ($$ (quote I))))
)

(deftest worm_add_5_5
  "I = 5 + 5 => 10"
  (prog
    "        I = 5 + 5"
    "end"
  )
    (is (= 10 ($$ (quote I))))
)

(deftest worm_sub_0_0
  "I = 0 - 0 => 0"
  (prog
    "        I = 0 - 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_sub_0_1
  "I = 0 - 1 => -1"
  (prog
    "        I = 0 - 1"
    "end"
  )
    (is (= -1 ($$ (quote I))))
)

(deftest worm_sub_0_2
  "I = 0 - 2 => -2"
  (prog
    "        I = 0 - 2"
    "end"
  )
    (is (= -2 ($$ (quote I))))
)

(deftest worm_sub_0_3
  "I = 0 - 3 => -3"
  (prog
    "        I = 0 - 3"
    "end"
  )
    (is (= -3 ($$ (quote I))))
)

(deftest worm_sub_0_4
  "I = 0 - 4 => -4"
  (prog
    "        I = 0 - 4"
    "end"
  )
    (is (= -4 ($$ (quote I))))
)

(deftest worm_sub_0_5
  "I = 0 - 5 => -5"
  (prog
    "        I = 0 - 5"
    "end"
  )
    (is (= -5 ($$ (quote I))))
)

(deftest worm_sub_1_0
  "I = 1 - 0 => 1"
  (prog
    "        I = 1 - 0"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_sub_1_1
  "I = 1 - 1 => 0"
  (prog
    "        I = 1 - 1"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_sub_1_2
  "I = 1 - 2 => -1"
  (prog
    "        I = 1 - 2"
    "end"
  )
    (is (= -1 ($$ (quote I))))
)

(deftest worm_sub_1_3
  "I = 1 - 3 => -2"
  (prog
    "        I = 1 - 3"
    "end"
  )
    (is (= -2 ($$ (quote I))))
)

(deftest worm_sub_1_4
  "I = 1 - 4 => -3"
  (prog
    "        I = 1 - 4"
    "end"
  )
    (is (= -3 ($$ (quote I))))
)

(deftest worm_sub_1_5
  "I = 1 - 5 => -4"
  (prog
    "        I = 1 - 5"
    "end"
  )
    (is (= -4 ($$ (quote I))))
)

(deftest worm_sub_2_0
  "I = 2 - 0 => 2"
  (prog
    "        I = 2 - 0"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_sub_2_1
  "I = 2 - 1 => 1"
  (prog
    "        I = 2 - 1"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_sub_2_2
  "I = 2 - 2 => 0"
  (prog
    "        I = 2 - 2"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_sub_2_3
  "I = 2 - 3 => -1"
  (prog
    "        I = 2 - 3"
    "end"
  )
    (is (= -1 ($$ (quote I))))
)

(deftest worm_sub_2_4
  "I = 2 - 4 => -2"
  (prog
    "        I = 2 - 4"
    "end"
  )
    (is (= -2 ($$ (quote I))))
)

(deftest worm_sub_2_5
  "I = 2 - 5 => -3"
  (prog
    "        I = 2 - 5"
    "end"
  )
    (is (= -3 ($$ (quote I))))
)

(deftest worm_sub_3_0
  "I = 3 - 0 => 3"
  (prog
    "        I = 3 - 0"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_sub_3_1
  "I = 3 - 1 => 2"
  (prog
    "        I = 3 - 1"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_sub_3_2
  "I = 3 - 2 => 1"
  (prog
    "        I = 3 - 2"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_sub_3_3
  "I = 3 - 3 => 0"
  (prog
    "        I = 3 - 3"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_sub_3_4
  "I = 3 - 4 => -1"
  (prog
    "        I = 3 - 4"
    "end"
  )
    (is (= -1 ($$ (quote I))))
)

(deftest worm_sub_3_5
  "I = 3 - 5 => -2"
  (prog
    "        I = 3 - 5"
    "end"
  )
    (is (= -2 ($$ (quote I))))
)

(deftest worm_sub_4_0
  "I = 4 - 0 => 4"
  (prog
    "        I = 4 - 0"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_sub_4_1
  "I = 4 - 1 => 3"
  (prog
    "        I = 4 - 1"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_sub_4_2
  "I = 4 - 2 => 2"
  (prog
    "        I = 4 - 2"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_sub_4_3
  "I = 4 - 3 => 1"
  (prog
    "        I = 4 - 3"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_sub_4_4
  "I = 4 - 4 => 0"
  (prog
    "        I = 4 - 4"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_sub_4_5
  "I = 4 - 5 => -1"
  (prog
    "        I = 4 - 5"
    "end"
  )
    (is (= -1 ($$ (quote I))))
)

(deftest worm_sub_5_0
  "I = 5 - 0 => 5"
  (prog
    "        I = 5 - 0"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_sub_5_1
  "I = 5 - 1 => 4"
  (prog
    "        I = 5 - 1"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_sub_5_2
  "I = 5 - 2 => 3"
  (prog
    "        I = 5 - 2"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_sub_5_3
  "I = 5 - 3 => 2"
  (prog
    "        I = 5 - 3"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_sub_5_4
  "I = 5 - 4 => 1"
  (prog
    "        I = 5 - 4"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_sub_5_5
  "I = 5 - 5 => 0"
  (prog
    "        I = 5 - 5"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_0_0
  "I = 0 * 0 => 0"
  (prog
    "        I = 0 * 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_0_1
  "I = 0 * 1 => 0"
  (prog
    "        I = 0 * 1"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_0_2
  "I = 0 * 2 => 0"
  (prog
    "        I = 0 * 2"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_0_3
  "I = 0 * 3 => 0"
  (prog
    "        I = 0 * 3"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_0_4
  "I = 0 * 4 => 0"
  (prog
    "        I = 0 * 4"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_0_5
  "I = 0 * 5 => 0"
  (prog
    "        I = 0 * 5"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_1_0
  "I = 1 * 0 => 0"
  (prog
    "        I = 1 * 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_1_1
  "I = 1 * 1 => 1"
  (prog
    "        I = 1 * 1"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_mul_1_2
  "I = 1 * 2 => 2"
  (prog
    "        I = 1 * 2"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_mul_1_3
  "I = 1 * 3 => 3"
  (prog
    "        I = 1 * 3"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_mul_1_4
  "I = 1 * 4 => 4"
  (prog
    "        I = 1 * 4"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_mul_1_5
  "I = 1 * 5 => 5"
  (prog
    "        I = 1 * 5"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_mul_2_0
  "I = 2 * 0 => 0"
  (prog
    "        I = 2 * 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_2_1
  "I = 2 * 1 => 2"
  (prog
    "        I = 2 * 1"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_mul_2_2
  "I = 2 * 2 => 4"
  (prog
    "        I = 2 * 2"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_mul_2_3
  "I = 2 * 3 => 6"
  (prog
    "        I = 2 * 3"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_mul_2_4
  "I = 2 * 4 => 8"
  (prog
    "        I = 2 * 4"
    "end"
  )
    (is (= 8 ($$ (quote I))))
)

(deftest worm_mul_2_5
  "I = 2 * 5 => 10"
  (prog
    "        I = 2 * 5"
    "end"
  )
    (is (= 10 ($$ (quote I))))
)

(deftest worm_mul_3_0
  "I = 3 * 0 => 0"
  (prog
    "        I = 3 * 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_3_1
  "I = 3 * 1 => 3"
  (prog
    "        I = 3 * 1"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_mul_3_2
  "I = 3 * 2 => 6"
  (prog
    "        I = 3 * 2"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_mul_3_3
  "I = 3 * 3 => 9"
  (prog
    "        I = 3 * 3"
    "end"
  )
    (is (= 9 ($$ (quote I))))
)

(deftest worm_mul_3_4
  "I = 3 * 4 => 12"
  (prog
    "        I = 3 * 4"
    "end"
  )
    (is (= 12 ($$ (quote I))))
)

(deftest worm_mul_3_5
  "I = 3 * 5 => 15"
  (prog
    "        I = 3 * 5"
    "end"
  )
    (is (= 15 ($$ (quote I))))
)

(deftest worm_mul_4_0
  "I = 4 * 0 => 0"
  (prog
    "        I = 4 * 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_4_1
  "I = 4 * 1 => 4"
  (prog
    "        I = 4 * 1"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_mul_4_2
  "I = 4 * 2 => 8"
  (prog
    "        I = 4 * 2"
    "end"
  )
    (is (= 8 ($$ (quote I))))
)

(deftest worm_mul_4_3
  "I = 4 * 3 => 12"
  (prog
    "        I = 4 * 3"
    "end"
  )
    (is (= 12 ($$ (quote I))))
)

(deftest worm_mul_4_4
  "I = 4 * 4 => 16"
  (prog
    "        I = 4 * 4"
    "end"
  )
    (is (= 16 ($$ (quote I))))
)

(deftest worm_mul_4_5
  "I = 4 * 5 => 20"
  (prog
    "        I = 4 * 5"
    "end"
  )
    (is (= 20 ($$ (quote I))))
)

(deftest worm_mul_5_0
  "I = 5 * 0 => 0"
  (prog
    "        I = 5 * 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_mul_5_1
  "I = 5 * 1 => 5"
  (prog
    "        I = 5 * 1"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_mul_5_2
  "I = 5 * 2 => 10"
  (prog
    "        I = 5 * 2"
    "end"
  )
    (is (= 10 ($$ (quote I))))
)

(deftest worm_mul_5_3
  "I = 5 * 3 => 15"
  (prog
    "        I = 5 * 3"
    "end"
  )
    (is (= 15 ($$ (quote I))))
)

(deftest worm_mul_5_4
  "I = 5 * 4 => 20"
  (prog
    "        I = 5 * 4"
    "end"
  )
    (is (= 20 ($$ (quote I))))
)

(deftest worm_mul_5_5
  "I = 5 * 5 => 25"
  (prog
    "        I = 5 * 5"
    "end"
  )
    (is (= 25 ($$ (quote I))))
)

(deftest worm_div_0_1
  "I = 0 / 1 => 0 (integer division)"
  (prog
    "        I = 0 / 1"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_0_2
  "I = 0 / 2 => 0 (integer division)"
  (prog
    "        I = 0 / 2"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_0_3
  "I = 0 / 3 => 0 (integer division)"
  (prog
    "        I = 0 / 3"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_0_4
  "I = 0 / 4 => 0 (integer division)"
  (prog
    "        I = 0 / 4"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_0_5
  "I = 0 / 5 => 0 (integer division)"
  (prog
    "        I = 0 / 5"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_1_1
  "I = 1 / 1 => 1 (integer division)"
  (prog
    "        I = 1 / 1"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_1_2
  "I = 1 / 2 => 0 (integer division)"
  (prog
    "        I = 1 / 2"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_1_3
  "I = 1 / 3 => 0 (integer division)"
  (prog
    "        I = 1 / 3"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_1_4
  "I = 1 / 4 => 0 (integer division)"
  (prog
    "        I = 1 / 4"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_1_5
  "I = 1 / 5 => 0 (integer division)"
  (prog
    "        I = 1 / 5"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_2_1
  "I = 2 / 1 => 2 (integer division)"
  (prog
    "        I = 2 / 1"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_div_2_2
  "I = 2 / 2 => 1 (integer division)"
  (prog
    "        I = 2 / 2"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_2_3
  "I = 2 / 3 => 0 (integer division)"
  (prog
    "        I = 2 / 3"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_2_4
  "I = 2 / 4 => 0 (integer division)"
  (prog
    "        I = 2 / 4"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_2_5
  "I = 2 / 5 => 0 (integer division)"
  (prog
    "        I = 2 / 5"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_3_1
  "I = 3 / 1 => 3 (integer division)"
  (prog
    "        I = 3 / 1"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_div_3_2
  "I = 3 / 2 => 1 (integer division)"
  (prog
    "        I = 3 / 2"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_3_3
  "I = 3 / 3 => 1 (integer division)"
  (prog
    "        I = 3 / 3"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_3_4
  "I = 3 / 4 => 0 (integer division)"
  (prog
    "        I = 3 / 4"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_3_5
  "I = 3 / 5 => 0 (integer division)"
  (prog
    "        I = 3 / 5"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_4_1
  "I = 4 / 1 => 4 (integer division)"
  (prog
    "        I = 4 / 1"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_div_4_2
  "I = 4 / 2 => 2 (integer division)"
  (prog
    "        I = 4 / 2"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_div_4_3
  "I = 4 / 3 => 1 (integer division)"
  (prog
    "        I = 4 / 3"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_4_4
  "I = 4 / 4 => 1 (integer division)"
  (prog
    "        I = 4 / 4"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_4_5
  "I = 4 / 5 => 0 (integer division)"
  (prog
    "        I = 4 / 5"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_div_5_1
  "I = 5 / 1 => 5 (integer division)"
  (prog
    "        I = 5 / 1"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_div_5_2
  "I = 5 / 2 => 2 (integer division)"
  (prog
    "        I = 5 / 2"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_div_5_3
  "I = 5 / 3 => 1 (integer division)"
  (prog
    "        I = 5 / 3"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_5_4
  "I = 5 / 4 => 1 (integer division)"
  (prog
    "        I = 5 / 4"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_5_5
  "I = 5 / 5 => 1 (integer division)"
  (prog
    "        I = 5 / 5"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_6_1
  "I = 6 / 1 => 6 (integer division)"
  (prog
    "        I = 6 / 1"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_div_6_2
  "I = 6 / 2 => 3 (integer division)"
  (prog
    "        I = 6 / 2"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_div_6_3
  "I = 6 / 3 => 2 (integer division)"
  (prog
    "        I = 6 / 3"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_div_6_4
  "I = 6 / 4 => 1 (integer division)"
  (prog
    "        I = 6 / 4"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_6_5
  "I = 6 / 5 => 1 (integer division)"
  (prog
    "        I = 6 / 5"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_7_1
  "I = 7 / 1 => 7 (integer division)"
  (prog
    "        I = 7 / 1"
    "end"
  )
    (is (= 7 ($$ (quote I))))
)

(deftest worm_div_7_2
  "I = 7 / 2 => 3 (integer division)"
  (prog
    "        I = 7 / 2"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_div_7_3
  "I = 7 / 3 => 2 (integer division)"
  (prog
    "        I = 7 / 3"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_div_7_4
  "I = 7 / 4 => 1 (integer division)"
  (prog
    "        I = 7 / 4"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_div_7_5
  "I = 7 / 5 => 1 (integer division)"
  (prog
    "        I = 7 / 5"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_0_1
  "REMDR(0,1) => 0"
  (prog
    "        I = REMDR(0,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_0_2
  "REMDR(0,2) => 0"
  (prog
    "        I = REMDR(0,2)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_0_3
  "REMDR(0,3) => 0"
  (prog
    "        I = REMDR(0,3)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_0_4
  "REMDR(0,4) => 0"
  (prog
    "        I = REMDR(0,4)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_0_5
  "REMDR(0,5) => 0"
  (prog
    "        I = REMDR(0,5)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_1_1
  "REMDR(1,1) => 0"
  (prog
    "        I = REMDR(1,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_1_2
  "REMDR(1,2) => 1"
  (prog
    "        I = REMDR(1,2)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_1_3
  "REMDR(1,3) => 1"
  (prog
    "        I = REMDR(1,3)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_1_4
  "REMDR(1,4) => 1"
  (prog
    "        I = REMDR(1,4)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_1_5
  "REMDR(1,5) => 1"
  (prog
    "        I = REMDR(1,5)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_2_1
  "REMDR(2,1) => 0"
  (prog
    "        I = REMDR(2,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_2_2
  "REMDR(2,2) => 0"
  (prog
    "        I = REMDR(2,2)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_2_3
  "REMDR(2,3) => 2"
  (prog
    "        I = REMDR(2,3)"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_remdr_2_4
  "REMDR(2,4) => 2"
  (prog
    "        I = REMDR(2,4)"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_remdr_2_5
  "REMDR(2,5) => 2"
  (prog
    "        I = REMDR(2,5)"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_remdr_3_1
  "REMDR(3,1) => 0"
  (prog
    "        I = REMDR(3,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_3_2
  "REMDR(3,2) => 1"
  (prog
    "        I = REMDR(3,2)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_3_3
  "REMDR(3,3) => 0"
  (prog
    "        I = REMDR(3,3)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_3_4
  "REMDR(3,4) => 3"
  (prog
    "        I = REMDR(3,4)"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_remdr_3_5
  "REMDR(3,5) => 3"
  (prog
    "        I = REMDR(3,5)"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_remdr_4_1
  "REMDR(4,1) => 0"
  (prog
    "        I = REMDR(4,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_4_2
  "REMDR(4,2) => 0"
  (prog
    "        I = REMDR(4,2)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_4_3
  "REMDR(4,3) => 1"
  (prog
    "        I = REMDR(4,3)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_4_4
  "REMDR(4,4) => 0"
  (prog
    "        I = REMDR(4,4)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_4_5
  "REMDR(4,5) => 4"
  (prog
    "        I = REMDR(4,5)"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_remdr_5_1
  "REMDR(5,1) => 0"
  (prog
    "        I = REMDR(5,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_5_2
  "REMDR(5,2) => 1"
  (prog
    "        I = REMDR(5,2)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_5_3
  "REMDR(5,3) => 2"
  (prog
    "        I = REMDR(5,3)"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_remdr_5_4
  "REMDR(5,4) => 1"
  (prog
    "        I = REMDR(5,4)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_5_5
  "REMDR(5,5) => 0"
  (prog
    "        I = REMDR(5,5)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_6_1
  "REMDR(6,1) => 0"
  (prog
    "        I = REMDR(6,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_6_2
  "REMDR(6,2) => 0"
  (prog
    "        I = REMDR(6,2)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_6_3
  "REMDR(6,3) => 0"
  (prog
    "        I = REMDR(6,3)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_6_4
  "REMDR(6,4) => 2"
  (prog
    "        I = REMDR(6,4)"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_remdr_6_5
  "REMDR(6,5) => 1"
  (prog
    "        I = REMDR(6,5)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_7_1
  "REMDR(7,1) => 0"
  (prog
    "        I = REMDR(7,1)"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_remdr_7_2
  "REMDR(7,2) => 1"
  (prog
    "        I = REMDR(7,2)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_7_3
  "REMDR(7,3) => 1"
  (prog
    "        I = REMDR(7,3)"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_remdr_7_4
  "REMDR(7,4) => 3"
  (prog
    "        I = REMDR(7,4)"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_remdr_7_5
  "REMDR(7,5) => 2"
  (prog
    "        I = REMDR(7,5)"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_ltr_2p3m4
  "I = 2 + 3 * 4 => 20 (left-to-right, no precedence)"
  (prog
    "        I = 2 + 3 * 4"
    "end"
  )
    (is (= 20 ($$ (quote I))))
)

(deftest worm_ltr_10m2p3
  "I = 10 - 2 + 3 => 11 (left-to-right, no precedence)"
  (prog
    "        I = 10 - 2 + 3"
    "end"
  )
    (is (= 11 ($$ (quote I))))
)

(deftest worm_ltr_2m3p4
  "I = 2 * 3 + 4 => 10 (left-to-right, no precedence)"
  (prog
    "        I = 2 * 3 + 4"
    "end"
  )
    (is (= 10 ($$ (quote I))))
)

(deftest worm_ltr_6d2p1
  "I = 6 / 2 + 1 => 4 (left-to-right, no precedence)"
  (prog
    "        I = 6 / 2 + 1"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_ltr_1p2p3
  "I = 1 + 2 + 3 => 6 (left-to-right, no precedence)"
  (prog
    "        I = 1 + 2 + 3"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_ltr_5m1m1
  "I = 5 - 1 - 1 => 3 (left-to-right, no precedence)"
  (prog
    "        I = 5 - 1 - 1"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

