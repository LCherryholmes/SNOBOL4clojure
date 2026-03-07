(ns SNOBOL4clojure.test-patterns
  ;; Tests for pattern constructors in patterns.clj.
  ;; Verifies the representation they produce AND that MATCH drives them correctly.
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.patterns :refer :all]
            [SNOBOL4clojure.match    :refer [MATCH]]))

;; ── Constructor representation ────────────────────────────────────────────────
(deftest test-constructors-produce-lists
  (is (= '(ANY$    #{\a \e \i \o \u}) (ANY    "aeiou")))
  (is (= '(SPAN$   #{\0 \1 \2})       (SPAN   "012")))
  (is (= '(BREAK$  #{\. \,})          (BREAK  ".,")))
  (is (= '(NOTANY$ #{\x})             (NOTANY "x")))
  (is (= '(LEN#    5)                 (LEN    5)))
  (is (= '(POS#    0)                 (POS    0)))
  (is (= '(RPOS#   0)                 (RPOS   0)))
  (is (= '(TAB#    4)                 (TAB    4)))
  (is (= '(RTAB#   2)                 (RTAB   2)))
  (is (= '(ARBNO!  "x")              (ARBNO  "x")))
  (is (= '(FENCE!)                   (FENCE)))
  (is (= '(FENCE!  "x")             (FENCE  "x"))))

;; ── Constant pattern values ───────────────────────────────────────────────────
(deftest test-constant-patterns
  (is (= '(ARB!)     ARB))
  (is (= '(BAL!)     BAL))
  (is (= '(REM!)     REM))
  (is (= '(ABORT!)   ABORT))
  (is (= '(FAIL!)    FAIL))
  (is (= '(SUCCEED!) SUCCEED)))

;; ── ANY in MATCH ──────────────────────────────────────────────────────────────
(deftest test-any-match
  (let [A (ANY "BFLR")]
    (is      (MATCH (seq "BED")  0 A))
    (is      (MATCH (seq "FAD")  0 A))
    (is (not (MATCH (seq "XYZ")  0 (list 'SEQ (POS 0) A))))))

;; ── SPAN in MATCH ────────────────────────────────────────────────────────────
(deftest test-span-match
  (let [S (SPAN "0123456789")]
    (is      (MATCH (seq "123abc") 0 S))
    (is (not (MATCH (seq "abc")    0 (list 'SEQ (POS 0) S (RPOS 0)))))))

;; ── BREAK in MATCH ───────────────────────────────────────────────────────────
(deftest test-break-match
  (let [B (BREAK ".!?")]
    (is (MATCH (seq "hello.world") 0 B))
    ;; BREAK stops before the delimiter
    (is (MATCH (seq "hello.world") 0 (list 'SEQ B ".")))))

;; ── POS / RPOS anchoring ──────────────────────────────────────────────────────
(deftest test-pos-rpos-match
  (let [P (list 'SEQ (POS 0) "abc" (RPOS 0))]
    (is      (MATCH (seq "abc")  0 P))
    (is (not (MATCH (seq "xabc") 0 P)))
    (is (not (MATCH (seq "abcx") 0 P)))))

;; ── LEN ──────────────────────────────────────────────────────────────────────
(deftest test-len-match
  (is      (MATCH (seq "abcde") 0 (LEN 3)))
  (is (not (MATCH (seq "ab")    0 (list 'SEQ (POS 0) (LEN 3) (RPOS 0))))))

;; ── SUCCEED / FAIL ────────────────────────────────────────────────────────────
(deftest test-succeed-fail-patterns
  (is      (MATCH (seq "anything") 0 SUCCEED))
  (is (not (MATCH (seq "anything") 0 FAIL))))
