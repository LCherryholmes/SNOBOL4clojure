;; bench/run_benchmarks.clj
;;
;; Three-engine × four-backend benchmark grid.
;;
;; Engines:   SPITBOL v4.0f | CSNOBOL4 2.3.3 | SNOBOL4clojure
;; Backends:  (clojure only) Interpreter | Transpiler | Stack VM | JVM codegen
;;
;; Run with:
;;   cd /home/claude/SNOBOL4clojure
;;   lein exec -p bench/run_benchmarks.clj
;; or from the REPL:
;;   (load-file "bench/run_benchmarks.clj")

(ns bench.run-benchmarks
  (:require [SNOBOL4clojure.harness    :as h]
            [SNOBOL4clojure.compiler   :as comp]
            [SNOBOL4clojure.runtime    :as rt]
            [SNOBOL4clojure.transpiler :as tr]
            [SNOBOL4clojure.vm         :as vm]
            [SNOBOL4clojure.jvm-codegen :as jc]
            [SNOBOL4clojure.env        :as env]
            [clojure.string            :as str]
            [clojure.java.shell        :as sh]))

;; ── Benchmark programs ───────────────────────────────────────────────────────

(def BENCH-DIR "corpus/spitbol/")
(def GIMPEL-DIR "corpus/gimpel/SPITBOL/")

