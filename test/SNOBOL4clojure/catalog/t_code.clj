(ns SNOBOL4clojure.catalog.t-code
  "Sprint 25F — CODE(src) compiles a SNOBOL4 source fragment and returns
   an entry point (integer statement number or label keyword).
   Execution happens by jumping to the entry point with :(entry).
   SPITBOL semantics (Gimpel corpus compatible).
  "
  (:require [clojure.test :refer [deftest is]]
            [SNOBOL4clojure.test-helpers :refer [prog]]
            [SNOBOL4clojure.env :refer [$$]]))

(deftest code_output
  "CODE + computed goto produces output"
  (is (= "dynamic\n"
         (:stdout (prog "
        ENTRY = CODE(\"        OUTPUT = 'dynamic'  :(RETURN)\")
        :(ENTRY)
END")))))

(deftest code_sees_outer_env
  "CODE fragment can read variables set before the CODE call"
  (is (= "hello\n"
         (:stdout (prog "
        MSG = 'hello'
        ENTRY = CODE(\"        OUTPUT = MSG  :(RETURN)\")
        :(ENTRY)
END")))))

(deftest code_sets_outer_env
  "CODE fragment assignments are visible in the outer program after the call"
  (is (= "99\n"
         (:stdout (prog "
        ENTRY = CODE(\"        X = 99  :(AFTER)\")
        :(ENTRY)
AFTER   OUTPUT = X
END")))))

(deftest code_define_callable
  "CODE can install a DEFINE; the function is then callable in the outer program"
  (is (= "42\n"
         (:stdout (prog "
        ENTRY = CODE(\"        DEFINE('ANSWER()')  :(AFTER)\")
        :(ENTRY)
AFTER
ANSWER  OUTPUT = 42
        :(RETURN)
        ANSWER()
END")))))

(deftest code_return_type
  "CODE() returns an integer (first dynamic statement number)"
  (is (= "INTEGER\n"
         (:stdout (prog "
        ENTRY = CODE(\"        X = 1  :(RETURN)\")
        OUTPUT = DATATYPE(ENTRY)
END")))))
