(ns SNOBOL4clojure.test-operators
  ;; Tests for SNOBOL4 operators and the EVAL/EVAL!/INVOKE evaluator.
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.operators :refer :all]
            [SNOBOL4clojure.env       :refer [ε &LCASE &UCASE &DIGITS]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── ? (match operator) ───────────────────────────────────────────────────────
(deftest test-match-op
  (is (? "hello" "hel"))          ; substring match
  (is (? "hello" "hello"))
  (is (not (? "hello" "world"))))

;; ── | (alternation operator) ─────────────────────────────────────────────────
(deftest test-alt-op
  (let [P (| "cat" "dog")]
    (is (? "cat" P))
    (is (? "dog" P))
    (is (not (? "fish" P))))
  ;; Multi-arg
  (let [P (| "a" "b" "c")]
    (is (? "a" P))
    (is (? "b" P))
    (is (? "c" P))
    (is (not (? "d" P)))))

;; ── EQ / NE / LT / LE / GT / GE ──────────────────────────────────────────────
(deftest test-numeric-comparison
  (is      (EQ 1 1))
  (is (not (EQ 1 2)))
  (is      (NE 1 2))
  (is      (LT 1 2))
  (is (not (LT 2 1)))
  (is      (LE 1 1))
  (is      (LE 1 2))
  (is (not (LE 2 1)))
  (is      (GT 2 1))
  (is      (GE 2 2))
  (is (not (GE 1 2))))

;; ── LEQ / LNE (string comparison) ────────────────────────────────────────────
(deftest test-string-comparison
  (is      (LEQ "abc" "abc"))
  (is (not (LEQ "abc" "def")))
  (is      (LNE "abc" "xyz")))

;; ── IDENT / DIFFER ───────────────────────────────────────────────────────────
(deftest test-identity
  (let [x "hello"]
    ;; JVM interns string literals, so "hello" IS identical? to "hello"
    (is      (IDENT  x x))
    (is      (IDENT  x "hello"))    ; interned — same object
    (is (not (DIFFER x x)))
    (is (not (DIFFER x "hello"))))  ; interned — same object
  ;; IDENT uses value equality — (String. "hello") equals "hello"
  (let [x (str "hel" "lo")
        y (String. "hello")]
    (is      (IDENT x y))           ; same value → IDENT succeeds
    (is (not (DIFFER x y)))))       ; same value → DIFFER fails

;; ── EVAL with quoted expressions ──────────────────────────────────────────────
(deftest test-eval-quoted
  (let [P (EVAL '(ANY "aeiou"))]
    (is (? "apple" P))
    (is (not (? "xyz" P))))
  ;; Nested ALT
  (let [P (EVAL '(| "cat" "dog"))]
    (is (? "dog" P))))

;; ── EVAL with vector (SEQ) ───────────────────────────────────────────────────
(deftest test-eval-seq
  ;; Vector evaluates to a SEQ pattern
  (let [P (EVAL '["hel" "lo"])]
    (is (? "hello" P))
    (is (not (? "world" P)))))

;; ── EVAL with string (parses SNOBOL4 syntax) ─────────────────────────────────
(deftest test-eval-string
  (let [P (EVAL "ANY('aeiou')")]
    (is (? "apple" P))
    (is (not (? "xyz" P)))))

;; ── + and - operators ────────────────────────────────────────────────────────
(deftest test-arithmetic-ops
  ;; These produce Clojure list expressions (deferred arithmetic)
  ;; rather than immediately computing — that is by design.
  (is (list? (+ 1 2)))
  (is (list? (- 5 3)))
  (is (list? (* 2 3)))
  (is (list? (/ 6 2))))
