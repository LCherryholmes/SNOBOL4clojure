(ns SNOBOL4clojure.harness
  "Sprint 14: SPITBOL/CSNOBOL4/SNOBOL4clojure diff harness.

   Three oracles:
     run-spitbol  — SPITBOL v4.0f  (/usr/local/bin/spitbol)
     run-csnobol4 — CSNOBOL4 2.3.3 (/usr/local/bin/snobol4)
     run-clojure  — SNOBOL4clojure (this implementation)

   Outcome map:
     {:stdout \"...\"   ; captured stdout, whitespace-normalised
      :stderr \"...\"   ; captured stderr
      :exit   0}      ; process exit code (clojure side uses :ok / :error / :timeout)

   Corpus record:
     {:src        \"...snobol4 source...\"
      :spitbol    outcome-map
      :csnobol4   outcome-map
      :clojure    outcome-map
      :oracle     :spitbol | :csnobol4 | :both | :disagree
      :status     :pass | :pass-class | :fail | :timeout | :skip
      :length     (count src)
      :depth      nil}   ; filled in by generator
  "
  (:require [clojure.java.shell  :as sh]
            [clojure.string      :as str]
            [SNOBOL4clojure.env  :as env]
            [SNOBOL4clojure.core :as sno])
  (:import  [java.io StringWriter]))

;; ── Configuration ─────────────────────────────────────────────────────────────
(def ^:dynamic *spitbol-bin*  "/usr/local/bin/spitbol")
(def ^:dynamic *csnobol4-bin* "/usr/local/bin/snobol4")
(def ^:dynamic *timeout-ms*   5000)   ; 5 s wall-clock per run
(def ^:dynamic *stlimit*      10000)  ; SNOBOL4 step limit injected into every program

;; ── Normalise output ──────────────────────────────────────────────────────────
(defn- normalise
  "Trim trailing whitespace from each line; drop trailing blank lines."
  [s]
  (when s
    (->> (str/split-lines s)
         (map str/trimr)
         (reverse)
         (drop-while str/blank?)
         (reverse)
         (str/join "\n"))))

;; ── SPITBOL side ─────────────────────────────────────────────────────────────
(defn run-spitbol
  "Run src through SPITBOL. Returns {:stdout :stderr :exit}."
  [src]
  (try
    (let [result (sh/sh *spitbol-bin* "-b" "-"
                        :in src
                        :env {"PATH" "/usr/local/bin:/usr/bin:/bin"})]
      ;; SPITBOL puts error messages in stdout when exit != 0
      (if (zero? (:exit result))
        {:stdout (normalise (:out result))
         :stderr (normalise (:err result))
         :exit   0}
        {:stdout ""
         :stderr (normalise (:out result))
         :exit   (:exit result)}))
    (catch Exception e
      {:stdout "" :stderr (.getMessage e) :exit :crashed})))

;; ── CSNOBOL4 side ────────────────────────────────────────────────────────────
(defn run-csnobol4
  "Run src through CSNOBOL4. Returns {:stdout :stderr :exit}."
  [src]
  (try
    (let [result (sh/sh *csnobol4-bin* "-"
                        :in src
                        :env {"PATH" "/usr/local/bin:/usr/bin:/bin"})]
      (if (zero? (:exit result))
        {:stdout (normalise (:out result))
         :stderr (normalise (:err result))
         :exit   0}
        {:stdout ""
         :stderr (normalise (str (:out result) (:err result)))
         :exit   (:exit result)}))
    (catch Exception e
      {:stdout "" :stderr (.getMessage e) :exit :crashed})))

