(ns SNOBOL4clojure.functions
  ;; Built-in SNOBOL4 functions.
  ;; Stubs are marked clearly; implemented ones are complete.
  (:require [SNOBOL4clojure.env :refer [ε equal out subtract
                                        DATATYPE table? array? table-get table-set
                                        ARRAY ->SnobolArray array-get array-set]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── String functions ──────────────────────────────────────────────────────────
(defn SIZE    [s]         (count s))
(defn REPLACE [s1 s2 s3]
  ;; Replace chars in s1: each char of s2 mapped to corresponding char of s3
  (apply str (map #(let [i (.indexOf ^String s2 (str %))]
                     (if (>= i 0) (.charAt ^String s3 i) %))
                  s1)))
(defn DUPL    [x i]   (apply str (repeat i x)))  ; string only for now
(defn TRIM    [s]     (clojure.string/trim s))
(defn REVERSE [s]     (apply str (clojure.string/reverse s)))
(defn LPAD    [s n]   (clojure.string/join (concat (repeat (subtract n (count s)) \space) [s])))
(defn RPAD    [s n]   (clojure.string/join (concat [s] (repeat (subtract n (count s)) \space))))
(defn SUBSTR  [s i j] (subs s i j))
(defn CONVERT [x t]
  "CONVERT(x, type) — coerce x to the named SNOBOL4 type.
   Returns nil (failure) if the conversion is not defined."
  (let [from (clojure.string/upper-case (DATATYPE x))
        to   (clojure.string/upper-case (str t))]
    (cond
      ;; Same type → identity (but always a 'new' value logically)
      (clojure.core/= from to) x

      ;; STRING conversions
      (and (clojure.core/= from "STRING") (clojure.core/= to "INTEGER"))
      (let [v (try (Long/parseLong (clojure.string/trim (str x)))
                   (catch Exception _ nil))]
        v)   ; nil propagates as failure

      (and (clojure.core/= from "STRING") (clojure.core/= to "REAL"))
      (let [v (try (Double/parseDouble (clojure.string/trim (str x)))
                   (catch Exception _ nil))]
        v)

      (and (clojure.core/= from "STRING") (clojure.core/= to "PATTERN"))
      ;; A string converts to a literal LIT$ pattern node
      (list 'SEQ (list 'LIT$ (str x)))

      (and (clojure.core/= from "STRING") (clojure.core/= to "NAME"))
      ;; A string becomes a NAME (symbol pointing to a variable of that name)
      (symbol (str x))

      ;; INTEGER conversions
      (and (clojure.core/= from "INTEGER") (clojure.core/= to "STRING"))
      (str x)

      (and (clojure.core/= from "INTEGER") (clojure.core/= to "REAL"))
      (double x)

      (and (clojure.core/= from "INTEGER") (clojure.core/= to "PATTERN"))
      (list 'SEQ (list 'LIT$ (str x)))

      (and (clojure.core/= from "INTEGER") (clojure.core/= to "NAME"))
      (symbol (str x))

      ;; REAL conversions
      (and (clojure.core/= from "REAL") (clojure.core/= to "STRING"))
      (str x)

      (and (clojure.core/= from "REAL") (clojure.core/= to "INTEGER"))
      ;; Fails if value doesn't fit in a long
      (let [v (try (let [lv (long x)]
                     (when (clojure.core/<= (double Long/MIN_VALUE) x (double Long/MAX_VALUE))
                       lv))
                   (catch Exception _ nil))]
        v)

      (and (clojure.core/= from "REAL") (clojure.core/= to "PATTERN"))
      (list 'SEQ (list 'LIT$ (str x)))

      (and (clojure.core/= from "REAL") (clojure.core/= to "NAME"))
      (symbol (str x))

      ;; ARRAY conversions
      (and (clojure.core/= from "ARRAY") (clojure.core/= to "TABLE"))
      ;; ARRAY must be Nx2; column 1 = key, column 2 = value.
      ;; Only succeeds if exactly 2 dimensions and second dim size is 2.
      (let [dims (:dims x)]
        (if (and (clojure.core/= (count dims) 2)
                 (clojure.core/= (second (second dims)) 2)
                 (clojure.core/= (first (second dims)) 1))
          (let [t (atom {})
                [lo1 hi1] (first dims)]
            (doseq [i (range lo1 (inc hi1))]
              (let [k (array-get x [i 1])
                    v (array-get x [i 2])]
                (when-not (nil? k)
                  (swap! t assoc k (or v ε)))))
            t)
          nil))  ; wrong shape → fail

      ;; TABLE conversions
      (and (clojure.core/= from "TABLE") (clojure.core/= to "ARRAY"))
      ;; Convert TABLE to Nx2 ARRAY sorted by key string representation
      (let [entries (sort-by #(str (first %)) (seq @x))
            n       (count entries)
            arr     (ARRAY (str n ",2"))]
        (doseq [[i [k v]] (map-indexed vector entries)]
          (array-set arr [(inc i) 1] k)
          (array-set arr [(inc i) 2] v))
        arr)

      ;; PATTERN stays PATTERN
      (and (clojure.core/= from "PATTERN") (clojure.core/= to "PATTERN")) x

      ;; NAME conversions (NAME is a symbol in our representation)
      (and (clojure.core/= from "NAME") (clojure.core/= to "STRING"))
      (str x)

      (and (clojure.core/= from "NAME") (clojure.core/= to "INTEGER"))
      (let [v (try (Long/parseLong (str x)) (catch Exception _ nil))] v)

      (and (clojure.core/= from "NAME") (clojure.core/= to "REAL"))
      (let [v (try (Double/parseDouble (str x)) (catch Exception _ nil))] v)

      (and (clojure.core/= from "NAME") (clojure.core/= to "PATTERN"))
      (list 'SEQ (list 'LIT$ (str x)))

      (and (clojure.core/= from "NAME") (clojure.core/= to "NAME")) x

      ;; EXPRESSION stays EXPRESSION
      (and (clojure.core/= from "EXPRESSION") (clojure.core/= to "EXPRESSION")) x

      ;; CODE stays CODE
      (and (clojure.core/= from "CODE") (clojure.core/= to "CODE")) x

      ;; All other combinations fail
      :else nil)))
