(ns SNOBOL4clojure.compiler
  "CODE! / CODE: compile SNOBOL4 source text into a labeled statement table
   and load it into the runtime's global statement store.

   ## Stage 23A — Pre-compiled EDN cache

   The IR produced by CODE! is pure EDN: persistent maps, lists, keywords,
   primitives.  Serialising with pr-str and reloading with edn/read-string
   is lossless — the grammar and instaparse pipeline never run again.

   Benchmark (session 13): CODE! costs ~10.93ms per program — 95% of the
   total CODE+RUN cost of ~11.45ms for short programs.  Caching eliminates
   this entirely for repeated runs of the same source.

   ### Disk cache API
     (compile-to-file src path)   — compile src, write EDN to path
     (load-ir path)               — load [codes nos labels] from EDN file
     (CODE-ir ir)                 — like CODE but takes pre-compiled IR triple
     (CODE-cached src path)       — load from path if exists, else compile+save

   ### In-memory (memoised) cache API — fastest for test suites
     (CODE-memo src)              — compile once per unique source string
     (clear-memo!)                — evict all entries
     (memo-stats)                 — {:size N :hits N :misses N}

   ### Low-level serialisation
     (ir->edn ir)                 — [codes nos labels] -> EDN string
     (edn->ir s)                  — EDN string -> [codes nos labels]"
  (:require [clojure.string          :as string]
            [clojure.edn             :as edn]
            [SNOBOL4clojure.env      :refer [STNO <STNO> <LABL> <CODE>]]
            [SNOBOL4clojure.grammar  :refer [parse-statement]]
            [SNOBOL4clojure.emitter  :refer [emitter]]))