(defn slurp-strip
  "Read a .spt file and strip SPITBOL-only preprocessor directives
   so CSNOBOL4 can also run it."
  [path]
  (let [raw (slurp path)]
    (->> (str/split-lines raw)
         (remove #(re-matches #"^\.\*.*" %))    ; ./* comment blocks
         (remove #(re-matches #"^-TITLE.*" %))   ; -TITLE directives
         (str/join "\n"))))

(def PROGRAMS
  [{:id    "fact45"
    :label "Factorial 1..45 (big-number string arithmetic)"
    :src   (slurp-strip (str BENCH-DIR "testpgms-test3.spt"))
    :reps  5}

   {:id    "bsort"
    :label "Bubble sort N=5 strings (BSORT.INC)"
    :src   (str "-INCLUDE 'BSORT.INC'\n"
                "        ARR = ARRAY(5)\n"
                "        ARR<1> = 'pear'\n"
                "        ARR<2> = 'apple'\n"
                "        ARR<3> = 'mango'\n"
                "        ARR<4> = 'cherry'\n"
                "        ARR<5> = 'banana'\n"
                "        BSORT(.ARR,1,5)\n"
                "        I = 1\n"
                "LOOP    OUTPUT = ARR<I>\n"
                "        I = LT(I,5) I + 1   :S(LOOP)\n"
                "END\n")
    :include-dirs [(str (System/getProperty "user.dir") "/" GIMPEL-DIR)]
    :reps  10}

   {:id    "patmatch"
    :label "Pattern matching — test2 (LEN/TAB/ARB/SPAN/BREAK/ANY/REM)"
    :src   (slurp-strip (str BENCH-DIR "testpgms-test2.spt"))
    :reps  10}

   {:id    "arith"
    :label "Arithmetic loop — count 1..10000"
    :src   "        I = 0\nLOOP    I = I + 1\n        LT(I,10000)   :S(LOOP)\n        OUTPUT = I\nEND\n"
    :reps  20}

   {:id    "strconcat"
    :label "String concat loop — build 500-char string"
    :src   "        S = ''\n        I = 0\nLOOP    S = S 'x'\n        I = I + 1\n        LT(I,500)   :S(LOOP)\n        OUTPUT = SIZE(S)\nEND\n"
    :reps  20}])

;; ── Timing helpers ───────────────────────────────────────────────────────────

(defmacro timed-ms
  "Evaluate body n times after 1 warmup, return average wall-clock ms."
  [n & body]
  `(do
     ~@body  ; warmup
     (let [t0# (System/nanoTime)]
       (dotimes [_# ~n] ~@body)
       (/ (double (- (System/nanoTime) t0#)) (* ~n 1e6)))))

(defn time-shell
  "Time a shell command (spitbol/csnobol4) by running it n times.
   Returns average ms."
  [cmd-fn src n]
  (cmd-fn src)  ; warmup
  (let [t0 (System/nanoTime)]
    (dotimes [_ n] (cmd-fn src))
    (/ (double (- (System/nanoTime) t0)) (* n 1e6))))

(defn reset-env! []
  (env/GLOBALS)
  (reset! env/STNO 0)
  (reset! env/<STNO> {})
  (reset! env/<LABL> {})
  (reset! env/<CODE> {})
  (reset! env/<FUNS> {})
  (reset! env/<CHANNELS> {})
  (reset! env/&STCOUNT 0))

;; ── Per-backend run functions ────────────────────────────────────────────────

(defn run-interpreter [src]
  (reset-env!)
  (with-out-str
    (try (rt/RUN (comp/CODE-memo src))
         (catch clojure.lang.ExceptionInfo e
           (when-not (#{:end :return :freturn :nreturn}
                      (:snobol/signal (ex-data e)))
             (throw e))))))

(defn run-transpiler [src]
  (with-out-str (tr/run-transpiled! src)))

(defn run-stack-vm [src]
  (reset-env!)
  (with-out-str
    (try (vm/run-vm! (vm/compile-src src))
         (catch clojure.lang.ExceptionInfo e
           (when-not (#{:end :return :freturn :nreturn}
                      (:snobol/signal (ex-data e)))
             (throw e))))))

(defn run-jvm-codegen [src]
  (jc/run-jvm-captured! src))

;; ── Benchmark runner ─────────────────────────────────────────────────────────

(defn bench-program [{:keys [id label src reps include-dirs]}]
  (println (str "\n── " id " ──────────────────────────────────────"))
  (println (str "   " label))

  ;; Normalise: strip include dirs for oracle runs (they can't resolve them)
  ;; For programs with -INCLUDE, only run Clojure backends
  (let [has-include? (str/includes? src "-INCLUDE")

        ;; Oracle timings
        sp-ms  (when-not has-include?
                 (try (time-shell #(h/run-spitbol %)  src reps) (catch Exception _ nil)))
        cs-ms  (when-not has-include?
                 (try (time-shell #(h/run-csnobol4 %) src reps) (catch Exception _ nil)))

        ;; Clojure backend timings
        interp-ms  (try (timed-ms reps (run-interpreter   src)) (catch Exception e (do (println "  INTERP ERR:" (.getMessage e)) nil)))
        trans-ms   (try (timed-ms reps (run-transpiler     src)) (catch Exception e (do (println "  TRANS ERR:" (.getMessage e))  nil)))
        vm-ms      (try (timed-ms reps (run-stack-vm       src)) (catch Exception e (do (println "  VM ERR:"    (.getMessage e))  nil)))
        jvm-ms     (try (timed-ms reps (run-jvm-codegen    src)) (catch Exception e (do (println "  JVM ERR:"   (.getMessage e))  nil)))

        fmt  (fn [ms baseline]
               (if ms
                 (let [x (if baseline (format "  (%.1fx)" (/ baseline ms)) "")]
                   (format "%7.1f ms%s" ms x))
                 "       N/A"))

        baseline (or interp-ms 1.0)]

    (println (format "   %-22s %s" "SPITBOL v4.0f"       (fmt sp-ms  nil)))
    (println (format "   %-22s %s" "CSNOBOL4 2.3.3"      (fmt cs-ms  nil)))
    (println (format "   %-22s %s" "Clojure interpreter"  (fmt interp-ms baseline)))
    (println (format "   %-22s %s" "Clojure transpiler"   (fmt trans-ms  baseline)))
    (println (format "   %-22s %s" "Clojure stack VM"     (fmt vm-ms     baseline)))
    (println (format "   %-22s %s" "Clojure JVM codegen"  (fmt jvm-ms    baseline)))

    {:id id :label label
     :spitbol sp-ms :csnobol4 cs-ms
     :interpreter interp-ms :transpiler trans-ms :stack-vm vm-ms :jvm-codegen jvm-ms}))

;; ── Summary grid ─────────────────────────────────────────────────────────────

(defn print-grid [results]
  (println "\n\n╔══════════════════════════════════════════════════════════════════════════════╗")
  (println   "║               SNOBOL4 ENGINE BENCHMARK GRID  (ms per run, lower = faster)  ║")
  (println   "╠══════════════════════════════════════════════════════════════════════════════╣")
  (println   "║ Program            SPITBOL  CSNOBOL4  Interp   Trans    VM      JVM         ║")
  (println   "╠══════════════════════════════════════════════════════════════════════════════╣")
  (doseq [{:keys [id spitbol csnobol4 interpreter transpiler stack-vm jvm-codegen]} results]
    (let [f #(if % (format "%7.2f" %) "    N/A")]
      (println (format "║ %-18s %s  %s  %s  %s  %s  %s ║"
                       (subs id 0 (min 18 (count id)))
                       (f spitbol) (f csnobol4) (f interpreter)
                       (f transpiler) (f stack-vm) (f jvm-codegen)))))
  (println   "╚══════════════════════════════════════════════════════════════════════════════╝"))

;; ── Main ─────────────────────────────────────────────────────────────────────

(println "\nSNOBOL4 Backend Benchmark")
(println "=========================")
(println "Engines:  SPITBOL v4.0f  |  CSNOBOL4 2.3.3  |  SNOBOL4clojure (4 backends)")
(println "Platform:" (System/getProperty "java.vm.name") (System/getProperty "java.version"))
(println)

(def results (mapv bench-program PROGRAMS))
(print-grid results)
(println "\nDone.")
