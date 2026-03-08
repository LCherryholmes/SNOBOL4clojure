(ns SNOBOL4clojure.test-arb
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.match-api :refer [SEARCH FULLMATCH] :as m]
            [SNOBOL4clojure.patterns :refer [POS RPOS SPAN ARB ARBNO]]
            [SNOBOL4clojure.env      :refer [GLOBALS]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [t] (GLOBALS (find-ns 'SNOBOL4clojure.test-arb)) (t)))

;; ── ARB ──────────────────────────────────────────────────────────────────────

(deftest arb-basic
  ;; ARB matches any string, shortest first
  (let [arb ARB]
    ;; SEARCH finds first (shortest) match at pos 0
    (is (= [0 0] (SEARCH "abc" arb)))
    ;; FULLMATCH with ARB alone matches entire string
    (is (= [0 3] (FULLMATCH "abc" arb)))
    (is (= [0 0] (FULLMATCH "" arb)))))

(deftest arb-anchored
  ;; POS(0) ARB RPOS(0) = fullmatch any string
  (let [p (list 'SEQ (POS 0) ARB (RPOS 0))]
    (is (= [0 3] (SEARCH "abc" p)))
    (is (= [0 0] (SEARCH "" p)))))

(deftest arb-in-seq-backtrack
  ;; SEQ[ARB, "b"] — ARB starts at 0 chars, "b" fails; ARB grows to 1 char ("a"),
  ;; "b" matches -> [0,2] in "abc"
  (let [p (list 'SEQ ARB "b")]
    (is (= [0 2] (SEARCH "abc" p)))
    (is (= [0 2] (SEARCH "ab"  p)))))

(deftest arb-sandwich
  ;; "x" ARB "y" in "xhelloy" -> ARB matches "hello"
  (let [p (list 'SEQ "x" ARB "y")]
    (is (= [0 7] (SEARCH "xhelloy" p)))
    ;; shortest match: "xy" -> ARB=""
    (is (= [0 2] (SEARCH "xy" p)))))

;; ── ARBNO ────────────────────────────────────────────────────────────────────

(deftest arbno-basic
  ;; ARBNO("a") matches zero or more "a"s, shortest first
  (let [p (ARBNO "a")]
    (is (= [0 0] (SEARCH "aaa" p)))   ; shortest = 0 at pos 0
    (is (= [0 0] (FULLMATCH "" p))))) ; 0 reps of "a" = empty string

(deftest arbno-anchored
  ;; POS(0) ARBNO("a") RPOS(0) matches "aaa" entirely
  (let [p (list 'SEQ (POS 0) (ARBNO "a") (RPOS 0))]
    (is (= [0 3] (SEARCH "aaa" p)))
    (is (= [0 0] (SEARCH "" p)))))

(deftest arbno-with-span
  ;; ARBNO(SPAN(digits) ".") matches "1.22.333."
  (let [digits "0123456789"
        p      (list 'SEQ (POS 0) (ARBNO (list 'SEQ (SPAN digits) ".")) (RPOS 0))]
    (is (= [0 9] (SEARCH "1.22.333." p)))
    (is (= [0 0] (SEARCH "" p)))))

(deftest arbno-backtrack
  ;; SEQ[ARBNO("a"), "b"] on "aaab":
  ;; ARBNO first yields "" (0 reps), "b" fails; then "a" (1 rep), fails; then "aa" (2 reps), fails;
  ;; then "aaa" (3 reps), "b" matches -> [0,4]
  (let [p (list 'SEQ (ARBNO "a") "b")]
    (is (= [0 4] (SEARCH "aaab" p)))
    (is (= [0 1] (SEARCH "b"    p)))))
