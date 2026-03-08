(ns SNOBOL4clojure.functions
  ;; Built-in SNOBOL4 functions.
  ;; Stubs are marked clearly; implemented ones are complete.
  (:require [SNOBOL4clojure.env :refer [ε equal out subtract]])
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
(defn CONVERT [x _t]  x)   ; stub
(defn COPY    [x]     x)   ; stub

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
(def proto-data-name  #"^([A-Za-z][0-9-.A-Z_a-z]+)\((.*)$")
(def proto-data-field #"^([0-9-.A-Z_a-z]+)[,)](.*)$")

(defn proto-data [S]
  (let [[_ name rem] (re-find proto-data-name S)]
    (loop [rem rem fields []]
      (if (equal rem ε) [(symbol name) fields]
        (let [[_ field rem] (re-find proto-data-field rem)]
          (recur rem (conj fields (symbol field))))))))

(defn DATA! [S]
  (let [[name fields] (proto-data S)]
    (list 'do
      (apply list 'defprotocol (symbol (str \& name))
        (reduce #(conj %1 (list %2 ['this] ['this '_])) [] fields))
      (apply list 'deftype name
        (reduce #(conj %1 (with-meta %2 {:unsynchronized-mutable true})) [] fields)
        (symbol (str \& name))
        (reduce #(conj %1
                   (list %2 ['this] %2)
                   (list %2 ['this '_] (list 'set! %2 '_))) [] fields)))))

(defn DATA  [S] (let [data (DATA! S)] (binding [*print-meta* true] (out data)) (eval data) ε))
(defn FIELD [])

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
(defn APPLY  [] nil)
(defn ARG    [] nil)
(defn DEFINE [] nil)
(defn LOAD   [] nil)
(defn LOCAL  [] nil)
(defn OPSYN  [] nil)
(defn UNLOAD [] nil)

;; ── Table / array stubs ───────────────────────────────────────────────────────
(defn ITEM      [] nil)
(defn PROTOTYPE [] nil)
(defn SORT      [_A] nil)
(defn RSORT     [_A] nil)
