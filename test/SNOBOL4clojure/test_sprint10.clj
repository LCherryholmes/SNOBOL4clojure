(ns SNOBOL4clojure.test-sprint10
  "Sprint 10 — operator completeness: ~P optional, @N cursor, P&Q conjunction, *expr deferred.
   Tests derived from Snobol4.Net reference suite and SNOBOL4 language spec."
  (:require [clojure.test             :refer :all]
            [SNOBOL4clojure.match     :refer [SEARCH FULLMATCH MATCH COLLECT!]]
            [SNOBOL4clojure.patterns  :refer [ANY SPAN LEN POS RPOS ARB ARBNO
                                              CURSOR CONJ DEFER FAIL]]
            [SNOBOL4clojure.operators :refer [| tilde]]
            [SNOBOL4clojure.env       :refer [GLOBALS ε $$ snobol-set!]])
  (:refer-clojure :exclude [* num]))

(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint10)) (f)))

;; ── ~P optional (tilde = P | ε) ──────────────────────────────────────────────

(deftest test-tilde-basic
  ;; ~P matches P or nothing — so it always succeeds
  (is (= [0 1] (SEARCH "abc" (tilde (ANY "a")))))    ; matches "a"
  (is (= [0 0] (SEARCH "abc" (tilde (ANY "z")))))    ; P fails → matches empty at 0
  (is (= [0 3] (FULLMATCH "abc" (tilde (LEN 3))))))  ; matches full string

(deftest test-tilde-optional-segment
  ;; optional prefix: ~"Mr " followed by mandatory span of letters
  (let [P (list 'SEQ (tilde "Mr ") (SPAN "Smith"))]
    (is (SEARCH "Mr Smith" P))
    (is (SEARCH "Smith" P))))

(deftest test-tilde-in-seq
  ;; ~P inside a sequence: optional "s" suffix
  (let [P (list 'SEQ "colour" (tilde "s"))]
    (is (= [0 6] (SEARCH "colour"  P)))
    (is (= [0 7] (SEARCH "colours" P)))))

;; ── @N cursor assignment ──────────────────────────────────────────────────────

(deftest test-cursor-basic
  ;; After matching 3 chars, @N captures cursor position 3
  (let [P (list 'SEQ (LEN 3) (CURSOR 'C))]
    (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint10))
    (SEARCH "hello" P)
    (is (= 3 ($$ 'C)))))

(deftest test-cursor-at-start
  ;; @N at start captures 0
  (let [P (list 'SEQ (CURSOR 'C) (LEN 2))]
    (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint10))
    (MATCH "hello" P)
    (is (= 0 ($$ 'C)))))

(deftest test-cursor-mid-pattern
  ;; Capture cursor at two points in one pattern
  (let [P (list 'SEQ (LEN 2) (CURSOR 'A) (LEN 3) (CURSOR 'B))]
    (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint10))
    (MATCH "hello!" P)
    (is (= 2 ($$ 'A)))
    (is (= 5 ($$ 'B)))))

;; ── CONJ P&Q conjunction ──────────────────────────────────────────────────────

(deftest test-conj-basic
  ;; Both must match same span: ARB(1) ∩ "ab" = "ab" at pos 0
  (is (= [0 2] (SEARCH "abcd" (CONJ (LEN 2) "ab"))))
  ;; No position where LEN(2) and "xyz" agree → nil
  (is (nil? (SEARCH "abcd" (CONJ (LEN 2) "xyz")))))

(deftest test-conj-span-and-literal
  ;; SPAN("hello") ∩ "hello" — both produce the same span [0 5]
  (is (= [0 5] (SEARCH "hello world" (CONJ (SPAN "helo") "hello"))))
  ;; SPAN("helo") doesn't include 'w','r','d' so won't agree with "world"
  (is (nil? (SEARCH "hello world" (CONJ (SPAN "helo") "world")))))

(deftest test-conj-fail-length-mismatch
  ;; P matches 3 chars, Q matches 2 chars — ends disagree → nil
  (is (nil? (SEARCH "abc" (CONJ (LEN 3) (LEN 2))))))

;; ── *expr deferred guard ──────────────────────────────────────────────────────

(deftest test-defer-basic
  ;; Thunk returns a literal pattern evaluated at match time
  (let [P (DEFER (fn [] "bc"))]
    (is (= [1 3] (SEARCH "abcd" P)))))

(deftest test-defer-uses-runtime-var
  ;; Classic eager-eval bug: (EQ N 2) inside ALT is computed at build time,
  ;; so N's value at build time (ε) makes EQ fail, and the branch is never taken.
  ;; DEFER wraps it in a thunk so EQ runs during match when N is already set.
  (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint10))
  (snobol-set! 'N 0)
  (let [counted-p (list 'SEQ
                        (ARBNO (list 'SEQ
                                     (ANY "abc")
                                     (DEFER (fn []
                                              (snobol-set! 'N (inc ($$ 'N)))
                                              ε)))))]
    (FULLMATCH "abc" counted-p)
    (is (= 3 ($$ 'N)))))

(deftest test-defer-lazy-pattern-selection
  ;; Thunk picks pattern based on a variable set before match
  (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint10))
  (snobol-set! 'MODE "vowel")
  (let [P (DEFER (fn [] (if (= ($$ 'MODE) "vowel")
                          (ANY "aeiou")
                          (ANY "bcdfghjklmnpqrstvwxyz"))))]
    (is (= [0 1] (SEARCH "apple" P)))   ; vowel mode → matches "a" at 0
    (snobol-set! 'MODE "consonant")
    (is (= [1 2] (SEARCH "apple" P))))) ; consonant mode → matches "p" at 1
