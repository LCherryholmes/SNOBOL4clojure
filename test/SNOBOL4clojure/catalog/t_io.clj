(ns SNOBOL4clojure.catalog.t-io
  "Sprint 25D — Named I/O channel tests.
   All tests use real temp files; channels are opened/closed per-test.
   Shape A (atomic): single open + read/write.
   Shape B (bounded): loop with :F(DONE) on EOF — terminates by construction."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.env :as env]
            [SNOBOL4clojure.test-helpers :refer [run-with-timeout]]))

;; ── Fixture ────────────────────────────────────────────────────────────────────
(use-fixtures :each
  (fn [f]
    (GLOBALS)
    (reset! env/<CHANNELS> {})
    (f)
    ;; Close any lingering channels
    (doseq [[_ ch] @env/<CHANNELS>]
      (try (when-let [r (:reader ch)] (.close ^java.io.BufferedReader r)) (catch Exception _))
      (try (when-let [w (:writer ch)] (.close ^java.io.PrintWriter    w)) (catch Exception _)))
    (reset! env/<CHANNELS> {})))

(defn- tmpfile [content]
  (let [f (java.io.File/createTempFile "sno_test_" ".txt")]
    (.deleteOnExit f)
    (spit (.getAbsolutePath f) content)
    (.getAbsolutePath f)))

