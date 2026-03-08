(ns SNOBOL4clojure.catalog.t-terminal
  "Sprint 25C — TERMINAL variable writes to stderr, not stdout.

   TERMINAL in SNOBOL4 writes to the console/terminal bypassing the
   standard OUTPUT channel.  In SNOBOL4clojure it writes to *err*.
   Tests verify:
     - TERMINAL output appears in :stderr (not :stdout)
     - OUTPUT still goes to :stdout
     - Both can be used in the same program without interference
  "
  (:require [clojure.test :refer [deftest is]]
            [SNOBOL4clojure.test-helpers :refer [prog]]))

(deftest terminal_writes_to_stderr
  "TERMINAL = 'msg' writes to stderr, stdout is empty"
  (let [r (prog "
        TERMINAL = 'error message'
END")]
    (is (= "" (:stdout r))
        "TERMINAL should NOT appear on stdout")
    (is (.contains (:stderr r) "error message")
        "TERMINAL should appear on stderr")))

(deftest terminal_and_output_independent
  "OUTPUT goes to stdout, TERMINAL goes to stderr — independent streams"
  (let [r (prog "
        OUTPUT = 'on stdout'
        TERMINAL = 'on stderr'
END")]
    (is (= "on stdout\n" (:stdout r))
        "OUTPUT should appear on stdout")
    (is (.contains (:stderr r) "on stderr")
        "TERMINAL should appear on stderr")
    (is (not (.contains (:stdout r) "on stderr"))
        "TERMINAL content must not bleed into stdout")))
