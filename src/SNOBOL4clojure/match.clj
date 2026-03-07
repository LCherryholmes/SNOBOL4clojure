(ns SNOBOL4clojure.match
  ;; The SNOBOL4 pattern match engine.
  ;; An explicit iterative state machine over a 7-element frame vector:
  ;;   [Σ Δ σ δ Π φ Ψ]
  ;;   Σ/Δ — subject start + position on entry to this frame
  ;;   σ/δ — subject + position as updated by scanning
  ;;   Π   — current pattern node
  ;;   φ   — index into Π (for ALT/SEQ child iteration)
  ;;   Ψ   — parent frame stack
  ;; Actions: :proceed :succeed :recede :fail
  (:require [SNOBOL4clojure.env        :refer [ε equal out $$]]
            [SNOBOL4clojure.primitives :refer
             [LIT$ ANY$ NOTANY$ SPAN$ BREAK$ BREAKX$
              POS# RPOS# LEN# TAB# RTAB#
              SUCCEED! FAIL! ARB! BAL! ARBNO! ABORT! err]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Frame accessors ───────────────────────────────────────────────────────────
(defn ζΣ [ζ] (if ζ (ζ 0)))  ; subject start (seq of chars)
(defn ζΔ [ζ] (if ζ (ζ 1)))  ; position on entry
(defn ζσ [ζ] (if ζ (ζ 2)))  ; subject (current, seq of chars)
(defn ζδ [ζ] (if ζ (ζ 3)))  ; current position
(defn ζΠ [ζ] (if ζ (ζ 4)))  ; pattern node
(defn ζφ [ζ] (if ζ (ζ 5)))  ; child index (1-based)
(defn ζΨ [ζ] (if ζ (ζ 6)))  ; parent stack

(defn ζα [ζ] (<= (ζφ ζ) 1))
(defn ζω [ζ] (>= (ζφ ζ) (count (ζΠ ζ))))

(defn ζλ [ζ]   ; current operation symbol
  (cond
    (nil?        ζ)  nil
    (nil?    (ζΠ ζ)) nil
    (string? (ζΠ ζ)) 'LIT$
    (symbol? (ζΠ ζ)) (ζΠ ζ)
    (list?   (ζΠ ζ)) (first (ζΠ ζ))
    (seq?    (ζΠ ζ)) (first (ζΠ ζ))
    true     (out ["lamda? " (type (ζΠ ζ)) (ζΠ ζ)])))

;; ── Stack helpers ─────────────────────────────────────────────────────────────
(defn top  [Ψ]   (last Ψ))
(defn push [Ψ ζ] (if Ψ (conj Ψ ζ)))
(defn pull [Ψ]   (if Ψ (if-not (empty? Ψ) (pop Ψ))))
(defn 🡡 [Ω]     (top Ω))
(defn 🡥 [Ω ζ]   (push Ω ζ))
(defn 🡧 [Ω]     (pull Ω))

;; ── Frame transitions ─────────────────────────────────────────────────────────
(defn ζ↓
  ([ζ Π]  (let [[Σ Δ _ _ _ _ Ψ] ζ] [Σ Δ Σ Δ Π         1       Ψ]))   ; call over
  ([ζ]    (let [[Σ Δ _ _ Π φ Ψ] ζ] [Σ Δ Σ Δ (nth Π φ) 1       (🡥 Ψ ζ)]))) ; call down

(defn ζ↑
  ([ζ σ δ] (let [[Σ Δ _ _ _ _ Ψ] ζ] [Σ Δ σ δ (ζΠ (🡡 Ψ)) (ζφ (🡡 Ψ)) (🡧 Ψ)])) ; return up scan
  ([ζ]     (let [[Σ Δ σ δ _ _ Ψ] ζ] [Σ Δ σ δ (ζΠ (🡡 Ψ)) (ζφ (🡡 Ψ)) (🡧 Ψ)]))) ; return up result

(defn ζ→ [ζ] (let [[_ _ σ δ Π φ Ψ] ζ] [σ δ σ δ Π (inc φ) Ψ]))  ; proceed right
(defn ζ← [ζ] (let [[Σ Δ _ _ Π φ Ψ] ζ] [Σ Δ Σ Δ Π (inc φ) Ψ])) ; recede left

;; ── Trace / animate ───────────────────────────────────────────────────────────
(defn- preview
  ([action X φ] (preview action X 0 0 φ))
  ([action X pos depth φ]
   (str
     (if (> pos 0) " " "")
     (cond
       (nil? X)     "nil"
       (char? X)    (str "\\" X)
       (string? X)  (str "\"" X "\"")
       (integer? X) (str X)
       (symbol? X)  (str X)
       (float? X)   (str X)
       (>= depth 3) "_"
       (vector? X)  (str "[" (reduce str (map #(preview action %1 %2 (inc depth) 0) X (range))) "]")
       (list? X)    (str "("
                      (reduce str
                        (map #(cond
                                (equal %2 0) (str %1 " ")
                                (> φ 0)
                                  (cond
                                    (< %2 φ) "."
                                    (> %2 (clojure.core/+ φ 2)) "?"
                                    (>= %2 (clojure.core/+ φ 2)) " ?"
                                    (and (equal %2 φ) (identical? action :succeed)) "."
                                    true (preview action %1 (dec %2) (inc depth) 0))
                                true (preview action %1 (dec %2) (inc depth) 0))
                          X (range)))
                      ")")
       (set? X)     (str "\"" (apply str X) "\"")
       true         (str " Yikes!!! " (type X))))))

(defn- animate [action Σ ζ]
  (when (and Σ ζ)
    (println
      (format "%2s %-8s %16s %3d %16s %-9s %s"
        (count (ζΨ ζ))
        (str (ζλ ζ) "/" (ζφ ζ))
        (str "\"" (apply str (take (ζΔ ζ) Σ)) "\"")
        (ζΔ ζ)
        (str "\"" (apply str (reverse (ζΣ ζ))) "\"")
        (str " " action)
        (preview action (ζΠ ζ) (ζφ ζ))))))

;; ── MATCH ─────────────────────────────────────────────────────────────────────
(defn MATCH [Σ Δ Π]
  (loop [action :proceed, ζ [Σ Δ ε ε Π 1 []], Ω []]
    (let [λ (ζλ ζ)]
      (animate action Σ ζ)
      (case λ
        nil
        (do (println)
            (case action (:proceed :succeed) true (:recede :fail) false))

        ALT
        (case action
          :proceed (if (not (ζω ζ))
                     (recur :proceed (ζ↓ ζ) (🡥 Ω ζ))  ; try an alternate
                     (recur :recede  (🡡 Ω) (🡧 Ω)))    ; no more alternatives
          :recede  (recur :proceed (ζ← ζ) Ω)           ; try next alternate, keep left
          :succeed (recur :succeed (ζ↑ ζ) Ω)           ; return match
          :fail    (recur :proceed (ζ← ζ) Ω))          ; try next

        SEQ
        (case action
          :proceed (if (not (ζω ζ))
                     (recur :proceed (ζ↓ ζ) Ω)         ; try a subsequent
                     (recur :succeed (ζ↑ ζ) Ω))        ; all subsequents matched
          :succeed (recur :proceed (ζ→ ζ) Ω)           ; advance to next
          :fail    (recur :recede  (🡡 Ω) (🡧 Ω)))      ; backtrack

        LIT$
        (case action
          :proceed
          (let [[Σ Δ _ _ Π] ζ
                [σ δ]        (LIT$ Σ Δ Π)]
            (if (>= δ 0)
              (recur :succeed (ζ↑ ζ σ δ) Ω)
              (recur :fail    (ζ↑ ζ Σ Δ) Ω))))

        SUCCEED! (let [[Σ Δ] ζ] (recur :succeed (ζ↑ ζ Σ Δ) Ω))
        FAIL!    (let [[Σ Δ] ζ] (recur :fail    (ζ↑ ζ Σ Δ) Ω))

        (ANY$ NOTANY$ SPAN$ BREAK$ BREAKX$ POS# RPOS# LEN# TAB# RTAB#)
        (case action
          :proceed
          (let [[Σ Δ _ _ Π] ζ
                [σ δ]        (($$ λ) Σ Δ (second Π))]
            (if (>= δ 0)
              (recur :succeed (ζ↑ ζ σ δ) Ω)
              (recur :fail    (ζ↑ ζ Σ Δ) Ω))))

        X
        (case action
          :proceed
          (let [Π ($$ 'X)] (recur :proceed (ζ↓ ζ Π) Ω)))

        ;; Not yet implemented — return nil (caller treats as failure)
        ARB!   nil
        BAL!   nil
        ARBNO! nil
        ABORT! nil))))
