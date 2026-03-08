(ns SNOBOL4clojure.catalog.t_assign
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_assign))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_int_lit_0
  "I = 0"
  (prog
    "        I = 0"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_int_lit_1
  "I = 1"
  (prog
    "        I = 1"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_int_lit_2
  "I = 2"
  (prog
    "        I = 2"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_int_lit_3
  "I = 3"
  (prog
    "        I = 3"
    "end"
  )
    (is (= 3 ($$ (quote I))))
)

(deftest worm_int_lit_4
  "I = 4"
  (prog
    "        I = 4"
    "end"
  )
    (is (= 4 ($$ (quote I))))
)

(deftest worm_int_lit_5
  "I = 5"
  (prog
    "        I = 5"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_int_lit_6
  "I = 6"
  (prog
    "        I = 6"
    "end"
  )
    (is (= 6 ($$ (quote I))))
)

(deftest worm_int_lit_7
  "I = 7"
  (prog
    "        I = 7"
    "end"
  )
    (is (= 7 ($$ (quote I))))
)

(deftest worm_int_lit_8
  "I = 8"
  (prog
    "        I = 8"
    "end"
  )
    (is (= 8 ($$ (quote I))))
)

(deftest worm_int_lit_9
  "I = 9"
  (prog
    "        I = 9"
    "end"
  )
    (is (= 9 ($$ (quote I))))
)

(deftest worm_int_lit_10
  "I = 10"
  (prog
    "        I = 10"
    "end"
  )
    (is (= 10 ($$ (quote I))))
)

(deftest worm_int_lit_11
  "I = 11"
  (prog
    "        I = 11"
    "end"
  )
    (is (= 11 ($$ (quote I))))
)

(deftest worm_int_lit_12
  "I = 12"
  (prog
    "        I = 12"
    "end"
  )
    (is (= 12 ($$ (quote I))))
)

(deftest worm_int_lit_13
  "I = 13"
  (prog
    "        I = 13"
    "end"
  )
    (is (= 13 ($$ (quote I))))
)

(deftest worm_int_lit_14
  "I = 14"
  (prog
    "        I = 14"
    "end"
  )
    (is (= 14 ($$ (quote I))))
)

(deftest worm_int_lit_15
  "I = 15"
  (prog
    "        I = 15"
    "end"
  )
    (is (= 15 ($$ (quote I))))
)

(deftest worm_int_lit_16
  "I = 16"
  (prog
    "        I = 16"
    "end"
  )
    (is (= 16 ($$ (quote I))))
)

(deftest worm_int_lit_17
  "I = 17"
  (prog
    "        I = 17"
    "end"
  )
    (is (= 17 ($$ (quote I))))
)

(deftest worm_int_lit_18
  "I = 18"
  (prog
    "        I = 18"
    "end"
  )
    (is (= 18 ($$ (quote I))))
)

(deftest worm_int_lit_19
  "I = 19"
  (prog
    "        I = 19"
    "end"
  )
    (is (= 19 ($$ (quote I))))
)

(deftest worm_int_lit_20
  "I = 20"
  (prog
    "        I = 20"
    "end"
  )
    (is (= 20 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_10
  "I = -10"
  (prog
    "        I = -10"
    "end"
  )
    (is (= -10 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_9
  "I = -9"
  (prog
    "        I = -9"
    "end"
  )
    (is (= -9 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_8
  "I = -8"
  (prog
    "        I = -8"
    "end"
  )
    (is (= -8 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_7
  "I = -7"
  (prog
    "        I = -7"
    "end"
  )
    (is (= -7 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_6
  "I = -6"
  (prog
    "        I = -6"
    "end"
  )
    (is (= -6 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_5
  "I = -5"
  (prog
    "        I = -5"
    "end"
  )
    (is (= -5 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_4
  "I = -4"
  (prog
    "        I = -4"
    "end"
  )
    (is (= -4 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_3
  "I = -3"
  (prog
    "        I = -3"
    "end"
  )
    (is (= -3 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_2
  "I = -2"
  (prog
    "        I = -2"
    "end"
  )
    (is (= -2 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_1
  "I = -1"
  (prog
    "        I = -1"
    "end"
  )
    (is (= -1 ($$ (quote I))))
)

(deftest worm_int_lit_99
  "I = 99"
  (prog
    "        I = 99"
    "end"
  )
    (is (= 99 ($$ (quote I))))
)

(deftest worm_int_lit_100
  "I = 100"
  (prog
    "        I = 100"
    "end"
  )
    (is (= 100 ($$ (quote I))))
)

(deftest worm_int_lit_1000
  "I = 1000"
  (prog
    "        I = 1000"
    "end"
  )
    (is (= 1000 ($$ (quote I))))
)

(deftest wormneg_intneg_litneg_neg_100
  "I = -100"
  (prog
    "        I = -100"
    "end"
  )
    (is (= -100 ($$ (quote I))))
)

(deftest worm_str_lit_empty
  "S = ''"
  (prog
    "        S = ''"
    "end"
  )
    (is (= "" ($$ (quote S))))
)

(deftest worm_str_lit_space
  "S = ' '"
  (prog
    "        S = ' '"
    "end"
  )
    (is (= " " ($$ (quote S))))
)

(deftest worm_str_lit_hello
  "S = 'hello'"
  (prog
    "        S = 'hello'"
    "end"
  )
    (is (= "hello" ($$ (quote S))))
)

(deftest worm_str_lit_digits
  "S = '12345'"
  (prog
    "        S = '12345'"
    "end"
  )
    (is (= "12345" ($$ (quote S))))
)

(deftest worm_str_lit_mixed
  "S = 'aB3!'"
  (prog
    "        S = 'aB3!'"
    "end"
  )
    (is (= "aB3!" ($$ (quote S))))
)

(deftest worm_str_lit_long
  "S = 'abcdefghijklmnopqrstuvwxyz'"
  (prog
    "        S = 'abcdefghijklmnopqrstuvwxyz'"
    "end"
  )
    (is (= "abcdefghijklmnopqrstuvwxyz" ($$ (quote S))))
)

(deftest worm_str_lit_upper
  "S = 'HELLO'"
  (prog
    "        S = 'HELLO'"
    "end"
  )
    (is (= "HELLO" ($$ (quote S))))
)

(deftest worm_str_lit_punct
  "S = ',;:.'"
  (prog
    "        S = ',;:.'"
    "end"
  )
    (is (= ",;:." ($$ (quote S))))
)

(deftest worm_str_lit_singleq
  "S = ''"
  (prog
    "        S = ''"
    "end"
  )
    (is (= "" ($$ (quote S))))
)

(deftest worm_real_lit_0p0
  "A = 0.0"
  (prog
    "        A = 0.0"
    "end"
  )
    (is (= 0.0 ($$ (quote A))))
)

(deftest worm_real_lit_1p0
  "A = 1.0"
  (prog
    "        A = 1.0"
    "end"
  )
    (is (= 1.0 ($$ (quote A))))
)

(deftest worm_real_lit_neg_1p0
  "A = -1.0"
  (prog
    "        A = -1.0"
    "end"
  )
    (is (= -1.0 ($$ (quote A))))
)

(deftest worm_real_lit_3p14
  "A = 3.14"
  (prog
    "        A = 3.14"
    "end"
  )
    (is (= 3.14 ($$ (quote A))))
)

(deftest worm_real_lit_0p5
  "A = 0.5"
  (prog
    "        A = 0.5"
    "end"
  )
    (is (= 0.5 ($$ (quote A))))
)

(deftest worm_real_lit_100p0
  "A = 100.0"
  (prog
    "        A = 100.0"
    "end"
  )
    (is (= 100.0 ($$ (quote A))))
)

(deftest worm_real_lit_neg_3p14
  "A = -3.14"
  (prog
    "        A = -3.14"
    "end"
  )
    (is (= -3.14 ($$ (quote A))))
)

(deftest worm_real_lit_1p5
  "A = 1.5"
  (prog
    "        A = 1.5"
    "end"
  )
    (is (= 1.5 ($$ (quote A))))
)

(deftest worm_real_lit_2p5
  "A = 2.5"
  (prog
    "        A = 2.5"
    "end"
  )
    (is (= 2.5 ($$ (quote A))))
)

(deftest worm_real_lit_10p0
  "A = 10.0"
  (prog
    "        A = 10.0"
    "end"
  )
    (is (= 10.0 ($$ (quote A))))
)

