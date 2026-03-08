(ns SNOBOL4clojure.test-diag
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog]]))
(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-diag)) (f)))

(deftest diag-arbno
  ;; What does ARBNO('ab') actually return when called from prog?
  (prog "        P = ARBNO('ab')" "        S = 'ababcd'" "        OUTPUT = DATATYPE(P)" "END")
  (println "ARBNO datatype:" ($$ 'P))
  (is true))

(deftest diag-arbno-match
  ;; Try direct match with prog using ARBNO pattern
  (prog
    "        S = 'abXY'"
    "        S ARBNO('ab') . R 'XY'"
    "END")
  (println "R after ARBNO match:" ($$ 'R))
  (is true))

(deftest diag-conj
  (prog
    "        S = 'hello'"
    "        S CONJ(LEN(5), ANY('h')) . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (println "CONJ result OK=" ($$ 'OK) "R=" ($$ 'R))
  (is true))

(deftest diag-bal
  (prog
    "        S = '(a+b) rest'"
    "        S BAL . R ' rest'"
    "END")
  (println "BAL result R=" ($$ 'R))
  (is true))

(deftest diag-arbno-capture2
  ;; Try ARBNO capture a different way — without the . inside
  (prog
    "        S = 'ababcd'"
    "        S POS(0) ARBNO('ab') . R RPOS(0)"
    "END")
  (println "ARBNO anchored R=" ($$ 'R))
  (is true))

(deftest diag-bal-unbalanced
  ;; What does BAL do on unbalanced input?
  (prog
    "        S = '(a+b'"
    "        S BAL . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (println "BAL unbalanced: OK=" ($$ 'OK) "R=" ($$ 'R))
  (is true))

(deftest diag-conj2
  ;; CONJ: both P and Q must match SAME span — try same-span patterns
  (prog
    "        S = 'hello'"
    "        S CONJ(LEN(5), SPAN('helo')) . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (println "CONJ same-span: OK=" ($$ 'OK) "R=" ($$ 'R))
  (is true))

(deftest diag-arbno-ir
  (let [ast (SNOBOL4clojure.grammar/parse-statement "        S ARBNO('ab') . R 'XY'")
        ir  (SNOBOL4clojure.emitter/emitter ast)]
    (println "ARBNO IR:" ir))
  (is true))

(deftest diag-breakx-capture-prog
  ;; Test if BREAKX . R works via prog
  (prog
    "        S = 'hello'"
    "        S BREAKX('aeiou') . R 'e'"
    "END")
  (println "BREAKX prog capture R=" ($$ 'R))
  (is true))

(deftest diag-arbno-isolated
  ;; Fresh test with different var name to avoid cross-test pollution
  (prog
    "        S = 'ababXY'"
    "        S ARBNO('ab') . CAPTURED 'XY'"
    "END")
  (println "ARBNO isolated CAPTURED=" ($$ 'CAPTURED))
  (is true))

(deftest diag-arbno-trace
  ;; Direct API call to see if CAPTURE-COND + ARBNO works at the Clojure level
  (let [result (SNOBOL4clojure.match/SEARCH
                 "ababXY"
                 (list 'CAPTURE-COND
                       (SNOBOL4clojure.patterns/ARBNO "ab")
                       'CAPTURED2))]
    (println "Direct SEARCH result:" result)
    (println "CAPTURED2 after SEARCH:" ($$ 'CAPTURED2))
    (is true)))
