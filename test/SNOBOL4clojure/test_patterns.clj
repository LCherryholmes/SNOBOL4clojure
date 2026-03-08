(ns SNOBOL4clojure.test-patterns
  ;; Tests for pattern constructors in patterns.clj.
  ;; Verifies the representation they produce AND that MATCH drives them correctly.
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.patterns :refer :all]
            [SNOBOL4clojure.match-api :refer [SEARCH MATCH FULLMATCH]]))

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
    (is      (SEARCH "BED"  A))
    (is      (SEARCH "FAD"  A))
    (is (not (FULLMATCH "XYZ" A)))))

;; ── SPAN in MATCH ────────────────────────────────────────────────────────────
(deftest test-span-match
  (let [S (SPAN "0123456789")]
    (is      (SEARCH "123abc" S))
    (is (not (FULLMATCH "abc" S)))))

;; ── BREAK in MATCH ───────────────────────────────────────────────────────────
(deftest test-break-match
  (let [B (BREAK ".!?")]
    (is (SEARCH "hello.world" B))
    ;; BREAK stops before the delimiter
    (is (SEARCH "hello.world" (list 'SEQ B ".")))))

;; ── POS / RPOS anchoring ──────────────────────────────────────────────────────
(deftest test-pos-rpos-match
  (let [P (list 'SEQ (POS 0) "abc" (RPOS 0))]
    (is      (FULLMATCH "abc"  P))
    (is (not (FULLMATCH "xabc" P)))
    (is (not (FULLMATCH "abcx" P)))))

;; ── LEN ──────────────────────────────────────────────────────────────────────
(deftest test-len-match
  (is      (SEARCH "abcde" (LEN 3)))
  (is (not (FULLMATCH "ab" (LEN 3)))))

;; ── SUCCEED / FAIL ────────────────────────────────────────────────────────────
(deftest test-succeed-fail-patterns
  (is      (SEARCH "anything" SUCCEED))
  (is (not (SEARCH "anything" FAIL))))