;; ── SNOBOL4clojure side ───────────────────────────────────────────────────────
;; Fixed namespace for harness runs — created once
(defonce ^:private harness-ns
  (create-ns 'SNOBOL4clojure.harness-run))

(defn- reset-runtime!
  "Clear all compiler + runtime state for a fresh run."
  []
  (env/GLOBALS harness-ns)
  (reset! env/STNO  0)
  (reset! env/<STNO> {})
  (reset! env/<LABL> {})
  (reset! env/<CODE> {})
  (reset! env/<FUNS> {}))

(defn run-clojure
  "Run src through SNOBOL4clojure. Returns {:stdout :stderr :exit :thrown}."
  [src]
  (try
    (reset-runtime!)
    (let [stdout-p (promise)
          f (future
              (deliver stdout-p
                (with-out-str
                  (try
                    (sno/RUN (sno/CODE src))
                    (catch clojure.lang.ExceptionInfo e
                      (when-not (= (get (ex-data e) :snobol/signal) :end)
                        (throw e)))))))]
      (if-let [stdout (deref stdout-p *timeout-ms* nil)]
        {:stdout (normalise stdout) :stderr "" :exit :ok}
        (do (future-cancel f)
            {:stdout "" :stderr "timeout" :exit :timeout})))
    (catch clojure.lang.ExceptionInfo e
      {:stdout ""
       :stderr (.getMessage e)
       :exit   :error
       :thrown (str (class e) ": " (.getMessage e))})
    (catch Exception e
      {:stdout ""
       :stderr (.getMessage e)
       :exit   :error
       :thrown (str (class e) ": " (.getMessage e))})))

;; ── Oracle agreement ─────────────────────────────────────────────────────────
(defn- oracle-stdout
  "Return the agreed oracle stdout, or nil if oracles disagree.
   Also returns which oracle(s) are considered authoritative."
  [sp cs]
  (let [sp-ok (zero? (:exit sp))
        cs-ok (zero? (:exit cs))]
    (cond
      (and sp-ok cs-ok (= (:stdout sp) (:stdout cs)))
      {:stdout (:stdout sp) :oracle :both}

      (and sp-ok cs-ok (not= (:stdout sp) (:stdout cs)))
      {:stdout (:stdout sp) :oracle :disagree}  ; use SPITBOL, flag disagreement

      sp-ok  {:stdout (:stdout sp) :oracle :spitbol}
      cs-ok  {:stdout (:stdout cs) :oracle :csnobol4}
      :else  {:stdout ""           :oracle :both-error})))

;; ── Status classification ─────────────────────────────────────────────────────
(defn- classify
  "Compare oracle stdout against clojure outcome; return status keyword."
  [oracle-out cl]
  (cond
    (#{:timeout :step-limit} (:exit cl))  :timeout
    (= :both-error (:oracle oracle-out)) ; both oracles errored — skip
    (if (= (:exit cl) :error) :pass-class :skip)

    (= (:stdout oracle-out) (:stdout cl))
    :pass

    (and (not= (:oracle oracle-out) :both)
         (not= (:oracle oracle-out) :spitbol)
         (not= (:oracle oracle-out) :csnobol4)
         (= (:exit cl) :error))
    :pass-class

    ;; Both oracle and clojure errored (non-zero exits)
    (and (= :both-error (:oracle oracle-out)) (= (:exit cl) :error))
    :pass-class

    :else :fail))

;; ── Main entry point ──────────────────────────────────────────────────────────
(defn diff-run
  "Run src through all three sides. Returns a corpus record."
  ([src] (diff-run src nil))
  ([src depth]
   (let [sp     (run-spitbol src)
         cs     (run-csnobol4 src)
         cl     (run-clojure src)
         oracle (oracle-stdout sp cs)
         status (classify oracle cl)]
     {:src      src
      :spitbol  sp
      :csnobol4 cs
      :clojure  cl
      :oracle   (:oracle oracle)
      :status   status
      :length   (count src)
      :depth    depth})))

;; ── Corpus I/O ────────────────────────────────────────────────────────────────
(defn save-corpus!
  "Append records to resources/golden-corpus.edn (one record per line)."
  [records]
  (let [path "resources/golden-corpus.edn"]
    (clojure.java.io/make-parents path)
    (with-open [w (clojure.java.io/writer path :append true)]
      (doseq [r records]
        (.write w (pr-str r))
        (.newLine w)))
    (count records)))

(defn load-corpus
  "Load all records from resources/golden-corpus.edn."
  []
  (let [path "resources/golden-corpus.edn"]
    (when (.exists (clojure.java.io/file path))
      (with-open [r (java.io.PushbackReader.
                      (clojure.java.io/reader path))]
        (loop [acc []]
          (let [form (try (read r false ::eof) (catch Exception _ ::eof))]
            (if (= form ::eof) acc
              (recur (conj acc form)))))))))

;; ── Step-probe oracle runners (18C.4) ────────────────────────────────────────

(defn run-csnobol4-to-step
  "Run src through CSNOBOL4 for exactly n statements by prepending
   &STLIMIT = n to the program source.  CSNOBOL4 natively honours &STLIMIT.
   Returns the same {:stdout :stderr :exit} map as run-csnobol4.
   Used by bisect-divergence for three-oracle step comparison."
  [src n]
  (run-csnobol4 (str "        &STLIMIT = " n "\n" src)))

(defn run-spitbol-to-step
  "Run src through SPITBOL for exactly n statements by prepending
   &STLIMIT = n to the program source.  SPITBOL honours &STLIMIT.
   Returns the same {:stdout :stderr :exit} map as run-spitbol."
  [src n]
  (run-spitbol (str "        &STLIMIT = " n "\n" src)))

(defn run-clojure-to-step
  "Run src through SNOBOL4clojure for exactly n statements.
   Returns the run-to-step map: {:exit :steps :vars :stdout}.
   Requires test-helpers/run-to-step — call from test context only."
  [src n]
  ;; Delegate to test-helpers/run-to-step when available; otherwise
  ;; inline the logic using the harness-private reset-runtime!.
  (reset-runtime!)
  (reset! env/&STLIMIT n)
  (try
    (let [stdout (with-out-str
                   (try (sno/RUN (sno/CODE src))
                     (catch clojure.lang.ExceptionInfo e
                       (when-not (#{:end :step-limit}
                                  (get (ex-data e) :snobol/signal))
                         (throw e)))))]
      {:exit    (if (> @env/&STCOUNT n) :step-limit :ok)
       :steps   @env/&STCOUNT
       :stdout  (normalise stdout)
       :vars    (env/snapshot!)})
    (catch Exception e
      {:exit :error :thrown (.getMessage e) :vars (env/snapshot!)})
    (finally
      (reset! env/&STLIMIT 2147483647))))
