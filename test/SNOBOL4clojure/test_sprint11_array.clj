(ns SNOBOL4clojure.test-sprint11-array
  "Sprint 11 — ARRAY: multi-dimensional, bounds-checked, integer subscripts,
   default value, PROTOTYPE, DATATYPE."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.env       :refer [GLOBALS $$ DATATYPE array? array-prototype]]
            [SNOBOL4clojure.operators :refer [INVOKE]]
            [SNOBOL4clojure.compiler  :refer [CODE]]
            [SNOBOL4clojure.runtime   :refer [RUN]]))

(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint11-array)) (f)))

(defn run-prog [src]
  (let [start (CODE src)] (RUN start)))

;; ── Construction ─────────────────────────────────────────────────────────────

(deftest array-001-simple-construction
  (testing "array(N) creates a SnobolArray with dims [1..N]"
    (let [a (INVOKE 'ARRAY 10)]
      (is (array? a))
      (is (= "ARRAY" (DATATYPE a)))
      (is (= "1:10" (array-prototype a))))))

(deftest array-002-proto-string
  (testing "array('lo:hi,...') parses dimension specs"
    (let [a (INVOKE 'ARRAY "-5:10,3:5,20")]
      (is (array? a))
      (is (= "-5:10,3:5,1:20" (array-prototype a))))))

(deftest array-003-datatype
  (testing "DATATYPE of array variable via CODE/RUN"
    (run-prog "        A = array(10)
end")
    (is (= "ARRAY" (DATATYPE ($$ 'A))))))

;; ── Read / Write ──────────────────────────────────────────────────────────────

(deftest array-004-write-read
  (testing "A<i> = val then A<i> round-trips"
    (run-prog "        A = array(10)
        A<3> = 'hello'
        r1 = A<3>
end")
    (is (= "hello" ($$ 'r1)))))

(deftest array-005-unset-slot-returns-empty
  (testing "unset slot returns empty string (default)"
    (run-prog "        A = array(10)
        r1 = A<5>
end")
    (is (= "" ($$ 'r1)))))

(deftest array-006-default-value
  (testing "array(N, default) initialises all slots to default value"
    (run-prog "        A = array(10,999)
        r1 = A<3>
        r2 = A<7>
end")
    (is (= 999 ($$ 'r1)))
    (is (= 999 ($$ 'r2)))))

;; ── Bounds checking ───────────────────────────────────────────────────────────

(deftest array-007-bounds-below-fail
  (testing "a<-1> on array(10) fails (index below 1)"
    (run-prog "        a = array(10)
        b = a<-1>       :S(y)F(n)
y       result = 'success' :(end)
n       result = 'failure'
end")
    (is (= "failure" ($$ 'result)))))

(deftest array-008-bounds-zero-fail
  (testing "a<0> on array(10) fails"
    (run-prog "        a = array(10)
        b = a<0>       :S(y)F(n)
y       result = 'success' :(end)
n       result = 'failure'
end")
    (is (= "failure" ($$ 'result)))))

(deftest array-009-bounds-lo-succeed
  (testing "a<1> on array(10) succeeds"
    (run-prog "        a = array(10)
        b = a<1>       :S(y)F(n)
y       result = 'success' :(end)
n       result = 'failure'
end")
    (is (= "success" ($$ 'result)))))

(deftest array-010-bounds-hi-succeed
  (testing "a<10> on array(10) succeeds"
    (run-prog "        a = array(10)
        b = a<10>       :S(y)F(n)
y       result = 'success' :(end)
n       result = 'failure'
end")
    (is (= "success" ($$ 'result)))))

(deftest array-011-bounds-above-fail
  (testing "a<11> on array(10) fails (index above 10)"
    (run-prog "        a = array(10)
        b = a<11>       :S(y)F(n)
y       result = 'success' :(end)
n       result = 'failure'
end")
    (is (= "failure" ($$ 'result)))))

;; ── PROTOTYPE ────────────────────────────────────────────────────────────────

(deftest array-012-prototype-simple
  (testing "PROTOTYPE of array(20) returns '1:20'"
    (run-prog "        A = array(20)
        r1 = prototype(A)
end")
    (is (= "1:20" ($$ 'r1)))))

(deftest array-013-prototype-multi-dim
  (testing "PROTOTYPE of array('-5:10,3:5,20') returns '-5:10,3:5,1:20'"
    (run-prog "        A = array('-5:10,3:5,20')
        r1 = prototype(A)
end")
    (is (= "-5:10,3:5,1:20" ($$ 'r1)))))

;; ── Overwrite ────────────────────────────────────────────────────────────────

(deftest array-014-overwrite
  (testing "writing same slot twice gives second value"
    (run-prog "        A = array(10)
        A<5> = 'first'
        A<5> = 'second'
        r1 = A<5>
end")
    (is (= "second" ($$ 'r1)))))

;; ── Multi-dimensional ────────────────────────────────────────────────────────

(deftest array-015-multi-dim-write-read
  (testing "multi-dimensional array write/read"
    (run-prog "        A = array('3,3')
        A<2,2> = 'center'
        r1 = A<2,2>
        r2 = A<1,1>
end")
    (is (= "center" ($$ 'r1)))
    (is (= "" ($$ 'r2)))))
