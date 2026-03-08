(ns SNOBOL4clojure.engine-frame
  "Frame infrastructure for the SNOBOL4 match engine.

  A frame is a 7-element vector  [Σ Δ σ δ Π φ Ψ]:
    Σ/Δ — subject seq + absolute position on entry to this frame
    σ/δ — subject seq + absolute position as updated by scanning
    Π   — current pattern node (string | symbol | list | seq)
    φ   — child index into Π (1-based; used by ALT/SEQ)
    Ψ   — parent-frame stack (vector of frames)

  Provides: frame accessors, Omega/Psi stack helpers, frame
  transitions (descend ζ↓, ascend ζ↑, advance ζ→/ζ←), and
  per-step trace output."
  (:require [SNOBOL4clojure.env :refer [equal out]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Tracing (off by default) ──────────────────────────────────────────────────
(def ^:dynamic *trace* false)

;; ── Frame accessors ───────────────────────────────────────────────────────────
(defn ζΣ [ζ] (if ζ (ζ 0)))  ; subject chars on entry to this frame
(defn ζΔ [ζ] (if ζ (ζ 1)))  ; position on entry
(defn ζσ [ζ] (if ζ (ζ 2)))  ; subject chars (current)
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
    true     (out ["ζλ? " (type (ζΠ ζ)) (ζΠ ζ)])))

;; ── Stack helpers ─────────────────────────────────────────────────────────────
(defn top  [Ψ]   (last Ψ))
(defn push [Ψ ζ] (if Ψ (conj Ψ ζ)))
(defn pull [Ψ]   (if Ψ (if-not (empty? Ψ) (pop Ψ))))
(defn 🡡 [Ω]     (top Ω))
(defn 🡥 [Ω ζ]   (push Ω ζ))
(defn 🡧 [Ω]     (pull Ω))

;; ── Frame transitions ─────────────────────────────────────────────────────────
(defn ζ↓
  ([ζ Π]  (let [[Σ Δ _ _ _ _ Ψ] ζ] [Σ Δ Σ Δ Π         1       Ψ]))
  ([ζ]    (let [[Σ Δ _ _ Π φ Ψ] ζ] [Σ Δ Σ Δ (nth Π φ) 1       (🡥 Ψ ζ)])))

(defn ζ↑
  ([ζ σ δ] (let [[Σ Δ _ _ _ _ Ψ] ζ] [Σ Δ σ δ (ζΠ (🡡 Ψ)) (ζφ (🡡 Ψ)) (🡧 Ψ)]))
  ([ζ]     (let [[Σ Δ σ δ _ _ Ψ] ζ] [Σ Δ σ δ (ζΠ (🡡 Ψ)) (ζφ (🡡 Ψ)) (🡧 Ψ)])))

(defn ζ→ [ζ] (let [[_ _ σ δ Π φ Ψ] ζ] [σ δ σ δ Π (inc φ) Ψ]))
(defn ζ← [ζ] (let [[Σ Δ _ _ Π φ Ψ] ζ] [Σ Δ Σ Δ Π (inc φ) Ψ]))

;; ── Trace helpers ─────────────────────────────────────────────────────────────
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
                                  (< %2 φ)                            "."
                                  (> %2 (clojure.core/+ φ 2))        "?"
                                  (>= %2 (clojure.core/+ φ 2))       " ?"
                                  (and (equal %2 φ)
                                       (identical? action :succeed))  "."
                                  true (preview action %1 (dec %2) (inc depth) 0))
                                true (preview action %1 (dec %2) (inc depth) 0))
                          X (range)))
                      ")")
       (set? X)     (str "\"" (apply str X) "\"")
       true         (str " Yikes!!! " (type X))))))

(defn animate [action full-subject ζ]
  (when (and *trace* full-subject ζ)
    (println
      (format "%2s %-8s %16s %3d %16s %-9s %s"
        (count (ζΨ ζ))
        (str (ζλ ζ) "/" (ζφ ζ))
        (str "\"" (apply str (take (ζΔ ζ) full-subject)) "\"")
        (ζΔ ζ)
        (str "\"" (apply str (reverse (ζΣ ζ))) "\"")
        (str " " action)
        (preview action (ζΠ ζ) (ζφ ζ))))))
