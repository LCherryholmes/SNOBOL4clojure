(ns SNOBOL4clojure.test-match
  ;; Tests for the MATCH engine public API: SEARCH, MATCH, FULLMATCH, REPLACE.
  ;; These are the three entry points matching the Python/C# siblings.
  ;; Tests also exercise the engine internals via structural patterns.
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.match    :refer [SEARCH MATCH FULLMATCH REPLACE]]
            [SNOBOL4clojure.patterns :refer [ANY SPAN POS RPOS LEN]]))

;; ── SEARCH — slides pattern across subject ────────────────────────────────────
(deftest test-search-basic
  (is      (SEARCH "hello world" "world"))
  (is      (SEARCH "hello"       "hello"))
  (is (not (SEARCH "hello"       "xyz")))
  (is      (SEARCH "abcdef"      "cde")))  ; interior match

(deftest test-search-returns-span
  ;; SEARCH returns [start end] — a half-open span
  (is (= [6 11] (SEARCH "hello world" "world")))
  (is (= [0  5] (SEARCH "hello world" "hello")))
  (is (= [2  5] (SEARCH "abcde"       "cde"))))

(deftest test-search-empty-pattern
  ;; Empty string matches at position 0
  (is (= [0 0] (SEARCH "hello" ""))))

;; ── MATCH — anchored at position 0 ───────────────────────────────────────────
(deftest test-match-anchored
  (is      (MATCH "hello world" "hello"))   ; prefix match
  (is (not (MATCH "hello world" "world")))  ; not at position 0
  (is      (MATCH "abc" "")))               ; empty always matches at 0

(deftest test-match-returns-span
  (is (= [0 5] (MATCH "hello world" "hello")))
  (is (= [0 0] (MATCH "hello" ""))))

;; ── FULLMATCH — anchored at both ends ────────────────────────────────────────
(deftest test-fullmatch
  (is      (FULLMATCH "hello" "hello"))
  (is (not (FULLMATCH "hello world" "hello")))  ; trailing chars
  (is (not (FULLMATCH "hello" "hell")))          ; trailing chars
  (is      (FULLMATCH "" "")))

(deftest test-fullmatch-returns-span
  (is (= [0 5] (FULLMATCH "hello" "hello")))
  (is (= [0 0] (FULLMATCH "" ""))))

(deftest test-fullmatch-with-pattern
  (let [digits (SPAN "0123456789")]
    (is      (FULLMATCH "12345" digits))
    (is (not (FULLMATCH "123ab" digits)))))

;; ── REPLACE — match and substitute ───────────────────────────────────────────
(deftest test-replace-basic
  (is (= "hello Clojure" (REPLACE "hello world"  "world"   "Clojure")))
  (is (= "Xello"         (REPLACE "hello"        "h"       "X")))
  (is (= "hello world"   (REPLACE "hello world"  "hello"   "hello"))))

(deftest test-replace-no-match
  (is (nil? (REPLACE "hello" "xyz" "!"))))

(deftest test-replace-empty
  ;; Replacing first empty match inserts at position 0
  (is (= "Xhello" (REPLACE "hello" "" "X"))))

;; ── Literal patterns (LIT$ dispatch) ─────────────────────────────────────────
(deftest test-literal
  (is (SEARCH "hello"       "hel"))
  (is (SEARCH "hello world" "hello"))
  (is (not (SEARCH "world" "hello")))
  (is (SEARCH "abc"   "")))

;; ── SEQ (vector → sequence) ───────────────────────────────────────────────────
(deftest test-seq
  (is      (SEARCH "hello"  (list 'SEQ "hel" "lo")))
  (is (not (SEARCH "hello"  (list 'SEQ "hel" "xx"))))
  (is      (SEARCH "abcdef" (list 'SEQ "abc" "def"))))

;; ── ALT (alternation) ─────────────────────────────────────────────────────────
(deftest test-alt
  (let [P (list 'ALT "cat" "dog" "bird")]
    (is      (SEARCH "cat"  P))
    (is      (SEARCH "dog"  P))
    (is      (SEARCH "bird" P))
    (is (not (SEARCH "fish" P)))))

;; ── Anchoring with POS / RPOS ─────────────────────────────────────────────────
(deftest test-fullmatch-anchoring
  (let [P (list 'SEQ (POS 0) "hello" (RPOS 0))]
    (is      (SEARCH "hello"    P))
    (is (not (SEARCH "hello!"   P)))
    (is (not (SEARCH "  hello"  P)))))

(deftest test-search-finds-interior
  ;; SEARCH slides — finds "hello" even with leading/trailing chars
  (is (= [4 9] (SEARCH "say hello there" "hello"))))

;; ── Nested ALT inside SEQ ─────────────────────────────────────────────────────
(deftest test-nested-alt-seq
  (let [P (list 'SEQ
             (POS 0)
             (list 'ALT "B" "F" "R")
             (list 'ALT "E" "EA")
             (list 'ALT "D" "DS")
             (RPOS 0))]
    (is (SEARCH "BED"   P))
    (is (SEARCH "BEDS"  P))
    (is (SEARCH "BEAD"  P))
    (is (SEARCH "BEADS" P))
    (is (SEARCH "RED"   P))
    (is (SEARCH "READS" P))
    (is (not (SEARCH "LEADER" P)))
    (is (not (SEARCH "ZED"    P)))))

;; ── Backtracking ──────────────────────────────────────────────────────────────
;; TODO: cross-SEQ backtracking not yet implemented.
(deftest test-backtracking
  (is (SEARCH "BAD" (list 'SEQ (list 'ALT "BE" "B") "AD" (RPOS 0))))
  #_(is (not (SEARCH "BEAD" (list 'SEQ (list 'ALT "BE" "B") "AD" (RPOS 0))))))

;; ── ANY / SPAN patterns ───────────────────────────────────────────────────────
(deftest test-primitive-dispatch
  (let [vowels (ANY "aeiou")
        digits (SPAN "0123456789")]
    (is (SEARCH "apple" vowels))
    (is (SEARCH "123"   digits))
    (is (not (FULLMATCH "xyz" vowels)))
    (is      (FULLMATCH "999" digits))))

;; ── LEN ──────────────────────────────────────────────────────────────────────
(deftest test-len-match
  (let [L3 (LEN 3)]
    (is      (SEARCH "abc"    L3))
    (is      (SEARCH "abcdef" L3))
    (is (not (FULLMATCH "ab"  L3)))))
