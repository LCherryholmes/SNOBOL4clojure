(ns SNOBOL4clojure.env
  ;; Global SNOBOL4 environment: keywords, constants, data types,
  ;; utility arithmetic aliases, and the DATATYPE dispatch table.
  ;; No dependencies on other SNOBOL4clojure namespaces.
  (:require [clojure.pprint :as pp])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Utility arithmetic (avoid shadowing clojure.core in this ns) ─────────────
(defn Σ+       ([x y] (clojure.core/+ x y)) ([x] (clojure.core/+ x)))
(defn subtract ([x y] (clojure.core/- x y)) ([x] (clojure.core/- x)))
(defn multiply ([x y] (clojure.core/* x y)) ([x] (clojure.core/* x)))
(defn divide    [x y] (clojure.core// x y))
(defn equal     [x y] (clojure.core/= x y))
(defn not-equal [x y] (clojure.core/not= x y))
(defn out      [item] (binding [pp/*print-right-margin* 2048, pp/*print-miser-width* 2000]
                        (pp/pprint item)) item)

;; ── SNOBOL4 constants ─────────────────────────────────────────────────────────
(def  ε          "")        ; null / empty string
(def  η          ##NaN)     ; undefined numeric

;; ── SNOBOL4 keywords (& variables) ───────────────────────────────────────────
(def  &ALPHABET  (atom (apply vector (map #(char %) (range 256)))))
(def  &ANCHOR    (atom 0))
(def  &DIGITS    "0123456789")
(def  &DUMP      (atom 0))  ; 1, 2, and 3 levels
(def  &ERRLIMIT  (atom 0))
(def  &ERRTEXT   (atom ε))
(def  &ERRTYPE   (atom 0))
(def  &FTRACE    (atom 0))
(def  &FULLLSCAN (atom 0))
(def  &LASTNO    (atom 0))
(def  &LCASE     "abcdefghijklmnopqrstuvwxyz")
(def  &MAXLNGTH  (atom 4194304))
(def  &PROFILE   (atom 0))
(def  &TRACE     (atom 0))
(def  &TRIM      (atom 0))
(def  &STCOUNT   (atom 0))
(def  &STLIMIT   (atom 2147483647))
(def  &UCASE     "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

;; ── I/O channel atoms ────────────────────────────────────────────────────────
(def  INPUT$     (atom ε))
(def  OUTPUT$    (atom ε))
(def  TERMINAL$  (atom ε))

;; ── Runtime statement table (used by compiler + runtime) ─────────────────────
(def  STNO   (atom 0))
(def <STNO>  (atom {}))
(def <LABL>  (atom {}))
(def <CODE>  (atom {}))

;; ── NAME type: mutable named reference (SNOBOL4 . operator) ──────────────────
(definterface &NAME (n []) (n [_]))
(deftype NAME [^:unsynchronized-mutable n] &NAME
  (n [this]  n)
  (n [this _] (set! n _)))

;; ── Numeric conversion ────────────────────────────────────────────────────────
(defn num [x]
  (cond
    (double? x)  x
    (integer? x) (.doubleValue x)
    true (try (Double/parseDouble x)
           (catch NumberFormatException _ ##NaN))))

(defn  ncvt [x] (list 'num x))
(defn  scvt [x] (list 'str x))

;; ── Namespace variable lookup ($$ / reference) ───────────────────────────────
(defn reference [N]
  (if-let [ns-name (namespace N)]
    (when-let [ns-ref (or (get (ns-aliases *ns*) (symbol ns-name))
                          (find-ns (symbol ns-name)))]
      (get (ns-publics ns-ref) (symbol (name N))))
    (let [sym (symbol (name N))]
      ;; Search in priority order: current ns, core, primitives, operators,
      ;; then all loaded SNOBOL4clojure namespaces (covers test namespaces etc.)
      (or (get (ns-map *ns*) sym)
          (when-let [ns (find-ns 'SNOBOL4clojure.core)]       (get (ns-map ns) sym))
          (when-let [ns (find-ns 'SNOBOL4clojure.primitives)]  (get (ns-publics ns) sym))
          (when-let [ns (find-ns 'SNOBOL4clojure.operators)]   (get (ns-publics ns) sym))
          ;; Fall through: search all loaded SNOBOL4clojure namespaces
          (some (fn [ns]
                  (when (clojure.string/starts-with? (str (ns-name ns)) "SNOBOL4clojure")
                    (get (ns-map ns) sym)))
                (all-ns))))))

(defn $$ [N] (if-let [V (reference N)] (var-get V) ε))

;; ── Arrays and Tables ─────────────────────────────────────────────────────────
(defn ARRAY     [_proto] (object-array 10))
(defn TABLE     [] (hash-map))
(defn SET       [] (hash-set))

;; ── DATATYPE dispatch ─────────────────────────────────────────────────────────
;; Maps JVM class names to SNOBOL4 type names.
(defmulti  DATATYPE (fn [X] (str (class X))))
(defmethod DATATYPE "class java.lang.Character"                   [_] "STRING")
(defmethod DATATYPE "class java.lang.String"                      [_] "STRING")
(defmethod DATATYPE "class java.lang.Long"                        [_] "INTEGER")
(defmethod DATATYPE "class java.lang.Double"                      [_] "REAL")
(defmethod DATATYPE "class [Ljava.lang.Object;"                   [_] "ARRAY")
(defmethod DATATYPE "class [LLjava.lang.Object;"                  [_] "ARRAY")
(defmethod DATATYPE "class clojure.lang.PersistentArrayMap"       [_] "TABLE")
(defmethod DATATYPE "class clojure.lang.PersistentVector"         [_] "PATTERN")
(defmethod DATATYPE "class clojure.lang.Symbol"                   [_] "NAME")
(defmethod DATATYPE "class clojure.lang.PersistentList"           [_] "EXPRESSION")
(defmethod DATATYPE "class clojure.lang.PersistentList$EmptyList" [_] "EXPRESSION")
(defmethod DATATYPE "class clojure.lang.PersistentTreeMap"        [_] "CODE")
(defmethod DATATYPE "class clojure.lang.Keyword"                  [_] "CODE")
(defmethod DATATYPE "class clojure.lang.PersistentHashSet"        [_] "SET")
(defmethod DATATYPE "class clojure.lang.PersistentTreeSet"        [_] "SET")
(defmethod DATATYPE "class java.util.regex.Pattern"               [_] "REGEX")
(defmethod DATATYPE "class java.lang.Class"                       [_] "DATA")
(defmethod DATATYPE :default [X]
  (let [cn (str (class X))]
    (or
      ;; Custom SNOBOL4 data types defined via DATA
      (when-let [m (re-find #"class SNOBOL4clojure\.core\.(.*)" cn)]  (m 1))
      (when-let [m (re-find #"class SNOBOL4clojure\.env\.(.*)"  cn)]  (m 1))
      ;; NAME type specifically
      (when (re-find #"NAME" cn) "NAME"))))
