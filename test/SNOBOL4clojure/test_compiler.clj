(ns SNOBOL4clojure.test-compiler
  ;; Tests for the CODE!/CODE compiler in compiler.clj.
  ;; CODE! parses source into a triple [CODES NOS LABELS].
  ;; CODE loads the result into the global statement store.
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.compiler :refer [CODE!]]))

;; ── CODE! basic parse ─────────────────────────────────────────────────────────
(deftest test-code-simple-assignment
  (let [[codes _ _] (CODE! "X = 1")]
    (is (map? codes))
    (is (pos? (count codes)))))

(deftest test-code-with-label
  (let [[_ nos labels] (CODE! "START X = 1")]
    (is (contains? nos :START))
    (is (contains? labels (nos :START)))))

(deftest test-code-multiple-statements
  (let [[codes _ _] (CODE! "A = 1\nB = 2\nC = 3")]
    (is (= 3 (count codes)))))

(deftest test-code-comment-skipped
  ;; Comments should not become statements
  (let [[codes _ _] (CODE! "* This is a comment\nX = 1")]
    (is (= 1 (count codes)))))

(deftest test-code-goto
  (let [[codes nos _] (CODE! "LOOP X = 1 :S(LOOP)")]
    (is (contains? nos :LOOP))
    ;; The LOOP label maps directly as a key in codes
    (is (contains? codes :LOOP))))

(deftest test-code-empty-program
  ;; A blank/whitespace input: the block regex may produce a single empty-line
  ;; entry. Acceptable — either empty or all-nil entries.
  (let [[codes _ _] (CODE! "")]
    (is (or (empty? codes)
            (every? (fn [[_ v]] (every? nil? v)) codes)))))
