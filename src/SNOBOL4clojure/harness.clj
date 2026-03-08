(ns SNOBOL4clojure.harness
  "Sprint 14: SPITBOL/SNOBOL4clojure diff harness.

   run-spitbol  — run source string through SPITBOL binary, return outcome map
   run-clojure  — run source string through SNOBOL4clojure, return outcome map
   diff-run     — run both, compare, return corpus record

   Outcome map:
     {:stdout \"...\"   ; captured stdout, whitespace-normalised
      :stderr \"...\"   ; captured stderr
      :exit   0}      ; process exit code (clojure side uses :ok / :error / :timeout)

   Corpus record:
     {:src        \"...snobol4 source...\"
      :spitbol    outcome-map
      :clojure    outcome-map
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
      {:stdout (normalise (:out result))
       :stderr (normalise (:err result))
       :exit   (:exit result)})
    (catch Exception e
      {:stdout "" :stderr (.getMessage e) :exit :crashed})))

;; ── SNOBOL4clojure side ───────────────────────────────────────────────────────
(defn run-clojure
  "Run src through SNOBOL4clojure. Returns {:stdout :stderr :exit :thrown}."
  [src]
  (let [out-buf  (StringBuilder.)
        err-buf  (StringBuilder.)
        test-ns  (create-ns (gensym "sno-harness-"))]
    (try
      ;; Each run gets a clean isolated namespace
      (env/GLOBALS test-ns)
      ;; Capture OUTPUT writes — patch OUTPUT$ write-watcher
      (binding [env/OUTPUT$ (atom "")]
        ;; Override the OUTPUT$ atom's watcher to accumulate lines
        (add-watch env/OUTPUT$ :capture
                   (fn [_ _ _ v]
                     (when (and v (not= v ""))
                       (.append out-buf v)
                       (.append out-buf "\n"))))
        (try
          (let [stlimit-prefix (str "&STLIMIT = " *stlimit* "\n")]
            (sno/RUN (sno/CODE (str stlimit-prefix src))))
          (finally
            (remove-watch env/OUTPUT$ :capture))))
      {:stdout (normalise (str out-buf))
       :stderr (normalise (str err-buf))
       :exit   :ok}
      (catch clojure.lang.ExceptionInfo e
        (let [sig (get (ex-data e) :snobol/signal)]
          (if (= sig :end)
            {:stdout (normalise (str out-buf)) :stderr "" :exit :ok}
            {:stdout (normalise (str out-buf))
             :stderr (.getMessage e)
             :exit   :error
             :thrown (str (class e) ": " (.getMessage e))})))
      (catch Exception e
        {:stdout (normalise (str out-buf))
         :stderr (.getMessage e)
         :exit   :error
         :thrown (str (class e) ": " (.getMessage e))})
      (finally
        (remove-ns (ns-name test-ns))))))

;; ── Status classification ─────────────────────────────────────────────────────
(defn- classify
  "Compare spitbol and clojure outcomes; return status keyword."
  [sp cl]
  (cond
    ;; Either side timed out (step limit hit) — record but don't fail
    (or (= (:exit sp) :timeout) (= (:exit cl) :timeout))
    :timeout

    ;; SPITBOL itself crashed (bad input to oracle) — discard
    (= (:exit sp) :crashed)
    :skip

    ;; Both produced identical stdout — pass
    (= (:stdout sp) (:stdout cl))
    :pass

    ;; Both errored (non-zero/error exit) — pass at class level
    (and (not= (:exit sp) 0) (= (:exit cl) :error))
    :pass-class

    ;; Otherwise — genuine divergence
    :else
    :fail))

;; ── Main entry point ──────────────────────────────────────────────────────────
(defn diff-run
  "Run src through both sides. Returns a corpus record."
  ([src] (diff-run src nil))
  ([src depth]
   (let [sp     (run-spitbol src)
         cl     (run-clojure src)
         status (classify sp cl)]
     {:src     src
      :spitbol sp
      :clojure cl
      :status  status
      :length  (count src)
      :depth   depth})))

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
