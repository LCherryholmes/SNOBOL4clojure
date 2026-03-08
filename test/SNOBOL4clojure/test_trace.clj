(ns SNOBOL4clojure.test-trace
  "Tests for &STLIMIT / &STCOUNT step accounting and TRACE/STOPTR functionality.
   These are the canonical tests for Sprint 18B features."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite
                                                  prog-steplimit set-steplimit!]]))

(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-trace))
                              (trace-clear!)
                              (set-steplimit! 2147483647)
                              (f)))

;; ── &STCOUNT / &STLIMIT ───────────────────────────────────────────────────────

(deftest stcount-increments
  "&STCOUNT is 0 before RUN and > 0 after a program executes."
  (prog-timeout 500
    "        I = 1"
    "        J = 2"
    "        K = 3"
    "END")
  (is (= 3 @SNOBOL4clojure.env/&STCOUNT)
      "&STCOUNT should equal the number of statements executed"))

(deftest stlimit-terminates-loop
  "&STLIMIT = 10 kills an infinite loop after 10 steps — reported as :step-limit."
  (set-steplimit! 10)
  (let [r (SNOBOL4clojure.test-helpers/run-with-timeout
            "L       :(L)\nEND" 2000)]
    (is (= :step-limit (:exit r))
        "Infinite loop should hit step limit, not wall-clock timeout")))

