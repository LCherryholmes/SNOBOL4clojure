(ns SNOBOL4clojure.primitives
  ;; Low-level SNOBOL4 pattern scanners.
  ;; Each scanner takes [Σ Δ Π] — remaining subject chars, current position,
  ;; pattern argument — and returns [σ δ] on success or [σ (- -1 δ)] on failure.
  ;; No dependency on the MATCH engine; these are pure scanning functions.
  (:require [SNOBOL4clojure.env :refer [Σ+ equal not-equal $$]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Failure encoding ─────────────────────────────────────────────────────────
(defn err [Σ Δ] [Σ (clojure.core/- -1 Δ)])

;; ── Trivials ──────────────────────────────────────────────────────────────────
(defn SUCCEED! [Σ Δ _Π] [Σ Δ])
(defn FAIL!    [Σ Δ _Π] (err Σ Δ))
(defn ABORT!   [Σ Δ _Π] (err nil Δ))

;; ── Stub scanners (not yet implemented) ──────────────────────────────────────
(defn ARB!    [Σ Δ _Π] (err nil Δ))
(defn BAL!    [Σ Δ _Π] (err nil Δ))
(defn ARBNO!  [Σ Δ _Π] (err nil Δ))
(defn FENCE!  [Σ Δ _Π] (err nil Δ))
(defn FENCE!! [Σ Δ _Π] (err nil Δ))
(defn BREAKX$ [Σ Δ _Π] (err nil Δ))

;; ── Positional ────────────────────────────────────────────────────────────────
(defn POS#  [Σ Δ Π] (if (equal Δ Π)         [Σ Δ] (err Σ Δ)))
(defn RPOS# [Σ Δ Π] (if (equal (count Σ) Π) [Σ Δ] (err Σ Δ)))

;; ── Character class ───────────────────────────────────────────────────────────
(defn ANY$    [Σ Δ Π] (if (not (seq Σ))
                        (err Σ Δ)
                        (if     (contains? Π (first Σ)) [(rest Σ) (inc Δ)] (err Σ Δ))))
(defn NOTANY$ [Σ Δ Π] (if (not (seq Σ))
                        (err Σ Δ)
                        (if-not (contains? Π (first Σ)) [(rest Σ) (inc Δ)] (err Σ Δ))))

;; ── String matching ───────────────────────────────────────────────────────────
(defn REM! [Σ Δ _Π]
  (loop [σ Σ δ Δ]
    (if (not (seq σ)) [σ δ] (recur (rest σ) (inc δ)))))

(defn LIT$ [Σ Δ Π]
  (loop [σ Σ δ Δ π Π]
    (if (not (seq π)) [σ δ]
      (if (not (seq σ)) (err σ δ)
        (if (not-equal (first σ) (first π)) (err σ δ)
          (recur (rest σ) (inc δ) (rest π)))))))

;; ── Length and tab ────────────────────────────────────────────────────────────
(defn LEN# [Σ Δ Π]
  (loop [σ Σ δ Δ]
    (if (>= δ (Σ+ Δ Π)) [σ δ]
      (if (not (seq σ)) (err σ δ) (recur (rest σ) (inc δ))))))

(defn TAB# [Σ Δ Π]
  (loop [σ Σ δ Δ]
    (if (>= δ Π) [σ δ]
      (if (not (seq σ)) (err σ δ) (recur (rest σ) (inc δ))))))

(defn RTAB# [Σ Δ Π]
  (loop [σ Σ δ Δ]
    (if (>= (count σ) Π) [σ δ]
      (if (not (seq σ)) (err σ δ) (recur (rest σ) (inc δ))))))

;; ── Span and break ────────────────────────────────────────────────────────────
(defn SPAN$ [Σ Δ Π]
  (loop [σ Σ δ Δ]
    (if (not (contains? Π (first σ)))
      (if (not-equal δ Δ) [σ δ] (err σ δ))
      (recur (rest σ) (inc δ)))))

(defn BREAK$ [Σ Δ Π]
  (loop [σ Σ δ Δ]
    (if (not (seq σ)) (err σ δ)
      (if (contains? Π (first σ)) [σ δ]
        (recur (rest σ) (inc δ))))))

;; ── Charset helper ────────────────────────────────────────────────────────────
(defn charset [S] (reduce #(conj %1 %2) #{} S))