(defn COPY    [x]
  (cond
    (table? x) (atom @x)
    (array? x) (SNOBOL4clojure.env/->SnobolArray
                  (:dims x) (:dflt x) (atom @(:data x)))
    :else x))

;; ── Type conversion & char functions ─────────────────────────────────────────
(defn ASCII   [s]  (int (first (str s))))
(defn CHAR    [n]  (str (char n)))
(defn REMDR   [x y] (clojure.core/rem (long x) (long y)))

(defn INTEGER [x]
  "Convert x to integer, or return ε (fail) if not convertible."
  (cond
    (integer? x) x
    (number?  x) (long x)
    :else (try (Long/parseLong (clojure.string/trim (str x)))
               (catch Exception _ ε))))

(defn REAL    [x]
  "Convert x to real (double), or return ε (fail) if not convertible."
  (cond
    (float? x)   x
    (number? x)  (double x)
    :else (try (Double/parseDouble (clojure.string/trim (str x)))
               (catch Exception _ ε))))

(defn STRING  [x]
  "Convert any value to its string representation."
  (str x))

;; ── Program-defined datatype ──────────────────────────────────────────────────
;; PDD instances are represented as Clojure maps:
;;   {:__type__ "COMPLEX", "REAL" 3.14, "IMAG" -2.0}
;; Constructor and accessor functions are stored as Clojure fns in the SNOBOL
;; namespace so INVOKE can find them via the normal ($$ op) fallthrough.

;; Global registry: type-name-str → vector of field name strings (upper-case)
(def data-type-registry (atom {}))

