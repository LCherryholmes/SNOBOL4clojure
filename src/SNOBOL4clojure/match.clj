(ns SNOBOL4clojure.match
  "The SNOBOL4 pattern match engine.

  An explicit iterative state machine over a 7-element frame vector:
    [Σ Δ σ δ Π φ Ψ]
    Σ/Δ — subject chars + position on entry to this frame
    σ/δ — subject chars + position as updated by scanning
    Π   — current pattern node
    φ   — index into Π (for ALT/SEQ child iteration)
    Ψ   — parent frame stack
  Actions: :proceed :succeed :recede :fail

  Public API (three entry points, matching the Python/C# siblings):

    SEARCH    s pat  — slide pattern across subject; return [start end] or nil
    MATCH     s pat  — anchor at position 0;         return [start end] or nil
    FULLMATCH s pat  — anchor at both ends;           return [start end] or nil

  [start end] is a half-open span: (subs s start end) extracts the match.
  nil means the pattern did not match.

  Tracing: bind *trace* to true to enable per-step animation output."
  (:require [SNOBOL4clojure.env        :refer [ε equal out $$ snobol-set!]]
            [SNOBOL4clojure.primitives :refer
             [LIT$ ANY$ NOTANY$ SPAN$ BREAK$ BREAKX$
              POS# RPOS# LEN# TAB# RTAB#
              SUCCEED! FAIL! ARB! BAL! ARBNO! ABORT!]]
            [SNOBOL4clojure.patterns   :refer [POS RPOS]])
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

(defn- ζα [ζ] (<= (ζφ ζ) 1))
(defn- ζω [ζ] (>= (ζφ ζ) (count (ζΠ ζ))))

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
(defn- top  [Ψ]   (last Ψ))
(defn- push [Ψ ζ] (if Ψ (conj Ψ ζ)))
(defn- pull [Ψ]   (if Ψ (if-not (empty? Ψ) (pop Ψ))))
(defn- 🡡 [Ω]     (top Ω))
(defn- 🡥 [Ω ζ]   (push Ω ζ))
(defn- 🡧 [Ω]     (pull Ω))

;; ── Frame transitions ─────────────────────────────────────────────────────────
(defn- ζ↓
  ([ζ Π]  (let [[Σ Δ _ _ _ _ Ψ] ζ] [Σ Δ Σ Δ Π         1       Ψ]))
  ([ζ]    (let [[Σ Δ _ _ Π φ Ψ] ζ] [Σ Δ Σ Δ (nth Π φ) 1       (🡥 Ψ ζ)])))

(defn- ζ↑
  ([ζ σ δ] (let [[Σ Δ _ _ _ _ Ψ] ζ] [Σ Δ σ δ (ζΠ (🡡 Ψ)) (ζφ (🡡 Ψ)) (🡧 Ψ)]))
  ([ζ]     (let [[Σ Δ σ δ _ _ Ψ] ζ] [Σ Δ σ δ (ζΠ (🡡 Ψ)) (ζφ (🡡 Ψ)) (🡧 Ψ)])))

(defn- ζ→ [ζ] (let [[_ _ σ δ Π φ Ψ] ζ] [σ δ σ δ Π (inc φ) Ψ]))
(defn- ζ← [ζ] (let [[Σ Δ _ _ Π φ Ψ] ζ] [Σ Δ Σ Δ Π (inc φ) Ψ]))

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

(defn- animate [action full-subject ζ]
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

;; ── Core engine ───────────────────────────────────────────────────────────────
;; Returns [start end] on success, nil on failure.
;; Σ       — seq of chars (remaining subject at start position)
;; Δ       — start position (integer)
;; Π       — pattern node
;; start-Δ — the anchor position for the returned span start
(defn- engine [Σ Δ Π start-Δ full-subject]
  (loop [action :proceed, ζ [Σ Δ ε ε Π 1 []], Ω []]
    (let [λ (ζλ ζ)]
      (animate action full-subject ζ)
      (case λ
        nil
        (case action
          (:proceed :succeed) [start-Δ (ζδ ζ)]
          (:recede :fail)     nil)

        ALT
        (case action
          :proceed (if (not (ζω ζ))
                     (recur :proceed (ζ↓ ζ) (🡥 Ω ζ))
                     (recur :recede  (🡡 Ω)  (🡧 Ω)))
          :recede  (recur :proceed (ζ← ζ) Ω)
          :succeed (recur :succeed (ζ↑ ζ) Ω)
          :fail    (recur :proceed (ζ← ζ) Ω))

        SEQ
        (case action
          :proceed (if (not (ζω ζ))
                     (recur :proceed (ζ↓ ζ) Ω)
                     (recur :succeed (ζ↑ ζ) Ω))
          :succeed (recur :proceed (ζ→ ζ) Ω)
          :fail    (recur :recede  (🡡 Ω) (🡧 Ω)))

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
                prim-fn {'ANY$    ANY$    'NOTANY$ NOTANY$
                         'SPAN$   SPAN$   'BREAK$  BREAK$
                         'BREAKX$ BREAKX$ 'POS#    POS#
                         'RPOS#   RPOS#   'LEN#    LEN#
                         'TAB#    TAB#    'RTAB#   RTAB#}
                [σ δ]        ((prim-fn λ) Σ Δ (second Π))]
            (if (>= δ 0)
              (recur :succeed (ζ↑ ζ σ δ) Ω)
              (recur :fail    (ζ↑ ζ Σ Δ) Ω))))

        X
        (case action
          :proceed
          (let [Π ($$ 'X)] (recur :proceed (ζ↓ ζ Π) Ω)))

        CAPTURE
        ;; (CAPTURE pattern var-symbol): match pattern, assign matched text to var
        ;; Π = (CAPTURE P N) — on succeed, (subs subject entry-Δ current-δ) → N
        (case action
          :proceed
          (recur :proceed (ζ↓ ζ) (🡥 Ω ζ))
          :succeed
          (let [[_ _ var-sym] (ζΠ ζ)
                matched-text  (subs full-subject (ζΔ ζ) (ζδ ζ))]
            (snobol-set! var-sym matched-text)
            (recur :succeed (ζ↑ ζ) (🡧 Ω)))
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; ── FENCE! ───────────────────────────────────────────────────────────
        ;; FENCE(P) — match P, but if P succeeds, cut: prevent retry of P on backtrack.
        ;; Π = (FENCE! child-pattern)  or  (FENCE!)  (bare FENCE = cut-and-abort)
        ;; On :proceed  — descend into child; push self onto Ω as cut marker
        ;; On :succeed  — pop cut marker from Ω, propagate success
        ;; On :recede   — FENCE was backtracked into: refuse to retry (fail upward)
        ;;                For bare FENCE this aborts the match entirely.
        FENCE!
        ;; FENCE(P): match P; if P succeeds, commit (no retry of P on backtrack).
        ;; If P fails, FENCE itself fails (outer ALT can still try next branch).
        ;; Bare FENCE(): succeed once, then abort on any backtrack.
        (case action
          :proceed
          (let [[Σ Δ _ _ _ _ Ψ] ζ
                child            (second (ζΠ ζ))]   ; nil for bare (FENCE!)
            (if child
              ;; Build child frame: set Π=child, push FENCE onto Ψ so :succeed
              ;; returns here.  Also push FENCE onto Ω as a cut barrier.
              (recur :proceed [Σ Δ Σ Δ child 1 (🡥 Ψ ζ)] (🡥 Ω ζ))
              ;; Bare FENCE — succeed once; push cut barrier so :recede aborts
              (recur :succeed (ζ↑ ζ) (🡥 Ω ζ))))
          :succeed
          ;; Child succeeded — consume the cut barrier, propagate success upward
          (recur :succeed (ζ↑ ζ) (🡧 Ω))
          (:recede :fail)
          ;; Backtracked into FENCE — refuse to retry child; fail upward
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; ── ARB! ─────────────────────────────────────────────────────────────
        ;; ARB — matches any string, shortest first (0 chars, then 1, 2, …).
        ;; Uses φ as the "next retry length".
        ;; :proceed  — match 0 chars; push self onto Ω with φ=1
        ;; :recede   — consume φ chars if enough subject remains; push φ+1 frame
        ARB!
        (case action
          :proceed
          (recur :succeed (ζ↑ ζ (ζΣ ζ) (ζΔ ζ)) (🡥 Ω (assoc ζ 5 1)))
          :recede
          (let [len (ζφ ζ)
                remaining (count (ζΣ ζ))]
            (if (<= len remaining)
              (recur :succeed
                     (ζ↑ ζ (drop len (ζΣ ζ)) (clojure.core/+ (ζΔ ζ) len))
                     (🡥 Ω (assoc ζ 5 (inc len))))
              (recur :recede (🡡 Ω) (🡧 Ω)))))

        ;; ── ARBNO! ───────────────────────────────────────────────────────────
        ;; ARBNO(P) — zero or more repetitions of P, shortest first.
        ;; Expands lazily to ALT[ε, SEQ[P, ARBNO(P)]] on each :proceed.
        ;; The recursive ARBNO(P) in the SEQ ensures the engine re-expands
        ;; on every attempted repetition, giving correct backtracking.
        ARBNO!
        (case action
          :proceed
          (let [[Σ Δ _ _ _ _ Ψ] ζ
                P               (second (ζΠ ζ))
                expanded        (list 'ALT "" (list 'SEQ P (ζΠ ζ)))]
            (recur :proceed [Σ Δ Σ Δ expanded 1 (🡥 Ψ ζ)] Ω))
          :succeed
          (recur :succeed (ζ↑ ζ) Ω)
          (:recede :fail)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; Not yet implemented
        (BAL! ABORT!) nil))))

;; ── Public API ────────────────────────────────────────────────────────────────

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
   Returns [start end] on success, nil on failure.
   Equivalent to SEARCH with POS(0) prepended."
  [S P]
  (engine (seq S) 0 (list 'SEQ (POS 0) P) 0 S))

(defn FULLMATCH
  "Match pattern P against the entirety of string S (anchored both ends).
   Returns [0 (count S)] on success, nil on failure.
   Equivalent to SEARCH with POS(0) prepended and RPOS(0) appended."
  [S P]
  (engine (seq S) 0 (list 'SEQ (POS 0) P (RPOS 0)) 0 S))

(defn REPLACE
  "Match pattern P against S; if matched, replace the matched span with R.
   Returns the new string on success, nil on failure.
   Example: (REPLACE \"hello world\" \"world\" \"Clojure\") => \"hello Clojure\""
  [S P R]
  (when-let [[start end] (SEARCH S P)]
    (str (subs S 0 start) R (subs S end))))
