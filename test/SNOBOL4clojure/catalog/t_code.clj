(ns SNOBOL4clojure.catalog.t-code
  "Sprint 25F — CODE(src) compiles and executes a SNOBOL4 source fragment
   in the current environment.

   Tests:
     code_assigns_variable    — CODE('X = 42') sets X in current env
     code_output              — CODE produces output on stdout
     code_sees_outer_env      — fragment reads variables set before CODE call
     code_sets_outer_env      — fragment's assignments visible after CODE call
     code_define_callable     — CODE('DEFINE(...)...') makes function callable
  "
  (:require [clojure.test :refer [deftest is]]
            [SNOBOL4clojure.test-helpers :refer [prog]]
            [SNOBOL4clojure.env :refer [$$]]))

(deftest code_output
  "CODE('OUTPUT = ..') produces output"
  (is (= "dynamic\n"
         (:stdout (prog "
        CODE(\"        OUTPUT = 'dynamic'\")
END")))))

(deftest code_sees_outer_env
  "CODE fragment can read variables set before the CODE call"
  (is (= "hello\n"
         (:stdout (prog "
        MSG = 'hello'
        CODE(\"        OUTPUT = MSG\")
END")))))

(deftest code_sets_outer_env
  "CODE fragment assignments are visible in the outer program after the call"
  (is (= "99\n"
         (:stdout (prog "
        CODE(\"        X = 99\")
        OUTPUT = X
END")))))

(deftest code_define_callable
  "CODE can install a DEFINE; the function is then callable in the outer program"
  (is (= "42\n"
         (:stdout (prog "
        CODE(\"        DEFINE('ANSWER()')\")
ANSWER  OUTPUT = 42
        :(RETURN)
        ANSWER()
END")))))