(defn- parse-data-spec
  "Parse 'NAME(F1,F2,...)' into [name-str [field-str ...]].
   NAME and fields may contain any chars except ( ) , whitespace."
  [S]
  (let [s (clojure.string/trim S)
        lp (.indexOf ^String s "(")
        rp (.lastIndexOf ^String s ")")]
    (when (and (> lp 0) (>= rp (inc lp)))
      (let [type-name  (.substring ^String s 0 lp)
            fields-str (.substring ^String s (inc lp) rp)
            fields     (if (clojure.string/blank? fields-str)
                         []
                         (mapv (comp clojure.string/trim)
                               (clojure.string/split fields-str #",")))]
        [type-name fields]))))

(defn DATA!
  "Parse and register a DATA type definition.
   Returns a do-form that installs constructor + accessor fns in the active ns."
  [S]
  (when-let [[type-name fields] (parse-data-spec S)]
    ;; Upcase for case-insensitive SNOBOL4 compatibility
    (let [tname  (clojure.string/upper-case type-name)
          fnames (mapv clojure.string/upper-case fields)]
      ;; Register in the global registry
      (swap! data-type-registry assoc tname fnames)
      ;; Build constructor: (TYPE f1 f2 ...) → {:__type__ "TYPE" "F1" f1 ...}
      ;; Build accessors:   (F inst) → val   and   (F inst new-val) → updated inst
      ;; We emit a form suitable for eval in the user namespace.
      (let [constructor-body
              `(fn [& args#]
                 (let [field-names# ~fnames
                       pairs# (map vector field-names# args#)]
                   (into {:__type__ ~tname} pairs#)))
            accessor-forms
              (for [f fnames]
                [f
                 `(fn
                    ([inst#]       (get inst# ~f ε))
                    ([inst# val#]  (assoc inst# ~f val#)))])]
        `(do
           (SNOBOL4clojure.env/snobol-set! '~(symbol tname) ~constructor-body)
           ~@(for [[f form] accessor-forms]
               `(SNOBOL4clojure.env/snobol-set! '~(symbol f) ~form)))))))

(defn DATA [S]
  (when-let [form (DATA! S)]
    (out form)
    (eval form)
    ε))

(defn FIELD
  "FIELD(type-name, n) — return the nth field name of the program-defined type.
   n is 1-based. Returns nil on out-of-range."
  [type-name n]
  (let [tname  (clojure.string/upper-case (str type-name))
        fields (get @data-type-registry tname)]
    (when (and fields (>= (long n) 1) (<= (long n) (count fields)))
      (nth fields (dec (long n))))))

;; ── Date / time ───────────────────────────────────────────────────────────────
(defn DATE   []    (.toString (java.util.Date.)))
(defn TIME   []    (System/currentTimeMillis))

;; ── I/O ───────────────────────────────────────────────────────────────────────
;; INPUT variable: reading it calls read-line on *in*.
;; Returns the next line (without newline), or ε at EOF.
(defn READ-LINE! []
  (let [line (try (read-line) (catch Exception _ nil))]
    (if (nil? line) ε (str line))))

(defn BACKSPACE [] ε)
(defn DETACH    [] ε)
(defn EJECT     [] ε)
(defn ENDFILE   [] ε)
(defn INPUT     [] (READ-LINE!))   ; bare INPUT() call reads one line
(defn OUTPUT    [] ε)
(defn REWIND    [] ε)

;; ── Memory stubs ──────────────────────────────────────────────────────────────
(defn CLEAR   [] ε)
(defn COLLECT [] ε)
(defn DUMP    [] ε)

;; ── Program control stubs ─────────────────────────────────────────────────────
(defn EXIT    [] nil)
(defn HOST    [] nil)
(defn SETEXIT [] nil)
(defn STOPTR  [] nil)
(defn TRACE   [] nil)

;; ── Function definition stubs ─────────────────────────────────────────────────
(defn APPLY
  "APPLY(fname, arg1, ...) — call a named function by string/symbol."
  [fname & fargs]
  (let [f-sym (symbol (str fname))
        f-fn  (SNOBOL4clojure.env/$$ f-sym)]
    (when (fn? f-fn)
      (apply f-fn fargs))))
(defn ARG    [] nil)
(defn DEFINE [] nil)
(defn LOAD   [] nil)
(defn LOCAL  [] nil)
(defn OPSYN  [] nil)
(defn UNLOAD [] nil)

;; ── Table / array functions ───────────────────────────────────────────────────
(defn ITEM
  "ITEM(table, key) — subscript read/lvalue for TABLE.
   Returns the value at key, or ε if absent."
  [t k]
  (SNOBOL4clojure.env/table-get t k))

(defn PROTOTYPE
  "PROTOTYPE(array-or-table) — return the dimension spec string.
   For TABLE returns ε.  For ARRAY returns normalized 'lo:hi,...' string."
  [x]
  (cond
    (SNOBOL4clojure.env/table? x) ""
    (SNOBOL4clojure.env/array? x) (SNOBOL4clojure.env/array-prototype x)
    :else ""))

(defn- table->sorted-array
  "Convert TABLE to Nx2 ARRAY sorted by value (string comparison).
   SORT sorts ascending, RSORT descending."
  [t comparator]
  (let [entries (sort-by #(str (second %)) comparator (seq @t))
        n       (count entries)
        arr     (ARRAY (str n ",2"))]
    (doseq [[i [k v]] (map-indexed vector entries)]
      (array-set arr [(inc i) 1] k)
      (array-set arr [(inc i) 2] v))
    arr))

(defn SORT  [A]
  (cond
    (table? A) (table->sorted-array A compare)
    (array? A) A   ; ARRAY sort not yet defined — return as-is
    :else nil))

(defn RSORT [A]
  (cond
    (table? A) (table->sorted-array A #(compare %2 %1))
    (array? A) A
    :else nil))
