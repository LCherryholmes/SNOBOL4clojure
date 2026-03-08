(ns SNOBOL4clojure.test-sprint9-bal
  "BAL tests derived from Snobol4.Net TestSnobol4/Function/Pattern/Bal.cs"
  (:require [clojure.test         :refer :all]
            [SNOBOL4clojure.match :refer [SEARCH COLLECT!]]
            [SNOBOL4clojure.patterns :refer [BAL POS]]
            [SNOBOL4clojure.env   :refer [GLOBALS]]))

(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint9-bal)) (f)))

(defn- bstr [sub [s e]] (subs sub s e))

;; ── Basic BAL ─────────────────────────────────────────────────────────────────

(deftest bal-single-char
  (is (= [0 1] (SEARCH "A" BAL))))

(deftest bal-balanced-parens
  (is (= [0 3] (SEARCH "(A)" BAL))))

(deftest bal-nested-parens
  (is (= [0 7] (SEARCH "((A+B))" BAL))))

(deftest bal-unclosed-anchored
  ;; Anchored to pos 0: "(A" has no balanced match from pos 0
  (is (nil? (SEARCH "(A" (list 'SEQ (list 'POS# 0) BAL)))))

(deftest bal-unmatched-close-anchored
  (is (nil? (SEARCH ")A" (list 'SEQ (list 'POS# 0) BAL)))))

(deftest bal-first-match-canonical
  (is (= [0 13] (SEARCH "((A+(B*C))+D)" BAL))))

;; ── Multi-yield via COLLECT! — Snobol4.Net Bal_001/002 ───────────────────────

(deftest bal-collect-canonical
  (testing "BAL yields the exact Snobol4.Net sequence on ((A+(B*C))+D)"
    (let [sub      "((A+(B*C))+D)"
          expected ["((A+(B*C))+D)" "(A+(B*C))" "(A+(B*C))+" "(A+(B*C))+D"
                    "A" "A+" "A+(B*C)" "+" "+(B*C)" "(B*C)"
                    "B" "B*" "B*C" "*" "*C" "C" "+" "+D" "D"]
          got      (map (partial bstr sub) (COLLECT! sub BAL))]
      (is (= 19 (count got)))
      (is (= expected got)))))
