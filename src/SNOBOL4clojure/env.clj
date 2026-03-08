(ns SNOBOL4clojure.env
  ;; Global SNOBOL4 environment: keywords, constants, data types,
  ;; utility arithmetic aliases, and the DATATYPE dispatch table.
  ;; No dependencies on other SNOBOL4clojure namespaces.
  (:require [clojure.pprint  :as pp])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Utility arithmetic (avoid shadowing clojure.core in this ns) ─────────────
(defn Σ+       ([x y] (clojure.core/+' x y)) ([x] (clojure.core/+' x)))
(defn subtract ([x y] (clojure.core/-' x y)) ([x] (clojure.core/-' x)))
(defn multiply ([x y] (clojure.core/*' x y)) ([x] (clojure.core/*' x)))
(defn divide    [x y] (when-not (zero? y) (clojure.core// x y)))
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
    (nil? x)   ##NaN
    (double? x)  x
    (integer? x) (.doubleValue x)
    true (try (Double/parseDouble (str x))
           (catch NumberFormatException _ ##NaN))))

(defn  ncvt [x] (list 'num x))
(defn  scvt [x] (list 'str x))

;; ── SNOBOL4 global namespace — set once by GLOBALS, used everywhere ───────────
;; This is the direct analog of _env._g in SNOBOL4python: one authoritative
;; namespace where all user SNOBOL4 variables live.  The runtime never searches
;; multiple namespaces; it always interns into and resolves from this one ns.
(def ^:private snobol-ns (atom nil))

(defn GLOBALS
  "Point the SNOBOL4 runtime at the user's namespace.
   Call once at the top of every user script:  (GLOBALS *ns*)
   Mirrors GLOBALS(globals()) in SNOBOL4python."
  [ns]
  (reset! snobol-ns ns))

(defn- active-ns
  "Return the authoritative SNOBOL4 namespace.
   Falls back to SNOBOL4clojure.env if GLOBALS has not been called."
  []
  (or @snobol-ns (find-ns 'SNOBOL4clojure.env)))

;; ── Variable write / read ─────────────────────────────────────────────────────
(defn snobol-set! [sym val]
  (intern (active-ns) sym val))

(defn reference [N]
  (ns-resolve (active-ns) (symbol (name N))))

(defn $$ [N]
  (if (clojure.core/= (symbol (name N)) 'INPUT)
    (let [line (try (read-line) (catch Exception _ nil))]
      (if (nil? line) ε (str line)))
    (if-let [V (reference N)] (var-get V) ε)))

;; ── Arrays and Tables ─────────────────────────────────────────────────────────

;; SnobolArray: multi-dimensional, integer-subscripted, bounds-checked.
;; dims  = vector of [lo hi] pairs, one per dimension
;; dflt  = default value for unset slots
;; data  = atom wrapping a map from index-tuple (vector of ints) → value
(defrecord SnobolArray [dims dflt data])

(defn- parse-array-proto
  "Parse a SNOBOL4 array prototype string into a vector of [lo hi] pairs.
   Accepts integer N (meaning [1,N]) or string '[-]m:n,...'.
   Returns nil on any parse error."
  [proto]
  (let [s (str proto)]
    (try
      (let [parts (clojure.string/split s #",")]
        (mapv (fn [part]
                (let [part (clojure.string/trim part)]
                  (if (re-find #":" part)
                    (let [[lo hi] (clojure.string/split part #":" 2)
                          lo (Long/parseLong (clojure.string/trim lo))
                          hi (Long/parseLong (clojure.string/trim hi))]
                      (when (> lo hi) (throw (ex-info "lo>hi" {})))
                      [lo hi])
                    (let [n (Long/parseLong part)]
                      (when (<= n 0) (throw (ex-info "size<=0" {})))
                      [1 n]))))
              parts))
      (catch Exception _ nil))))

(defn ARRAY
  "ARRAY(proto) or ARRAY(proto, default).
   proto is an integer N → dimension [1..N], or a string like '-5:10,3:5,20'.
   Returns a SnobolArray, or throws on invalid proto."
  ([proto] (ARRAY proto ε))
  ([proto dflt]
   (let [dims (parse-array-proto proto)]
     (when (nil? dims) (throw (ex-info "ARRAY: invalid prototype" {:proto proto})))
     (->SnobolArray dims dflt (atom {})))))

(defn TABLE     ([]      (atom {}))
                ([_n]    (atom {}))
                ([_n _d] (atom {})))
(defn SET       [] (hash-set))

(defn table?    [x] (and (instance? clojure.lang.IDeref x)
                         (map? @x)))
(defn table-get [t k]        (get @t k ε))
(defn table-set [t k v]      (swap! t assoc k v) v)

(defn array?    [x] (instance? SnobolArray x))

(defn- array-check-bounds
  "Returns nil (failure) if idx-vec is out of bounds, else the normalised key."
  [^SnobolArray arr idx-vec]
  (let [dims (:dims arr)]
    (when (clojure.core/= (count idx-vec) (count dims))
      (when (every? true?
                    (map (fn [[lo hi] i]
                           (and (integer? i) (>= i lo) (<= i hi)))
                         dims idx-vec))
        idx-vec))))

(defn array-get
  "Read arr at idx-vec (vector of ints). Returns nil on bounds failure."
  [^SnobolArray arr idx-vec]
  (when-let [k (array-check-bounds arr idx-vec)]
    (get @(:data arr) k (:dflt arr))))

(defn array-set
  "Write arr at idx-vec. Returns nil on bounds failure, value on success."
  [^SnobolArray arr idx-vec v]
  (when-let [k (array-check-bounds arr idx-vec)]
    (swap! (:data arr) assoc k v)
    v))

(defn array-prototype
  "Return the PROTOTYPE string for a SnobolArray."
  [^SnobolArray arr]
  (clojure.string/join "," (map (fn [[lo hi]] (str lo ":" hi)) (:dims arr))))


;; ── SNOBOL4 control-flow signals ────────────────────────────────────────────
;; Used to unwind the call stack from :(RETURN), :(FRETURN), :(NRETURN), :(END)
;; These are ex-info maps thrown as exceptions and caught by DEFINE's wrapper.
(defn snobol-return!  [] (throw (ex-info "RETURN"  {:snobol/signal :return})))
(defn snobol-freturn! [] (throw (ex-info "FRETURN" {:snobol/signal :freturn})))
(defn snobol-nreturn! [] (throw (ex-info "NRETURN" {:snobol/signal :nreturn})))
(defn snobol-end!     [] (throw (ex-info "END"     {:snobol/signal :end})))
;; Maps JVM class names to SNOBOL4 type names.
(defmulti  DATATYPE (fn [X] (str (class X))))
(defmethod DATATYPE "class java.lang.Character"                   [_] "STRING")
(defmethod DATATYPE "class java.lang.String"                      [_] "STRING")
(defmethod DATATYPE "class java.lang.Long"                        [_] "INTEGER")
(defmethod DATATYPE "class java.lang.Double"                      [_] "REAL")
(defmethod DATATYPE "class [Ljava.lang.Object;"                   [_] "ARRAY")
(defmethod DATATYPE "class [LLjava.lang.Object;"                  [_] "ARRAY")
(defmethod DATATYPE "class SNOBOL4clojure.env.SnobolArray"        [_] "ARRAY")
(defmethod DATATYPE "class clojure.lang.Atom"                     [_] "TABLE")
(defmethod DATATYPE "class clojure.lang.PersistentArrayMap"       [_] "TABLE")
(defmethod DATATYPE "class clojure.lang.PersistentHashMap"        [_] "TABLE")
(defmethod DATATYPE "class clojure.lang.PersistentVector"         [_] "PATTERN")
(defmethod DATATYPE "class clojure.lang.Symbol"                   [_] "NAME")
(defmethod DATATYPE "class clojure.lang.PersistentList"           [X]
  (if ('#{SEQ ALT CAPTURE
          ANY$ NOTANY$ SPAN$ BREAK$ BREAKX$
          LEN# POS# RPOS# TAB# RTAB#
          FENCE! ARBNO! ARB! BAL! ABORT!
          SUCCEED! FAIL! X} (first X))
    "PATTERN"
    "EXPRESSION"))
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
      ;; Program-defined data types (map with :__type__ key)
      (when (and (map? X) (:__type__ X)) (:__type__ X))
      ;; Custom SNOBOL4 data types defined via DATA (legacy deftype path)
      (when-let [m (re-find #"class SNOBOL4clojure\.core\.(.*)" cn)]  (m 1))
      (when-let [m (re-find #"class SNOBOL4clojure\.env\.(.*)"  cn)]  (m 1))
      ;; NAME type specifically
      (when (re-find #"NAME" cn) "NAME"))))
