(ns SNOBOL4clojure.test-env
  ;; Tests for env.clj: globals, DATATYPE dispatch, NAME, $$, arithmetic aliases.
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.env :refer :all])
  (:refer-clojure :exclude [num])
  (:import  [SNOBOL4clojure.env NAME]))

;; ── SNOBOL4 constants ─────────────────────────────────────────────────────────
(deftest test-constants
  (is (= "" ε))
  (is (Double/isNaN η))
  (is (= "0123456789"                  &DIGITS))
  (is (= "abcdefghijklmnopqrstuvwxyz"  &LCASE))
  (is (= "ABCDEFGHIJKLMNOPQRSTUVWXYZ"  &UCASE)))

;; ── DATATYPE dispatch ─────────────────────────────────────────────────────────
(deftest test-datatype
  (is (= "STRING"     (DATATYPE "")))
  (is (= "STRING"     (DATATYPE "hello")))
  (is (= "INTEGER"    (DATATYPE 42)))
  (is (= "REAL"       (DATATYPE 3.14)))
  (is (= "ARRAY"      (DATATYPE (ARRAY "0:9"))))
  (is (= "TABLE"      (DATATYPE (TABLE))))
  (is (= "PATTERN"    (DATATYPE [:LEN 3])))
  (is (= "PATTERN"    (DATATYPE ["hello"])))
  (is (= "NAME"       (DATATYPE 'x)))
  (is (= "EXPRESSION" (DATATYPE '(+ 1 2))))
  (is (= "SET"        (DATATYPE (SET))))
  (is (= "SET"        (DATATYPE #{})))
  (is (= "REGEX"      (DATATYPE #"abc"))))

;; ── NAME deftype ──────────────────────────────────────────────────────────────
(deftest test-name-type
  (let [n (NAME. 'x)]
    (is (= 'x (.n n)))
    (is (= "NAME" (DATATYPE n)))
    (.n n 'y)
    (is (= 'y (.n n)))))

;; ── Arithmetic aliases ────────────────────────────────────────────────────────
(deftest test-arithmetic
  (is (= 5  (Σ+ 2 3)))
  (is (= 2  (subtract 5 3)))
  (is (= 6  (multiply 2 3)))
  (is (= 3  (divide 6 2)))
  (is      (equal     5 5))
  (is (not (equal     5 6)))
  (is      (not-equal 5 6))
  (is (not (not-equal 5 5))))

;; ── num conversion ────────────────────────────────────────────────────────────
(deftest test-num
  (is (= 3.14  (num 3.14)))
  (is (= 42.0  (num 42)))
  (is (= 1.5   (num "1.5")))
  (is (Double/isNaN (num "abc"))))

;; ── $$ lookup ────────────────────────────────────────────────────────────────
;; def must be at top level so $$ can find it via ns-map
(def test-$$-sentinel "hello")
(deftest test-$$
  (is (= "hello" ($$ 'test-$$-sentinel)))
  (is (= ""      ($$ 'no-such-var-xyz))))
