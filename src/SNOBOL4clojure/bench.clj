(ns SNOBOL4clojure.bench
  "Three-engine × four-backend benchmark grid.
   Run: lein run -m SNOBOL4clojure.bench"
  (:require [SNOBOL4clojure.harness     :as h]
            [SNOBOL4clojure.compiler    :as comp]
            [SNOBOL4clojure.runtime     :as rt]
            [SNOBOL4clojure.transpiler  :as tr]
            [SNOBOL4clojure.vm          :as vm]
            [SNOBOL4clojure.jvm-codegen :as jc]
            [SNOBOL4clojure.env         :as env]
            [clojure.string             :as str]))

;; ── Environment reset ────────────────────────────────────────────────────────
;; Each backend gets its own stable namespace so runs don't contaminate each other.

(defn- mk-ns [sym]
  (or (find-ns sym) (create-ns sym)))

(defn- reset-to! [ns-sym]
  (env/GLOBALS (mk-ns ns-sym))
  (reset! env/STNO 0) (reset! env/<STNO> {}) (reset! env/<LABL> {})
  (reset! env/<CODE> {}) (reset! env/<FUNS> {}) (reset! env/<CHANNELS> {})
  (reset! env/&STCOUNT 0))

(defn run-interpreter [src]
  (reset-to! 'snobol4.bench.interp)
  (with-out-str
    (try (rt/RUN (comp/CODE-memo src))
         (catch clojure.lang.ExceptionInfo e
           (when-not (#{:end :return :freturn :nreturn}
                      (:snobol/signal (ex-data e))) (throw e))))))

(defn run-transpiler [src]
  (with-out-str (tr/run-transpiled! src)))

(defn run-stack-vm [src]
  (reset-to! 'snobol4.bench.vm)
  (with-out-str
    (try (vm/run-vm! (vm/compile-src src))
         (catch clojure.lang.ExceptionInfo e
           (when-not (#{:end :return :freturn :nreturn}
                      (:snobol/signal (ex-data e))) (throw e))))))

(defn run-jvm [src]
  (jc/run-jvm-captured! src))

;; ── Benchmark programs ───────────────────────────────────────────────────────
;; Programs that load via -INCLUDE use comp/preprocess-includes to resolve.
;; We store the resolved source so all backends see the same text.

(def ^:private SDIR "corpus/spitbol")

(defn- include-src [filename]
  (comp/preprocess-includes
    (str "-INCLUDE '" filename "'")
    [SDIR]))

(def PROGRAMS
  [;; ── Pure arithmetic ────────────────────────────────────────────────────
   {:id    "arith-10k"
    :label "Arithmetic loop (I=0..10000, LT branch)"
    :src   "        I = 0\nLOOP    I = I + 1\n        LT(I,10000)   :S(LOOP)\n        OUTPUT = I\nEND\n"
    :reps  30}

   ;; ── String operations ──────────────────────────────────────────────────
   {:id    "strcat-500"
    :label "String concat (500 iters, S grows to 500 chars)"
    :src   "        S = ''\n        I = 0\nLOOP    S = S 'x'\n        I = I + 1\n        LT(I,500)   :S(LOOP)\n        OUTPUT = SIZE(S)\nEND\n"
    :reps  30}

   ;; ── Pattern matching ──────────────────────────────────────────────────
   {:id    "pat-span"
    :label "Pattern: SPAN match loop (1000 iterations)"
    :src   (str "        P = SPAN('aeiou')\n        S = 'hello world foo bar'\n"
                "        I = 0\nLOOP    S P                  :F(MISS)\n"
                "        I = I + 1\n        LT(I,1000) :S(LOOP)\n"
                "MISS    OUTPUT = I\nEND\n")
    :reps  30}

   ;; ── Big-number arithmetic (fact45 via -INCLUDE) ────────────────────────
   {:id    "fact45"
    :label "Factorial 1..45 (big-number string arithmetic)"
    :src   (include-src "testpgms-test3.spt")
    :reps  5}])

;; ── Timing ───────────────────────────────────────────────────────────────────

(defn time-fn [f reps]
  (try
    (f) ; warmup
    (let [t0 (System/nanoTime)]
      (dotimes [_ reps] (f))
      (/ (double (- (System/nanoTime) t0)) (* reps 1e6)))
    (catch Exception e
      (binding [*out* *err*]
        (println "  BENCH-ERR:" (.getMessage e)))
      nil)))

(defn bench-program [{:keys [id label src reps]}]
  (println (str "\n── " id ": " label " (n=" reps ")"))
  (let [sp (time-fn #(h/run-spitbol  src) reps)
        cs (time-fn #(h/run-csnobol4 src) reps)
        i  (time-fn #(run-interpreter src) reps)
        t  (time-fn #(run-transpiler  src) reps)
        v  (time-fn #(run-stack-vm    src) reps)
        j  (time-fn #(run-jvm         src) reps)
        fmt (fn [ms ref]
              (if ms
                (str (format "%8.2f ms" ms)
                     (if ref (format "  (%5.1fx)" (/ (double ref) ms)) "           "))
                "      N/A           "))]
    (println (format "   %-30s %s" "SPITBOL v4.0f"          (fmt sp nil)))
    (println (format "   %-30s %s" "CSNOBOL4 2.3.3"         (fmt cs nil)))
    (println (format "   %-30s %s" "SNOBOL4clj interpreter" (fmt i  i)))
    (println (format "   %-30s %s" "SNOBOL4clj transpiler"  (fmt t  i)))
    (println (format "   %-30s %s" "SNOBOL4clj stack VM"    (fmt v  i)))
    (println (format "   %-30s %s" "SNOBOL4clj JVM codegen" (fmt j  i)))
    {:id id :spitbol sp :csnobol4 cs :interp i :trans t :vm v :jvm j}))

(defn print-grid [results]
  (let [f #(if % (format "%8.2f" %) "     N/A")]
    (println)
    (println "╔══════════════════════════════════════════════════════════════════════════════════════╗")
    (println "║       SNOBOL4 ENGINE BENCHMARK GRID  (ms / run — lower = faster)                    ║")
    (println "╠═══════════════════════╦══════════╦══════════╦══════════╦══════════╦══════════╦══════════╣")
    (println "║ Program               ║ SPITBOL  ║ CSNOBOL4 ║ Interp   ║ Transpil ║ Stack VM ║ JVM code ║")
    (println "╠═══════════════════════╬══════════╬══════════╬══════════╬══════════╬══════════╬══════════╣")
    (doseq [{:keys [id spitbol csnobol4 interp trans vm jvm]} results]
      (println (format "║ %-21s ║%s ║%s ║%s ║%s ║%s ║%s ║"
                       (subs id 0 (min 21 (count id)))
                       (f spitbol) (f csnobol4) (f interp)
                       (f trans) (f vm) (f jvm))))
    (println "╚═══════════════════════╩══════════╩══════════╩══════════╩══════════╩══════════╩══════════╝")
    (println)
    (println "Ratios vs Clojure interpreter shown per-program above.")
    (println "SPITBOL/CSNOBOL4 include process-spawn overhead (~15ms/run).")
    (println (str "Platform: " (System/getProperty "java.vm.name")
                  " " (System/getProperty "java.version")
                  "  Date: " (java.time.LocalDate/now)))))

(defn -main [& _]
  (println "\nSNOBOL4 Backend Benchmark")
  (println "=========================")
  (println "Warming up Clojure backends...")
  (let [w "        I = 0\nLOOP    I = I + 1\n        LT(I,100) :S(LOOP)\nEND\n"]
    (dotimes [_ 3]
      (run-interpreter w) (run-transpiler w) (run-stack-vm w) (run-jvm w)))
  (println "Warmup done.\n")
  (let [results (mapv bench-program PROGRAMS)]
    (print-grid results))
  (println "\nDone.")
  (System/exit 0))
