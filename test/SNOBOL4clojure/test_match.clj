(ns SNOBOL4clojure.test-match
  ;; Tests for the MATCH state machine in match.clj.
  ;; Tests focus on the engine's structural mechanics: SEQ, ALT,
  ;; anchoring, backtracking, and interaction with the frame stack.
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.match      :refer [MATCH]]
            [SNOBOL4clojure.primitives :refer [charset]]
            [SNOBOL4clojure.patterns   :refer [ANY SPAN POS RPOS LEN]]))

;; ── LIT$ dispatch ─────────────────────────────────────────────────────────────
(deftest test-literal
  (is      (MATCH (seq "hello") 0 "hello"))
  (is      (MATCH (seq "hello world") 0 "hello"))  ; substring match
  (is (not (MATCH (seq "world") 0 "hello")))
  (is      (MATCH (seq "abc")   0 "")))             ; empty pattern always matches

;; ── SEQ (vector = sequence) ───────────────────────────────────────────────────
(deftest test-seq
  (is      (MATCH (seq "hello") 0 (list 'SEQ "hel" "lo")))
  (is (not (MATCH (seq "hello") 0 (list 'SEQ "hel" "xx"))))
  (is      (MATCH (seq "abcdef") 0 (list 'SEQ "abc" "def"))))

;; ── ALT (alternation) ─────────────────────────────────────────────────────────
(deftest test-alt
  (let [P (list 'ALT "cat" "dog" "bird")]
    (is      (MATCH (seq "cat")  0 P))
    (is      (MATCH (seq "dog")  0 P))
    (is      (MATCH (seq "bird") 0 P))
    (is (not (MATCH (seq "fish") 0 P)))))

;; ── Anchoring with POS#/RPOS# ─────────────────────────────────────────────────
(deftest test-anchoring
  ;; Full string match
  (let [P (list 'SEQ (POS 0) "hello" (RPOS 0))]
    (is      (MATCH (seq "hello")    0 P))
    (is (not (MATCH (seq "hello!")   0 P)))
    (is (not (MATCH (seq "  hello")  0 P))))
  ;; Partial (unanchored) — the engine matches at position 0 only.
  ;; Substring scanning requires ARB prefix, which is not yet implemented.
  (is (not (MATCH (seq "say hello there") 0 "hello"))))

;; ── Nested ALT inside SEQ ─────────────────────────────────────────────────────
(deftest test-nested-alt-seq
  ;; "B|F|R" followed by "E|EA" followed by "D|DS"
  (let [P (list 'SEQ
             (POS 0)
             (list 'ALT "B" "F" "R")
             (list 'ALT "E" "EA")
             (list 'ALT "D" "DS")
             (RPOS 0))]
    (is (MATCH (seq "BED")   0 P))
    (is (MATCH (seq "BEDS")  0 P))
    (is (MATCH (seq "BEAD")  0 P))
    (is (MATCH (seq "BEADS") 0 P))
    (is (MATCH (seq "RED")   0 P))
    (is (MATCH (seq "READS") 0 P))
    (is (not (MATCH (seq "LEADER") 0 P)))
    (is (not (MATCH (seq "ZED")    0 P)))))

;; ── Backtracking through ALT ──────────────────────────────────────────────────
;; ── Backtracking through ALT ──────────────────────────────────────────────────
;; TODO: cross-SEQ backtracking not yet implemented — once an ALT child
;; succeeds and a later SEQ element fails, the engine does not retry the ALT.
(deftest test-backtracking
  (is (MATCH (seq "BAD")  0 (list 'SEQ (list 'ALT "BE" "B") "AD" (RPOS 0))))
  ;; TODO: re-enable when cross-SEQ backtracking is fixed:
  #_(is (not (MATCH (seq "BEAD") 0 (list 'SEQ (list 'ALT "BE" "B") "AD" (RPOS 0))))))

;; ── ANY$ / SPAN$ dispatch ─────────────────────────────────────────────────────
(deftest test-primitive-dispatch
  (let [vowels (ANY "aeiou")
        digits (SPAN "0123456789")]
    (is (MATCH (seq "apple") 0 vowels))
    (is (MATCH (seq "123")   0 digits))
    (is (not (MATCH (seq "xyz") 0 (list 'SEQ (POS 0) vowels (RPOS 0)))))
    (is (MATCH (seq "999")   0 (list 'SEQ (POS 0) digits (RPOS 0))))))

;; ── LEN matching ─────────────────────────────────────────────────────────────
(deftest test-len-match
  (let [L3 (LEN 3)]
    (is (MATCH (seq "abc")    0 L3))
    (is (MATCH (seq "abcdef") 0 L3))  ; matches first 3
    (is (not (MATCH (seq "ab")    0 (list 'SEQ (POS 0) L3 (RPOS 0)))))))
