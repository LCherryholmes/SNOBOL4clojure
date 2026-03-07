(ns SNOBOL4clojure.test-runtime
  "Stage 6 runtime tests: milestones 6A–6E.
   Each test compiles a SNOBOL4 source string with CODE, runs it with RUN,
   and asserts on the captured stdout."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.compiler :refer [CODE! CODE]]
            [SNOBOL4clojure.runtime  :refer [RUN]]
            [SNOBOL4clojure.env      :as env]))

;; ── Reset helper — clears the global statement table between tests ────────────
(defn reset-rt! []
  (reset! env/STNO   0)
  (reset! env/<STNO> {})
  (reset! env/<LABL> {})
  (reset! env/<CODE> {}))

(use-fixtures :each (fn [t] (reset-rt!) (env/GLOBALS *ns*) (t)))

;; ── 6A: Hello, World ─────────────────────────────────────────────────────────
(deftest milestone-6A-hello-world
  (let [start (CODE "         OUTPUT = \"Hello, World!\"\nEND\n")
        out   (with-out-str (RUN start))]
    (is (= "Hello, World!\n" out))))

;; ── 6B: Counter loop ─────────────────────────────────────────────────────────
(deftest milestone-6B-counter-loop
  (let [prog  (str "         N = 0\n"
                   "LOOP     N = N + 1\n"
                   "         OUTPUT = N\n"
                   "         EQ(N, 5)         :S(END)\n"
                   "                          :(LOOP)\n"
                   "END\n")
        out   (with-out-str (RUN (CODE prog)))]
    (is (= "1\n2\n3\n4\n5\n" out))))

;; ── 6C: Fibonacci ────────────────────────────────────────────────────────────
(deftest milestone-6C-fibonacci
  (let [prog  (str "         A = 0\n"
                   "         B = 1\n"
                   "LOOP     OUTPUT = A\n"
                   "         C = A + B\n"
                   "         A = B\n"
                   "         B = C\n"
                   "         LE(A, 100)       :S(LOOP)\n"
                   "END\n")
        out   (with-out-str (RUN (CODE prog)))]
    (is (= "0\n1\n1\n2\n3\n5\n8\n13\n21\n34\n55\n89\n" out))))

;; ── 6D: Pattern match statement ──────────────────────────────────────────────
(deftest milestone-6D-pattern-match
  ;; Initialise WORD so the test is not sensitive to prior state.
  (env/snobol-set! 'WORD "")
  (let [prog  (str "         SUBJECT = \"The quick brown fox\"\n"
                   "         SUBJECT (SPAN(&LCASE)) . WORD\n"
                   "         OUTPUT = WORD\n"
                   "END\n")
        out   (with-out-str (RUN (CODE prog)))]
    ;; SPAN(&LCASE) matches the first run of lower-case letters: "he"
    (is (= "he\n" out))))

;; ── 6E: User-defined function ─────────────────────────────────────────────────
(deftest milestone-6E-define
  (let [prog  (str "         DEFINE('DOUBLE(N)')       :(EXEC)\n"
                   "DOUBLE   DOUBLE = N + N             :(RETURN)\n"
                   "EXEC     OUTPUT = DOUBLE(3)\n"
                   "         OUTPUT = DOUBLE(7)\n"
                   "END\n")
        out   (with-out-str (RUN (CODE prog)))]
    (is (= "6\n14\n" out))))