;; -- Source tokenizer ---------------------------------------------------------
(defn- comment? [cmd] (re-find #"^\*" cmd))
(defn- control? [cmd] (re-find #"^\-" cmd))
(defn- error-node [info] (list 'ERROR (:line info) (:column info) (:text info)))

(defn- split-source
  "Split SNOBOL4 source into raw statement strings."
  [src]
  (let [chars (str src "\n")
        n     (count chars)]
    (loop [i 0 cur (StringBuilder.) stmts [] in-sq false in-dq false]
      (if (>= i n)
        (let [s (string/trimr (str cur))]
          (if (seq s) (conj stmts s) stmts))
        (let [ch (.charAt chars i)]
          (cond
            (and (= ch \') (not in-dq))
            (recur (inc i) (.append cur ch) stmts (not in-sq) in-dq)
            (and (= ch \") (not in-sq))
            (recur (inc i) (.append cur ch) stmts in-sq (not in-dq))
            (and (= ch \;) (not in-sq) (not in-dq))
            (let [s (string/trimr (str cur))]
              (recur (inc i) (StringBuilder.) (if (seq s) (conj stmts s) stmts) false false))
            (= ch \newline)
            (let [s (string/trimr (str cur))]
              (if (seq s)
                (let [next-i (inc i)
                      next-ch (when (< next-i n) (.charAt chars next-i))]
                  (if (and next-ch (or (= next-ch \+) (= next-ch \.)))
                    (recur (+ i 2) (.append cur \space) stmts false false)
                    (recur (inc i) (StringBuilder.) (conj stmts s) false false)))
                (recur (inc i) (StringBuilder.) stmts false false)))
            :else
            (recur (inc i) (.append cur ch) stmts in-sq in-dq)))))))

;; -- CODE! — parse source into {CODES NOS LABELS} ----------------------------
(defn CODE! [S]
  (let [raw-stmts (split-source (str S))]
    (loop [stmts raw-stmts NO 1 CODES {} NOS {} LABELS {}]
      (if (empty? stmts)
        [CODES NOS LABELS]
        (let [command (first stmts)]
          (cond
            (comment? command)(recur (rest stmts) NO CODES NOS LABELS)
            (control? command)(recur (rest stmts) NO CODES NOS LABELS)
            :else
            (let [stmt   (string/replace command #"\r" "")
                  ast    (parse-statement stmt)
                  code   (emitter ast)]
              (if (and (map? code) (:reason code))
                (recur (rest stmts) (inc NO) (assoc CODES NO (error-node code)) NOS LABELS)
                (let [label  (:label code)
                      body   (:body  code)
                      goto   (:goto  code)
                      key    (if label label NO)
                      code   (reduce #(conj %1 %2) [] [body goto])
                      nos    (if (keyword? key) (assoc NOS   key NO) NOS)
                      labels (if (keyword? key) (assoc LABELS NO key) LABELS)
                      codes  (assoc CODES key code)]
                  (recur (rest stmts) (inc NO) codes nos labels))))))))))

;; -- CODE — compile and load into the global statement store ------------------
(defn CODE [S]
  (let [[codes nos labels] (CODE! S)
        start              (inc @STNO)]
    (loop [NO 1]
      (if (> NO (count codes))
        (if (and (@<LABL> start) (@<CODE> (@<LABL> start)))
          (@<LABL> start)
          (when (@<CODE> start) start))
        (do
          (swap! STNO inc)
          (if-let [label (labels NO)]
            (do
              (swap! <CODE> #(assoc % label (codes label)))
              (swap! <LABL> #(assoc % @STNO label))
              (swap! <STNO> #(assoc % label @STNO)))
            (swap! <CODE> #(assoc % @STNO (codes NO))))
          (recur (inc NO)))))))

;; -- Stage 23A: EDN serialisation --------------------------------------------

(defn ir->edn
  "Serialise a CODE! result triple [codes nos labels] to a compact EDN string.
   The IR is pure Clojure data so pr-str is lossless."
  [[codes nos labels]]
  (pr-str {:codes codes :nos nos :labels labels}))

(defn edn->ir
  "Deserialise an EDN string produced by ir->edn back to [codes nos labels].
   Uses clojure.edn/read-string — safe, no eval."
  [s]
  (let [{:keys [codes nos labels]} (edn/read-string s)]
    [codes nos labels]))

;; -- Stage 23A: CODE-ir — load pre-compiled IR into statement store -----------

(defn CODE-ir
  "Like CODE, but takes a pre-compiled [codes nos labels] triple.
   Skips grammar and emitter entirely. ~10x faster than CODE for short programs."
  [[codes nos labels]]
  (let [start (inc @STNO)]
    (loop [NO 1]
      (if (> NO (count codes))
        (if (and (@<LABL> start) (@<CODE> (@<LABL> start)))
          (@<LABL> start)
          (when (@<CODE> start) start))
        (do
          (swap! STNO inc)
          (if-let [label (labels NO)]
            (do
              (swap! <CODE> #(assoc % label (codes label)))
              (swap! <LABL> #(assoc % @STNO label))
              (swap! <STNO> #(assoc % label @STNO)))
            (swap! <CODE> #(assoc % @STNO (codes NO))))
          (recur (inc NO)))))))

;; -- Stage 23A: Disk cache ---------------------------------------------------

(defn compile-to-file
  "Compile SNOBOL4 source src and write the IR as EDN to path.
   Returns the [codes nos labels] triple."
  [src path]
  (let [ir (CODE! src)]
    (clojure.java.io/make-parents path)
    (spit path (ir->edn ir))
    ir))

(defn load-ir
  "Load a pre-compiled [codes nos labels] triple from an EDN file at path."
  [path]
  (edn->ir (slurp path)))

(defn CODE-cached
  "Compile src and load into the statement store, using a disk EDN cache.
   If path exists: load from disk (skip grammar + emitter).
   If path absent: compile src, save to path, then load."
  [src path]
  (let [ir (if (.exists (clojure.java.io/file path))
              (load-ir path)
              (compile-to-file src path))]
    (CODE-ir ir)))

;; -- Stage 23A: In-memory memoised cache -------------------------------------
;;
;; Fastest for test suites: compile each unique source string once per JVM
;; session.  The lein test suite compiles the same programs on every run;
;; memoisation skips the grammar after the first pass.

(def ^:private memo-cache
  (atom {:cache {} :hits 0 :misses 0}))

(defn CODE-memo
  "Memoised CODE: compile src exactly once per JVM session, then reuse the IR.
   Thread-safe. Drop-in replacement for CODE in test fixtures and harness.

   Side effects (loading into <CODE>/<STNO>/<LABL>) still happen on every call
   so that reset-runtime! between tests works correctly."
  [src]
  (let [cached (get-in @memo-cache [:cache src])]
    (if cached
      (do (swap! memo-cache update :hits inc)
          (CODE-ir cached))
      (do (swap! memo-cache update :misses inc)
          (let [ir (CODE! src)]
            (swap! memo-cache update :cache assoc src ir)
            (CODE-ir ir))))))

(defn clear-memo!
  "Evict all entries from the in-memory IR cache."
  []
  (swap! memo-cache assoc :cache {} :hits 0 :misses 0)
  nil)

(defn memo-stats
  "Return cache statistics: {:size N :hits N :misses N :hit-rate pct}."
  []
  (let [{:keys [cache hits misses]} @memo-cache
        total (+ hits misses)]
    {:size     (count cache)
     :hits     hits
     :misses   misses
     :hit-rate (if (pos? total)
                 (format "%.1f%%" (* 100.0 (/ hits total)))
                 "n/a")}))

;; -- Stage 23A: Corpus pre-compiler ------------------------------------------

(defn precompile-corpus!
  "Pre-compile a sequence of source strings to EDN files under cache-dir.
   Each file is named by the hash of the source string.
   Skips files that already exist (idempotent).
   Returns {:compiled N :skipped N :errors N}.

   Example:
     (require '[SNOBOL4clojure.generator :as gen])
     (precompile-corpus! (gen/gen-by-length) \"resources/ir-cache\")"
  [sources cache-dir]
  (clojure.java.io/make-parents (str cache-dir "/x"))
  (reduce
    (fn [acc src]
      (let [path   (str cache-dir "/" (format "%x" (Math/abs (hash src))) ".ir.edn")
            exists (.exists (clojure.java.io/file path))]
        (if exists
          (update acc :skipped inc)
          (try
            (compile-to-file src path)
            (update acc :compiled inc)
            (catch Exception _
              (update acc :errors inc))))))
    {:compiled 0 :skipped 0 :errors 0}
    sources))
