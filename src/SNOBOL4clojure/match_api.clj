(ns SNOBOL4clojure.match-api
  "Public API for SNOBOL4 pattern matching.
  Wraps the engine in engine.clj with user-facing entry points."
  (:require [SNOBOL4clojure.match    :refer [engine]]
            [SNOBOL4clojure.patterns :refer [POS RPOS]])
  (:refer-clojure :exclude [= + - * / num]))

(defn SEARCH
  "Slide pattern P across string S, trying each start position 0..n.
   Returns [start end] for the first match found, nil if none.
   (subs S start end) extracts the matched substring."
  [S P]
  (let [chars (seq S)
        n     (count S)]
    (loop [i 0]
      (when (<= i n)
        (let [result (engine (drop i chars) i P i S)]
          (if result
            result
            (recur (inc i))))))))

(defn MATCH
  "Match pattern P against string S anchored at position 0.
   Returns [start end] on success, nil on failure."
  [S P]
  (engine (seq S) 0 (list 'SEQ (POS 0) P) 0 S))

(defn FULLMATCH
  "Match pattern P against the entirety of string S (anchored both ends).
   Returns [0 (count S)] on success, nil on failure."
  [S P]
  (engine (seq S) 0 (list 'SEQ (POS 0) P (RPOS 0)) 0 S))

(defn REPLACE
  "Match pattern P against S; if matched, replace the matched span with R.
   Returns the new string on success, nil on failure."
  [S P R]
  (when-let [[start end] (SEARCH S P)]
    (str (subs S 0 start) R (subs S end))))

(defn COLLECT!
  "Exhaustively collect every match [start end] of pattern P across string S,
   including all multi-yield backtrack alternatives (e.g. BAL, ARB, ARBNO).
   Mirrors the SNOBOL4 idiom  P $ X FAIL.
   Returns a vector of [start end] pairs in scan order."
  [S P]
  (let [chars (seq S)
        n     (count S)
        bag   (atom [])
        node  (list 'COLLECT! bag P)]
    (doseq [i (range (inc n))]
      (engine (drop i chars) i node i S))
    @bag))