;; ── INPUT: open and read a single line ────────────────────────────────────────
(deftest t-io-input-single-line
  "INPUT(.RDR, unit, 'file') — read one line via READER = RDR"
  (let [path (tmpfile "hello\n")
        r    (run-with-timeout
               (str "        INPUT(.RDR,5,'" path "')\n"
                    "        LINE = RDR\n"
                    "        OUTPUT = LINE\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "hello\n" (:stdout r)))))

(deftest t-io-input-two-lines
  "Read two lines sequentially from the same channel."
  (let [path (tmpfile "alpha\nbeta\n")
        r    (run-with-timeout
               (str "        INPUT(.RDR,5,'" path "')\n"
                    "        L1 = RDR\n"
                    "        L2 = RDR\n"
                    "        OUTPUT = L1\n"
                    "        OUTPUT = L2\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "alpha\nbeta\n" (:stdout r)))))

(deftest t-io-input-eof-fails
  "Reading past EOF returns nil → statement fails → :F branch taken.
   Shape B: bounded by file size (3 lines)."
  (let [path (tmpfile "one\ntwo\nthree\n")
        r    (run-with-timeout
               (str "        INPUT(.RDR,5,'" path "')\n"
                    "LOOP    LINE = RDR                          :F(DONE)\n"
                    "        OUTPUT = LINE                       :(LOOP)\n"
                    "DONE    OUTPUT = 'EOF'\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "one\ntwo\nthree\nEOF\n" (:stdout r)))))

(deftest t-io-input-empty-file
  "Reading from an empty file immediately hits EOF."
  (let [path (tmpfile "")
        r    (run-with-timeout
               (str "        INPUT(.RDR,5,'" path "')\n"
                    "        LINE = RDR                          :F(EMPTY)\n"
                    "        OUTPUT = 'GOT LINE'\n"
                    "        :(END)\n"
                    "EMPTY   OUTPUT = 'EMPTY'\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "EMPTY\n" (:stdout r)))))

;; ── OUTPUT: open and write lines ───────────────────────────────────────────────
(deftest t-io-output-single-line
  "OUTPUT(.WTR, unit, 'file') — write one line to a file channel."
  (let [path (.getAbsolutePath (doto (java.io.File/createTempFile "sno_out_" ".txt")
                                  (.deleteOnExit)))
        r    (run-with-timeout
               (str "        OUTPUT(.WTR,6,'" path "')\n"
                    "        WTR = 'written'\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "written\n" (slurp path)))))

(deftest t-io-output-multiple-lines
  "Write multiple lines; file should contain all of them."
  (let [path (.getAbsolutePath (doto (java.io.File/createTempFile "sno_out_" ".txt")
                                  (.deleteOnExit)))
        r    (run-with-timeout
               (str "        OUTPUT(.WTR,6,'" path "')\n"
                    "        WTR = 'line1'\n"
                    "        WTR = 'line2'\n"
                    "        WTR = 'line3'\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "line1\nline2\nline3\n" (slurp path)))))

;; ── ENDFILE: close channel ─────────────────────────────────────────────────────
(deftest t-io-endfile-by-unit
  "ENDFILE(unit) closes the channel; <CHANNELS> should be empty after."
  (let [path (tmpfile "x\n")
        r    (run-with-timeout
               (str "        INPUT(.RDR,7,'" path "')\n"
                    "        LINE = RDR\n"
                    "        OUTPUT = LINE\n"
                    "        ENDFILE(7)\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "x\n" (:stdout r)))
    (is (empty? @env/<CHANNELS>) "channel deregistered after ENDFILE")))

(deftest t-io-endfile-before-eof
  "ENDFILE before reading all lines closes file; subsequent read returns nil."
  (let [path (tmpfile "a\nb\nc\n")
        r    (run-with-timeout
               (str "        INPUT(.RDR,7,'" path "')\n"
                    "        LINE = RDR\n"
                    "        OUTPUT = LINE\n"
                    "        ENDFILE(7)\n"
                    "        LINE2 = RDR                         :F(DONE)\n"
                    "        OUTPUT = 'SHOULD NOT PRINT'\n"
                    "DONE    OUTPUT = 'CLOSED'\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "a\nCLOSED\n" (:stdout r)))))

;; ── DETACH: disassociate var from channel ──────────────────────────────────────
(deftest t-io-detach
  "DETACH(.RDR) closes and deregisters the channel."
  (let [path (tmpfile "hello\n")
        r    (run-with-timeout
               (str "        INPUT(.RDR,8,'" path "')\n"
                    "        LINE = RDR\n"
                    "        OUTPUT = LINE\n"
                    "        DETACH(.RDR)\n"
                    "END\n")
               3000)]
    (is (= :ok (:exit r)))
    (is (= "hello\n" (:stdout r)))
    (is (empty? @env/<CHANNELS>) "channel deregistered after DETACH")))

;; ── Multiple simultaneous channels ────────────────────────────────────────────
(deftest t-io-two-input-channels
  "Two input files open simultaneously on different units."
  (let [p1 (tmpfile "foo\n")
        p2 (tmpfile "bar\n")
        r  (run-with-timeout
              (str "        INPUT(.R1,5,'" p1 "')\n"
                   "        INPUT(.R2,6,'" p2 "')\n"
                   "        L1 = R1\n"
                   "        L2 = R2\n"
                   "        OUTPUT = L1\n"
                   "        OUTPUT = L2\n"
                   "END\n")
              3000)]
    (is (= :ok (:exit r)))
    (is (= "foo\nbar\n" (:stdout r)))))

(deftest t-io-input-and-output-channels
  "One input and one output channel open simultaneously."
  (let [in-path  (tmpfile "source line\n")
        out-path (.getAbsolutePath (doto (java.io.File/createTempFile "sno_io_" ".txt")
                                      (.deleteOnExit)))
        r        (run-with-timeout
                   (str "        INPUT(.RDR,5,'"  in-path  "')\n"
                        "        OUTPUT(.WTR,6,'" out-path "')\n"
                        "        LINE = RDR\n"
                        "        WTR = LINE\n"
                        "END\n")
                   3000)]
    (is (= :ok (:exit r)))
    (is (= "source line\n" (slurp out-path)))))

;; ── INPUT without filename — stdin association ─────────────────────────────────
(deftest t-io-input-stdin-register
  "INPUT(.RDR, unit) with no filename registers without crashing.
   We do not actually read from stdin in tests; just verify registration."
  (let [r (run-with-timeout
             "        INPUT(.RDR,9)\n        OUTPUT = 'ok'\nEND\n"
             1000)]
    (is (= :ok (:exit r)))
    (is (= "ok\n" (:stdout r)))))

;; ── Loop reading an entire file ────────────────────────────────────────────────
(deftest t-io-loop-read-file
  "Read all N lines from a file in a loop. Shape B: bounded by file contents."
  (let [lines ["red" "green" "blue" "yellow" "purple"]
        path  (tmpfile (clojure.string/join "\n" (conj lines "")))
        r     (run-with-timeout
                (str "        INPUT(.RDR,5,'" path "')\n"
                     "LOOP    LINE = RDR                      :F(DONE)\n"
                     "        OUTPUT = LINE                   :(LOOP)\n"
                     "DONE END\n")
                3000)]
    (is (= :ok (:exit r)))
    (is (= (clojure.string/join "\n" lines) (clojure.string/trim (:stdout r))))))
