(ns SNOBOL4clojure.catalog.t-opsyn
  "Sprint 25E: OPSYN operator/function synonyms and LOAD stub."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]))

(use-fixtures :each (fn [f]
                      (GLOBALS (find-ns 'SNOBOL4clojure.catalog.t-opsyn))
                      (reset! <CHANNELS> {})
                      (f)))

(defn- run-prog [src]
  (with-out-str
    (try (RUN (CODE-memo src))
         (catch clojure.lang.ExceptionInfo e
           (when-not (= :end (get (ex-data e) :snobol/signal)) (throw e))))))

;; ── 25E.1: OPSYN — built-in function synonym ─────────────────────────────────
(deftest opsyn-builtin-fn-synonym
  (testing "OPSYN('LENGTH','SIZE') makes LENGTH work like SIZE"
    (is (clojure.string/includes?
          (run-prog "        OPSYN('LENGTH','SIZE')\n        OUTPUT = LENGTH('hello')\nEND\n")
          "5"))))

;; ── 25E.2: OPSYN — synonym dispatches via INVOKE ─────────────────────────────
(deftest opsyn-invoke-dispatch
  (testing "After OPSYN, INVOKE dispatches to the aliased function"
    (INVOKE 'OPSYN "MYSIZE" "SIZE")
    (is (= 5 (INVOKE 'MYSIZE "hello")))))

;; ── 25E.3: OPSYN — user-defined function synonym ─────────────────────────────
(deftest opsyn-user-fn-synonym
  (testing "OPSYN aliases a user-defined DEFINE'd function"
    (is (clojure.string/includes?
          (run-prog "        DEFINE('DOUBLE(X)')\n        OPSYN('TWICE','DOUBLE')                  :(DOUBLE_END)\nDOUBLE  DOUBLE = X X                             :(RETURN)\nDOUBLE_END\n        OUTPUT = TWICE('ab')\nEND\n")
          "abab"))))

;; ── 25E.4: OPSYN — binary operator synonym (n=2) ─────────────────────────────
(deftest opsyn-binary-op-as-function
  (testing "OPSYN('ADD','+',2) makes ADD(3,4) = 7"
    (is (clojure.string/includes?
          (run-prog "        OPSYN('ADD','+',2)\n        OUTPUT = ADD(3,4)\nEND\n")
          "7"))))

;; ── 25E.5: OPSYN — unary operator synonym (n=1) ──────────────────────────────
(deftest opsyn-unary-op-as-function
  (testing "OPSYN('NEG','-',1) makes NEG(5) = -5"
    (is (clojure.string/includes?
          (run-prog "        OPSYN('NEG','-',1)\n        OUTPUT = NEG(5)\nEND\n")
          "-5"))))

;; ── 25E.6: OPSYN — chain (A→SIZE, B→A resolves to SIZE) ──────────────────────
(deftest opsyn-chain
  (testing "OPSYN chain: A→SIZE, B→A both call SIZE"
    (is (clojure.string/includes?
          (run-prog "        OPSYN('A','SIZE')\n        OPSYN('B','A')\n        OUTPUT = B('xyz')\nEND\n")
          "3"))))

;; ── 25E.7: OPSYN — Gimpel-style REVERSE alias ────────────────────────────────
(deftest opsyn-gimpel-style
  (testing "OPSYN('REVSTR','REVERSE') works in a program"
    (is (clojure.string/includes?
          (run-prog "        OPSYN('REVSTR','REVERSE')\n        OUTPUT = REVSTR('HELLO')\nEND\n")
          "OLLEH"))))

;; ── 25E.8: LOAD — graceful failure (stub) ────────────────────────────────────
(deftest load-stub-fails-gracefully
  (testing "LOAD returns failure (nil) without crashing"
    (is (clojure.string/includes?
          (run-prog "        LOAD('MYFN','libmyfn.so')          :S(OK)F(FAIL)\nOK      OUTPUT = 'loaded'\n                                         :(END)\nFAIL    OUTPUT = 'not-loaded'\nEND\n")
          "not-loaded"))))