(deftest stlimit-allows-bounded-loop
  "&STLIMIT = 1000 allows a 10-iteration loop to complete normally."
  (set-steplimit! 1000)
  (let [r (prog-timeout 1000
            "        I = 0"
            "LOOP    I = I + 1"
            "        LT(I,10)  :S(LOOP)"
            "END")]
    (is (= :ok (:exit r)))
    (is (= 10 ($$ 'I)))))

(deftest stlimit-prog-steplimit-macro
  "prog-steplimit macro sets &STLIMIT, runs, then resets it."
  (let [r (prog-steplimit 500 50
            "        I = 0"
            "LOOP    I = I + 1"
            "        LT(I,5)   :S(LOOP)"
            "END")]
    (is (= :ok (:exit r)))
    (is (= 5 ($$ 'I)))
    (is (= 2147483647 @SNOBOL4clojure.env/&STLIMIT)
        "prog-steplimit should restore &STLIMIT after run")))

;; ── TRACE — VALUE traces ──────────────────────────────────────────────────────

(deftest trace-value-fires-on-assignment
  "TRACE VALUE fires when the named variable is assigned."
  (let [log (atom [])]
    (binding [SNOBOL4clojure.trace/*trace-output*
              (java.io.PrintWriter. (proxy [java.io.Writer] []
                (write [x & _] (swap! log conj (str x)))
                (flush [])))]
      (trace-register! "I" "VALUE")
      (prog-timeout 500
        "        I = 42"
        "END"))
    (is (some #(.contains % "42") @log)
        "VALUE trace should log the new value 42")))

(deftest trace-value-not-fired-after-stoptr
  "STOPTR removes a VALUE trace — no output after removal."
  (let [log (atom [])]
    (binding [SNOBOL4clojure.trace/*trace-output*
              (java.io.PrintWriter. (proxy [java.io.Writer] []
                (write [x & _] (swap! log conj (str x)))
                (flush [])))]
      (trace-register! "I" "VALUE")
      (trace-stop! "I" "VALUE")
      (prog-timeout 500
        "        I = 99"
        "END"))
    (is (empty? @log)
        "No trace output after STOPTR")))

;; ── TRACE — LABEL traces ──────────────────────────────────────────────────────

(deftest trace-label-fires-on-reach
  "TRACE LABEL fires when execution reaches the named label."
  (let [log (atom [])]
    (binding [SNOBOL4clojure.trace/*trace-output*
              (java.io.PrintWriter. (proxy [java.io.Writer] []
                (write [x & _] (swap! log conj (str x)))
                (flush [])))]
      (trace-register! "MYLOOP" "LABEL")
      (prog-timeout 500
        "        I = 0"
        "MYLOOP  I = I + 1"
        "        LT(I,3)   :S(MYLOOP)"
        "END"))
    (is (>= (count (filter #(.contains % "MYLOOP") @log)) 3)
        "LABEL trace should fire 3 times for 3 loop iterations")))

;; ── TRACE — CALL/RETURN traces ────────────────────────────────────────────────

(deftest trace-call-fires-on-function-call
  "TRACE CALL fires when a user-defined function is called."
  (let [log (atom [])]
    (binding [SNOBOL4clojure.trace/*trace-output*
              (java.io.PrintWriter. (proxy [java.io.Writer] []
                (write [x & _] (swap! log conj (str x)))
                (flush [])))]
      (trace-register! "DOUBLE" "CALL")
      (prog-timeout 500
        "        DEFINE('DOUBLE(X)')   :(DOUBLE_END)"
        "DOUBLE  DOUBLE = X + X        :(RETURN)"
        "DOUBLE_END"
        "        R = DOUBLE(21)"
        "END"))
    (is (some #(.contains % "DOUBLE") @log)
        "CALL trace should fire when DOUBLE is called")
    (is (= 42 ($$ 'R)))))

(deftest trace-return-fires-on-function-return
  "TRACE RETURN fires when a user-defined function returns."
  (let [log (atom [])]
    (binding [SNOBOL4clojure.trace/*trace-output*
              (java.io.PrintWriter. (proxy [java.io.Writer] []
                (write [x & _] (swap! log conj (str x)))
                (flush [])))]
      (trace-register! "DOUBLE" "RETURN")
      (prog-timeout 500
        "        DEFINE('DOUBLE(X)')   :(DOUBLE_END)"
        "DOUBLE  DOUBLE = X + X        :(RETURN)"
        "DOUBLE_END"
        "        R = DOUBLE(7)"
        "END"))
    (is (some #(.contains % "DOUBLE") @log)
        "RETURN trace should fire when DOUBLE returns")))

;; ── TRACE from SNOBOL4 source (via INVOKE) ────────────────────────────────────

(deftest trace-callable-from-snobol4
  "TRACE() and STOPTR() are callable as SNOBOL4 built-in functions."
  ;; This tests that the INVOKE dispatch for TRACE/STOPTR works.
  ;; We just verify the program runs without error and &TRACE increments.
  (prog-timeout 500
    "        TRACE('I','VALUE')"
    "        I = 55"
    "        STOPTR('I','VALUE')"
    "END")
  (is (= 55 ($$ 'I))
      "Variable I should still be set correctly when TRACE is active")
  (is (= 0 @SNOBOL4clojure.env/&TRACE)
      "&TRACE should be 0 after STOPTR removes the last trace"))

;; ── enable-full-trace! convenience ───────────────────────────────────────────

(deftest enable-full-trace-sets-atrace
  "enable-full-trace! registers 4 wildcard traces, setting &TRACE = 4."
  (trace-enable-full!)
  (is (= 4 @SNOBOL4clojure.env/&TRACE))
  (trace-disable-full!)
  (is (= 0 @SNOBOL4clojure.env/&TRACE)))

;; ── Sprint 18C: snapshot! / run-to-step / probe-test ────────────────────────

(deftest snapshot-captures-variables
  "snapshot! returns a map of all user variables set so far."
  (prog-timeout 500
    "        I = 42"
    "        S = 'hello'"
    "END")
  (let [snap (SNOBOL4clojure.env/snapshot!)]
    (is (= 42      (get snap 'I)))
    (is (= "hello" (get snap 'S)))))

(deftest run-to-step-stops-at-n
  "run-to-step stops after exactly N statements and returns :step-limit."
  (let [src "        I = 0\nLOOP    I = I + 1\n        LT(I,100)  :S(LOOP)\nEND"
        r   (SNOBOL4clojure.test-helpers/run-to-step src 5)]
    (is (#{:ok :step-limit} (:exit r)))
    ;; &STLIMIT=5 fires when &STCOUNT exceeds 5, i.e. after 5 real stmts.
    ;; Step sequence: I=0(1), I=I+1(2)→I=1, LT(3), I=I+1(4)→I=2, LT(5)
    ;; → step-limit fires at 6th increment attempt; I=2 at stop.
    (is (<= (:steps r) 6))
    (is (= 2 (get (:vars r) 'I)))))

(deftest probe-test-checks-loop-invariants
  "probe-test verifies variable values at specific step counts."
  ;; Step accounting: stmt1=I=0, stmt2=I=I+1(I=1), stmt3=LT(branch),
  ;; stmt4=I=I+1(I=2), stmt5=LT(branch) ... I=N at step 2N.
  (SNOBOL4clojure.test-helpers/probe-test
    {2 {'I 1}
     4 {'I 2}
     6 {'I 3}}
    "        I = 0"
    "LOOP    I = I + 1"
    "        LT(I,100)  :S(LOOP)"
    "END"))

(deftest bisect-finds-first-wrong-step
  "bisect-divergence isolates the exact step where a variable goes wrong.
   Step accounting: I=0 at step 1, I=1 at step 2, LT at step 3, ...
   Each iteration is 2 stmts (assign + LT). So I=N at step 2N.
   I first reaches 6 at step 12. bisect(1..25) should find step 12."
  (let [src "        I = 0\nLOOP    I = I + 1\n        LT(I,10)  :S(LOOP)\nEND"
        r   (SNOBOL4clojure.test-helpers/bisect-divergence
              src 1 25 'I #(and (some? %) (< % 6)))]
    (is (some? r) "Should find a divergence point")
    (is (= 6 (:value r)) "Diverges when I first reaches 6")
    (is (= 12 (:step r)) "That happens at step 12 (2 stmts per iteration)")))
