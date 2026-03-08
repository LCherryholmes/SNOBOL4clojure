(ns SNOBOL4clojure.test-sprint11
  "Sprint 11 — TABLE: atom-backed mutable hash, subscript read/write, ITEM, PROTOTYPE, DATATYPE."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.env       :refer [GLOBALS $$ snobol-set! DATATYPE table? table-get]]
            [SNOBOL4clojure.operators :refer [INVOKE EVAL!]]
            [SNOBOL4clojure.compiler  :refer [CODE]]
            [SNOBOL4clojure.runtime   :refer [RUN]]))

(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint11)) (f)))

(defn run-prog [src]
  (let [start (CODE src)] (RUN start)))

;; ── TABLE construction ────────────────────────────────────────────────────────

(deftest table-001-construction
  (testing "TABLE() creates an atom-backed table"
    (let [t (INVOKE 'TABLE)]
      (is (table? t))
      (is (= "TABLE" (DATATYPE t))))))

(deftest table-002-datatype
  (testing "DATATYPE of table variable"
    (run-prog "        A = table()
end")
    (is (= "TABLE" (DATATYPE ($$ 'A))))))

;; ── Subscript write / read — angle-bracket ───────────────────────────────────

(deftest table-003-angle-write-read
  (testing "A<key> = val then A<key> round-trips"
    (run-prog "        A = table()
        A<'hello'> = 'world'
        r1 = A<'hello'>
end")
    (is (= "world" ($$ 'r1)))))

;; ── Subscript write / read — square-bracket ──────────────────────────────────

(deftest table-004-square-write-read
  (testing "A[key] = val then A[key] round-trips"
    (run-prog "        A = table()
        A['foo'] = 'bar'
        r1 = A['foo']
end")
    (is (= "bar" ($$ 'r1)))))

;; ── Missing key returns ε ─────────────────────────────────────────────────────

(deftest table-005-missing-key
  (testing "A<missing> returns empty string"
    (run-prog "        A = table()
        r1 = A<'nosuchkey'>
end")
    (is (= "" ($$ 'r1)))))

;; ── Variable key ─────────────────────────────────────────────────────────────

(deftest table-006-variable-key
  (testing "table subscript with variable key"
    (run-prog "        A = table()
        K = 'mykey'
        A<K> = 'myval'
        r1 = A<K>
end")
    (is (= "myval" ($$ 'r1)))))

;; ── Reference semantics ───────────────────────────────────────────────────────

(deftest table-007-reference-semantics
  (testing "B = A shares the same atom; writes via A visible through B"
    (run-prog "        A = table()
        B = A
        A<'k'> = 'v'
        r1 = B<'k'>
end")
    (is (= "v" ($$ 'r1)))))

;; ── Multiple keys ────────────────────────────────────────────────────────────

(deftest table-008-multiple-keys
  (testing "multiple keys in same table are independent"
    (run-prog "        A = table()
        A<'x'> = '1'
        A<'y'> = '2'
        A<'z'> = '3'
        r1 = A<'x'>
        r2 = A<'y'>
        r3 = A<'z'>
end")
    (is (= "1" ($$ 'r1)))
    (is (= "2" ($$ 'r2)))
    (is (= "3" ($$ 'r3)))))

;; ── Overwrite ────────────────────────────────────────────────────────────────

(deftest table-009-overwrite
  (testing "writing same key twice gives second value"
    (run-prog "        A = table()
        A<'k'> = 'first'
        A<'k'> = 'second'
        r1 = A<'k'>
end")
    (is (= "second" ($$ 'r1)))))

;; ── ITEM function ─────────────────────────────────────────────────────────────

(deftest table-010-item-read
  (testing "ITEM(table, key) reads same as A<key>"
    (run-prog "        A = table()
        A<'abc'> = 'ABC'
        A<'def'> = 'DEF'
        r1 = item(A,'abc')
        r2 = item(A,'def')
end")
    (is (= "ABC" ($$ 'r1)))
    (is (= "DEF" ($$ 'r2)))))

;; ── PROTOTYPE of table returns ε ─────────────────────────────────────────────

(deftest table-011-prototype
  (testing "PROTOTYPE of a TABLE returns empty string"
    (run-prog "        A = table()
        r1 = prototype(A)
end")
    (is (= "" ($$ 'r1)))))

;; ── Integer keys ─────────────────────────────────────────────────────────────

(deftest table-012-integer-key
  (testing "integer keys work in tables"
    (run-prog "        A = table()
        A<42> = 'answer'
        r1 = A<42>
end")
    (is (= "answer" ($$ 'r1)))))
