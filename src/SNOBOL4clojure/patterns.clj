(ns SNOBOL4clojure.patterns
  ;; Pattern constructors: build pattern representation (lists/symbols)
  ;; that the MATCH engine knows how to dispatch on.
  (:require [SNOBOL4clojure.primitives :refer [charset]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Character-class patterns ──────────────────────────────────────────────────
(defn ANY    [S] (list 'ANY$    (charset S)))
(defn BREAK  [S] (list 'BREAK$  (charset S)))
(defn BREAKX [S] (list 'BREAKX$ (charset S)))
(defn NOTANY [S] (list 'NOTANY$ (charset S)))
(defn SPAN   [S] (list 'SPAN$   (charset S)))

;; ── Length / position patterns ────────────────────────────────────────────────
(defn LEN  [I] (list 'LEN#  I))
(defn POS  [I] (list 'POS#  I))
(defn RPOS [I] (list 'RPOS# I))
(defn RTAB [I] (list 'RTAB# I))
(defn TAB  [I] (list 'TAB#  I))

;; ── Repetition / structural patterns ─────────────────────────────────────────
(defn ARBNO [P]  (list 'ARBNO! P))
(defn FENCE
  ([]  (list 'FENCE!))
  ([P] (list 'FENCE! P)))

;; ── Constant pattern values ───────────────────────────────────────────────────
(def ARB     (list 'ARB!))
(def BAL     (list 'BAL!))
(def REM     (list 'REM!))
(def ABORT   (list 'ABORT!))
(def FAIL    (list 'FAIL!))
(def SUCCEED (list 'SUCCEED!))
