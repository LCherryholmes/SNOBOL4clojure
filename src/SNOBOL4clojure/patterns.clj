(ns SNOBOL4clojure.patterns
  ;; Pattern constructors: build pattern representation (lists/symbols)
  ;; that the MATCH engine knows how to dispatch on.
  (:require [SNOBOL4clojure.primitives :refer [charset]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Character-class patterns ──────────────────────────────────────────────────
(defn ANY    [S] (list 'ANY$    (charset S)))
(defn BREAK  [S] (list 'BREAK$  (charset S)))
(defn BREAKX [S] (list 'BREAKX# (charset S)))   ; backtracking form
(defn NOTANY [S] (list 'NOTANY$ (charset S)))
(defn NSPAN  [S] (list 'NSPAN$  (charset S)))    ; 0-or-more span
(defn SPAN   [S] (list 'SPAN$   (charset S)))

;; ── Length / position patterns ────────────────────────────────────────────────
(defn LEN  [I] (list 'LEN#  I))
(defn POS  [I] (list 'POS#  I))
(defn RPOS [I] (list 'RPOS# I))
(defn RTAB [I] (list 'RTAB# I))
(defn TAB  [I] (list 'TAB#  I))

;; ── Zero-width anchor patterns ────────────────────────────────────────────────
(def BOL (list 'BOL#))    ; beginning of subject (position 0)
(def EOL (list 'EOL#))    ; end of subject (no chars remaining)

;; ── Repetition / structural patterns ─────────────────────────────────────────
(defn ARBNO [P]  (list 'ARBNO! P))
(defn FENCE
  ([]  (list 'FENCE!))
  ([P] (list 'FENCE! P)))

;; ── Cursor assignment ─────────────────────────────────────────────────────────
(defn CURSOR [N] (list 'CURSOR-IMM! N))  ; @N — assign cursor position to N

;; ── Conjunction ───────────────────────────────────────────────────────────────
(defn CONJ [P Q] (list 'CONJ! P Q))     ; P & Q — both must match same span

;; ── Deferred pattern ──────────────────────────────────────────────────────────
(defn DEFER [thunk] (list 'DEFER! thunk)) ; *expr — evaluate thunk at match time

;; ── Constant pattern values ───────────────────────────────────────────────────
(def ARB     (list 'ARB!))
(def BAL     (list 'BAL!))
(def REM     (list 'REM!))
(def ABORT   (list 'ABORT!))
(def FAIL    (list 'FAIL!))
(def SUCCEED (list 'SUCCEED!))
