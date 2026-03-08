(ns SNOBOL4clojure.test-probe18c
  "Sprint 18C — Step-probe bisection debugger tests.
   Verifies: snapshot!, run-to-step, probe-at, bisect-divergence,
             probe-test macro, run-with-restart, auto-snapshot on step-limit."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout
                                                  run-with-timeout
                                                  run-to-step probe-at
                                                  bisect-divergence
                                                  probe-test
                                                  run-with-restart
                                                  set-steplimit!]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-probe18c)) (f)))

;; Counting loop source.
;; Step 1: I=0 (assignment).  Steps 2–11: loop body runs 10 times.
;; I increments 0→1→2→...→10. Step 12+: program ended.
(def ^:private count-src
  (clojure.string/join "\n"
    ["        I = 0"
     "LOOP    I = I + 1  LT(I,10) :S(LOOP)F(DONE)"
     "DONE"
     "END"]))

;; ── 18C.1 snapshot! ──────────────────────────────────────────────────────────

(deftest test-snapshot-after-run
  "snapshot! captures variable state after a complete run."
  (prog "        X = 42" "        S = 'hello'" "END")
  (let [snap (snapshot!)]
    (is (= 42      (get snap 'X)) "integer variable")
    (is (= "hello" (get snap 'S)) "string variable")))

;; ── 18C.2 run-to-step ────────────────────────────────────────────────────────

(deftest test-run-to-step-step1
  "Step 1: I=0 assigned."
  (let [r (run-to-step count-src 1)]
    (is (= :step-limit (:exit r)))
    (is (= 1 (:steps r)))
    (is (= 0 (get (:vars r) 'I)))))

(deftest test-run-to-step-step5
  "Step 5: I='4' (string) after 4 loop iterations."
  (let [r (run-to-step count-src 5)]
    (is (= :step-limit (:exit r)))
    (is (= "4" (get (:vars r) 'I)))))

(deftest test-run-to-step-completes
  "Large budget: program completes naturally with I=10."
  (let [r (run-to-step count-src 1000)]
    (is (= :ok (:exit r)))
    (is (= 10 (get (:vars r) 'I)))))

;; ── probe-at ─────────────────────────────────────────────────────────────────

(deftest test-probe-at
  "probe-at returns variable value at step n."
  (is (= 0    (probe-at count-src 1  'I)) "step 1: I=0 (integer, direct assign)")
  (is (= "2"  (probe-at count-src 3  'I)) "step 3: I='2' (string via SEQ idiom)")
  (is (= "10" (probe-at count-src 15 'I)) "step 15: program done, I='10'"))

;; ── 18C.3 bisect-divergence ──────────────────────────────────────────────────

(deftest test-bisect-finds-step
  "bisect-divergence finds exact step where I string-value first exceeds '5'."
  ;; I goes: 0(step1,Long) "1"(step2) "2"(step3) ... "6"(step7)
  ;; Use clojure.core/= explicitly since = is shadowed by SNOBOL4's = in this ns.
  (let [result (bisect-divergence count-src 1 20 'I
                  (fn [v] (or (nil? v)
                              (clojure.core/= v 0)
                              (and (string? v) (neg? (compare v "6"))))))]
    (is (some? result)        "divergence should be found")
    (is (clojure.core/= 7 (:step result)) "I becomes '6' at step 7")))

(deftest test-bisect-no-divergence
  "bisect-divergence returns nil when predicate always holds in range."
  (let [result (bisect-divergence count-src 1 3 'I
                  (fn [v] (or (nil? v)
                              (clojure.core/= v 0)
                              (clojure.core/= v "1")
                              (clojure.core/= v "2"))))]
    (is (nil? result) "no divergence in steps 1-3")))

;; ── 18C.5 probe-test macro ───────────────────────────────────────────────────

(deftest test-probe-test-macro
  "probe-test asserts I at specific steps. After step 1 (I=0 int), I is string."
  (probe-test
    {1  {'I 0}
     3  {'I "2"}
     6  {'I "5"}
     11 {'I "10"}}
    "        I = 0"
    "LOOP    I = I + 1  LT(I,10) :S(LOOP)F(DONE)"
    "DONE"
    "END"))

;; ── 18C.9 auto-snapshot on step-limit ────────────────────────────────────────

(deftest test-auto-snapshot-on-step-limit
  "run-with-timeout auto-captures :vars snapshot when :step-limit fires."
  (set-steplimit! 4)
  (let [r (run-with-timeout count-src 2000)]
    (is (= :step-limit (:exit r))   "should hit step limit")
    (is (map? (:vars r))            ":vars key present")
    (is (= "3" (get (:vars r) 'I))  "I='3' (string) at step 4")))

;; ── 18C.6 run-with-restart ───────────────────────────────────────────────────

(deftest test-run-with-restart-inspect
  "run-with-restart stops at n steps and returns restart handle."
  (let [h (run-with-restart count-src 4)]
    (is (= :step-limit (:exit h))   "stopped at step limit")
    (is (= 4 (:steps h))            "4 steps taken")
    (is (= "3" (get (:vars h) 'I))  "I='3' (string) at step 4")
    (is (fn? (:resume h))           ":resume is a function")))
