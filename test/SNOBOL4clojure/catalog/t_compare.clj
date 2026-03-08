(ns SNOBOL4clojure.catalog.t_compare
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_compare))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_cmp_EQ_0_0
  "EQ(0,0) => yes"
  (prog
    "        EQ(0,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_EQ_1_1
  "EQ(1,1) => yes"
  (prog
    "        EQ(1,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_EQ_0_1
  "EQ(0,1) => no"
  (prog
    "        EQ(0,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_EQ_1_0
  "EQ(1,0) => no"
  (prog
    "        EQ(1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_EQ_5_3
  "EQ(5,3) => no"
  (prog
    "        EQ(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_EQ_3_5
  "EQ(3,5) => no"
  (prog
    "        EQ(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_EQ_3_3
  "EQ(3,3) => yes"
  (prog
    "        EQ(3,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_EQ_neg_1_0
  "EQ(-1,0) => no"
  (prog
    "        EQ(-1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_EQ_0_neg_1
  "EQ(0,-1) => no"
  (prog
    "        EQ(0,-1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_NE_0_0
  "NE(0,0) => no"
  (prog
    "        NE(0,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_NE_1_1
  "NE(1,1) => no"
  (prog
    "        NE(1,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_NE_0_1
  "NE(0,1) => yes"
  (prog
    "        NE(0,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_NE_1_0
  "NE(1,0) => yes"
  (prog
    "        NE(1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_NE_5_3
  "NE(5,3) => yes"
  (prog
    "        NE(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_NE_3_5
  "NE(3,5) => yes"
  (prog
    "        NE(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_NE_3_3
  "NE(3,3) => no"
  (prog
    "        NE(3,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_NE_neg_1_0
  "NE(-1,0) => yes"
  (prog
    "        NE(-1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_NE_0_neg_1
  "NE(0,-1) => yes"
  (prog
    "        NE(0,-1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LT_0_0
  "LT(0,0) => no"
  (prog
    "        LT(0,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_LT_1_1
  "LT(1,1) => no"
  (prog
    "        LT(1,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_LT_0_1
  "LT(0,1) => yes"
  (prog
    "        LT(0,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LT_1_0
  "LT(1,0) => no"
  (prog
    "        LT(1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_LT_5_3
  "LT(5,3) => no"
  (prog
    "        LT(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_LT_3_5
  "LT(3,5) => yes"
  (prog
    "        LT(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LT_3_3
  "LT(3,3) => no"
  (prog
    "        LT(3,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_LT_neg_1_0
  "LT(-1,0) => yes"
  (prog
    "        LT(-1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LT_0_neg_1
  "LT(0,-1) => no"
  (prog
    "        LT(0,-1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GT_0_0
  "GT(0,0) => no"
  (prog
    "        GT(0,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GT_1_1
  "GT(1,1) => no"
  (prog
    "        GT(1,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GT_0_1
  "GT(0,1) => no"
  (prog
    "        GT(0,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GT_1_0
  "GT(1,0) => yes"
  (prog
    "        GT(1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_GT_5_3
  "GT(5,3) => yes"
  (prog
    "        GT(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_GT_3_5
  "GT(3,5) => no"
  (prog
    "        GT(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GT_3_3
  "GT(3,3) => no"
  (prog
    "        GT(3,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GT_neg_1_0
  "GT(-1,0) => no"
  (prog
    "        GT(-1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GT_0_neg_1
  "GT(0,-1) => yes"
  (prog
    "        GT(0,-1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LE_0_0
  "LE(0,0) => yes"
  (prog
    "        LE(0,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LE_1_1
  "LE(1,1) => yes"
  (prog
    "        LE(1,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LE_0_1
  "LE(0,1) => yes"
  (prog
    "        LE(0,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LE_1_0
  "LE(1,0) => no"
  (prog
    "        LE(1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_LE_5_3
  "LE(5,3) => no"
  (prog
    "        LE(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_LE_3_5
  "LE(3,5) => yes"
  (prog
    "        LE(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LE_3_3
  "LE(3,3) => yes"
  (prog
    "        LE(3,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LE_neg_1_0
  "LE(-1,0) => yes"
  (prog
    "        LE(-1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_LE_0_neg_1
  "LE(0,-1) => no"
  (prog
    "        LE(0,-1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GE_0_0
  "GE(0,0) => yes"
  (prog
    "        GE(0,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_GE_1_1
  "GE(1,1) => yes"
  (prog
    "        GE(1,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_GE_0_1
  "GE(0,1) => no"
  (prog
    "        GE(0,1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GE_1_0
  "GE(1,0) => yes"
  (prog
    "        GE(1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_GE_5_3
  "GE(5,3) => yes"
  (prog
    "        GE(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_GE_3_5
  "GE(3,5) => no"
  (prog
    "        GE(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GE_3_3
  "GE(3,3) => yes"
  (prog
    "        GE(3,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_cmp_GE_neg_1_0
  "GE(-1,0) => no"
  (prog
    "        GE(-1,0) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_cmp_GE_0_neg_1
  "GE(0,-1) => yes"
  (prog
    "        GE(0,-1) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_strcmp_IDENT_abc_abc
  "IDENT('abc','abc') => yes"
  (prog
    "        IDENT('abc','abc') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_strcmp_IDENT_abc_xyz
  "IDENT('abc','xyz') => no"
  (prog
    "        IDENT('abc','xyz') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_strcmp_IDENT_empty_empty
  "IDENT('','') => yes"
  (prog
    "        IDENT('','') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_strcmp_IDENT_a_empty
  "IDENT('a','') => no"
  (prog
    "        IDENT('a','') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_strcmp_DIFFER_abc_xyz
  "DIFFER('abc','xyz') => yes"
  (prog
    "        DIFFER('abc','xyz') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_strcmp_DIFFER_abc_abc
  "DIFFER('abc','abc') => no"
  (prog
    "        DIFFER('abc','abc') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_strcmp_DIFFER_empty_a
  "DIFFER('','a') => yes"
  (prog
    "        DIFFER('','a') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_strcmp_LGT_b_a
  "LGT('b','a') => yes"
  (prog
    "        LGT('b','a') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_strcmp_LGT_a_b
  "LGT('a','b') => no"
  (prog
    "        LGT('a','b') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_strcmp_LGT_abc_ab
  "LGT('abc','ab') => yes"
  (prog
    "        LGT('abc','ab') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_strcmp_LGT_a_a
  "LGT('a','a') => no"
  (prog
    "        LGT('a','a') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_strcmp_LGT_B_A
  "LGT('B','A') => yes"
  (prog
    "        LGT('B','A') :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_neg_EQ_5_5
  "~EQ(5,5) => no"
  (prog
    "        ~EQ(5,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_neg_EQ_5_6
  "~EQ(5,6) => yes"
  (prog
    "        ~EQ(5,6) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_neg_LT_3_5
  "~LT(3,5) => no"
  (prog
    "        ~LT(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_neg_LT_5_3
  "~LT(5,3) => yes"
  (prog
    "        ~LT(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_neg_GT_5_3
  "~GT(5,3) => no"
  (prog
    "        ~GT(5,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

(deftest worm_neg_GT_3_5
  "~GT(3,5) => yes"
  (prog
    "        ~GT(3,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

