(ns SNOBOL4clojure.match
  "SNOBOL4 pattern match engine — core loop and public API.
  Frame infrastructure is in engine-frame.clj."
  (:require [SNOBOL4clojure.env          :refer [ε equal $$ snobol-set!]]
            [SNOBOL4clojure.primitives   :refer
             [LIT$ ANY$ NOTANY$ SPAN$ NSPAN$ BREAK$ BREAKX$
              POS# RPOS# LEN# TAB# RTAB# BOL# EOL#
              SUCCEED! FAIL! ARB! BAL! ARBNO! ABORT!]]
            [SNOBOL4clojure.patterns     :refer [POS RPOS]]
            [SNOBOL4clojure.engine-frame :refer
             [*trace* ζΣ ζΔ ζσ ζδ ζΠ ζφ ζΨ ζλ ζα ζω animate
              ζ↓ ζ↑ ζ→ ζ← 🡡 🡥 🡧]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Core engine ───────────────────────────────────────────────────────────────
;; Returns [start end] on success, nil on failure.
;; Σ       — seq of chars (remaining subject at start position)
;; Δ       — start position (integer)
;; Π       — pattern node
;; start-Δ — the anchor position for the returned span start
(defn engine [Σ Δ Π start-Δ full-subject]
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

        (ANY$ NOTANY$ SPAN$ NSPAN$ BREAK$ BREAKX$ POS# RPOS# LEN# TAB# RTAB# BOL# EOL#)
        (case action
          :proceed
          (let [[Σ Δ _ _ Π] ζ
                prim-fn {'ANY$    ANY$    'NOTANY$ NOTANY$
                         'SPAN$   SPAN$   'NSPAN$  NSPAN$
                         'BREAK$  BREAK$  'BREAKX$ BREAKX$
                         'POS#    POS#    'RPOS#   RPOS#
                         'LEN#    LEN#    'TAB#    TAB#
                         'RTAB#   RTAB#   'BOL#    BOL#
                         'EOL#    EOL#}
                [σ δ]        ((prim-fn λ) Σ Δ (second Π))]
            (if (>= δ 0)
              (recur :succeed (ζ↑ ζ σ δ) Ω)
              (recur :fail    (ζ↑ ζ Σ Δ) Ω))))

        X
        (case action
          :proceed
          (let [Π ($$ 'X)] (recur :proceed (ζ↓ ζ Π) Ω)))

        ;; ── BREAKX# ──────────────────────────────────────────────────────────
        ;; BREAKX(cs) — like BREAK but allows backtracking.
        ;; On first attempt: scan to first char in cs (same as BREAK).
        ;; On retry (:recede): advance one MORE char past the break char, then
        ;; scan for the next occurrence.  φ (ζ slot 5) stores the retry offset.
        ;; This gives the "slide forward" behaviour seen in Snobol4.Net BreakX_014.
        BREAKX#
        (case action
          :proceed
          (let [[Σ Δ _ _ Π] ζ
                cs           (second Π)
                [σ δ]        (BREAKX$ Σ Δ cs)]
            (if (>= δ 0)
              ;; matched up to a break char — push retry frame, succeed
              (recur :succeed (ζ↑ ζ σ δ) (🡥 Ω (assoc ζ 5 (inc δ))))
              ;; no break char in sight — fail normally
              (recur :fail (ζ↑ ζ Σ Δ) Ω)))
          :recede
          ;; retry: skip the previously-found break char and scan again
          (let [retry-Δ (ζφ ζ)
                full-len (count full-subject)]
            (if (> retry-Δ full-len)
              (recur :recede (🡡 Ω) (🡧 Ω))
              (let [Σ-retry (drop retry-Δ (seq full-subject))
                    cs      (second (ζΠ ζ))
                    [σ δ]   (BREAKX$ Σ-retry retry-Δ cs)]
                (if (>= δ 0)
                  (recur :succeed (ζ↑ ζ σ δ) (🡥 Ω (assoc ζ 5 (inc δ))))
                  (recur :recede (🡡 Ω) (🡧 Ω))))))
          :succeed (recur :succeed (ζ↑ ζ) Ω)
          :fail    (recur :recede  (🡡 Ω) (🡧 Ω)))

        ;; ── CAPTURE-IMM ($) ──────────────────────────────────────────────────
        ;; Immediate assignment: assign to var as soon as the inner pattern P
        ;; matches, regardless of whether the overall match eventually succeeds.
        ;; On backtrack, the assignment is NOT undone (true SNOBOL4 $ semantics).
        ;; ── CAPTURE-IMM ($) ──────────────────────────────────────────────────
        ;; Immediate assignment: assign to var as soon as the inner pattern P
        ;; matches, unconditionally (even if overall match later fails).
        ;; IMPORTANT: on :succeed we do NOT pop Ω — any retry frames pushed by
        ;; our child (e.g. BREAKX#) must remain on Ω so backtracking can reach them.
        ;; We pushed our own frame at :proceed; on :recede we pop that frame.
        CAPTURE-IMM
        (case action
          :proceed
          (recur :proceed (ζ↓ ζ) (🡥 Ω ζ))
          :succeed
          (let [[_ _ var-sym] (ζΠ ζ)
                matched-text  (subs full-subject (ζΔ ζ) (ζδ ζ))]
            (snobol-set! var-sym matched-text)   ; assign immediately, unconditionally
            (recur :succeed (ζ↑ ζ) Ω))           ; leave child retry frames on Ω
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))          ; pop our own frame, pass recede

        ;; ── CAPTURE (legacy alias, same as CAPTURE-IMM) ──────────────────────
        CAPTURE
        (case action
          :proceed
          (recur :proceed (ζ↓ ζ) (🡥 Ω ζ))
          :succeed
          (let [[_ _ var-sym] (ζΠ ζ)
                matched-text  (subs full-subject (ζΔ ζ) (ζδ ζ))]
            (snobol-set! var-sym matched-text)
            (recur :succeed (ζ↑ ζ) Ω))
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; ── CAPTURE-COND (.) ─────────────────────────────────────────────────
        ;; Conditional assignment: assign only when the full match succeeds.
        ;; Same frame/Ω discipline as CAPTURE-IMM.
        CAPTURE-COND
        (case action
          :proceed
          (recur :proceed (ζ↓ ζ) (🡥 Ω ζ))
          :succeed
          (let [[_ _ var-sym] (ζΠ ζ)
                matched-text  (subs full-subject (ζΔ ζ) (ζδ ζ))]
            (snobol-set! var-sym matched-text)
            (recur :succeed (ζ↑ ζ) Ω))           ; leave child retry frames on Ω
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; ── FENCE! ───────────────────────────────────────────────────────────
        ;; Two forms per SNOBOL4/SPITBOL spec (C backend docstring):
        ;;   FENCE(P)  — fence_function: P matches; backtracking *into* P blocked;
        ;;               backtracking *past* this node to outer ALT still allowed.
        ;;   FENCE()   — fence_simple: matches empty string; any backtrack past
        ;;               this point aborts the entire match (returns nil).
        FENCE!
        (case action
          :proceed
          (let [[Σ Δ _ _ _ _ Ψ] ζ
                child            (second (ζΠ ζ))]
            (if child
              ;; FENCE(P): descend into child; push cut barrier onto Ω
              (recur :proceed [Σ Δ Σ Δ child 1 (🡥 Ψ ζ)] (🡥 Ω ζ))
              ;; Bare FENCE(): succeed once; push :ABORT sentinel onto Ω
              (recur :succeed (ζ↑ ζ) (🡥 Ω :ABORT))))
          :succeed
          ;; Child (or bare FENCE) succeeded — pop cut barrier, propagate up
          (recur :succeed (ζ↑ ζ) (🡧 Ω))
          (:recede :fail)
          ;; Backtracked into FENCE.
          ;; FENCE(P): refuse retry, let outer context try next alternative.
          ;; Bare FENCE(): :ABORT sentinel on Ω — terminate entire match.
          (if (clojure.core/= (🡡 Ω) :ABORT)
            nil
            (recur :recede (🡡 Ω) (🡧 Ω))))

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

        ;; ── ABORT! ───────────────────────────────────────────────────────────
        ;; ABORT — immediately terminate the entire match, regardless of context.
        ;; Returns nil from the engine loop unconditionally.
        ABORT!
        nil

        ;; ── REM! ─────────────────────────────────────────────────────────────
        ;; REM — matches all remaining characters from the current cursor.
        ;; Always succeeds (including empty remainder). No backtrack alternative.
        REM!
        (case action
          :proceed
          (recur :succeed (ζ↑ ζ [] (count full-subject)) Ω)
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; -- BAL! ---------------------------------------------------------------
        ;; Matches balanced-paren substrings, multi-yield.
        ;; Scans from entry-Delta carrying running nest depth.
        ;; Yields (succeeds) every time nest returns to 0.
        ;; On retry (:recede), resumes scan from saved [pos nest].
        ;; phi (slot 5) stores [resume-pos resume-nest].
        BAL!
        (let [sublen  (count full-subject)
              pos0    (ζΔ ζ)
              scan-bal (fn [pos nest]
                         (loop [p pos n nest]
                           (cond
                             (clojure.core/>= p sublen) nil
                             :else
                             (let [ch (nth full-subject p)
                                   n2 (case ch
                                        \( (inc n)
                                        \) (dec n)
                                        n)]
                               (cond
                                 (clojure.core/< n2 0) nil
                                 (clojure.core/= n2 0) [(inc p) (inc p) 0]
                                 :else (recur (inc p) n2))))))]
          (case action
            :proceed
            (if-let [[end np nn] (scan-bal pos0 0)]
              (recur :succeed
                     (ζ↑ ζ (drop end (seq full-subject)) end)
                     (🡥 Ω (assoc ζ 5 [np nn])))
              (recur :fail (ζ↑ ζ (ζΣ ζ) pos0) Ω))
            :recede
            (let [[rp rn] (ζφ ζ)]
              (if-let [[end np nn] (scan-bal rp rn)]
                (recur :succeed
                       (ζ↑ ζ (drop end (seq full-subject)) end)
                       (🡥 Ω (assoc ζ 5 [np nn])))
                (recur :recede (🡡 Ω) (🡧 Ω))))
            :succeed (recur :succeed (ζ↑ ζ) Ω)
            :fail    (recur :recede  (🡡 Ω) (🡧 Ω))))

        ;; -- COLLECT! ------------------------------------------------------------
        ;; (COLLECT! bag-atom P): match P; on each :succeed append [entry-Δ δ] to
        ;; bag then immediately recede — driving P through all its Ω retry frames.
        ;; Mirrors the SNOBOL4 idiom  P $ X FAIL  for exhausting multi-yield nodes.
        COLLECT!
        (case action
          :proceed
          (let [[S D _ _ Pi _ Psi] ζ
                P   (nth Pi 2)
                chi [S D S D P 1 (🡥 Psi ζ)]]
            (recur :proceed chi (🡥 Ω ζ)))
          :succeed
          (let [bag (nth (ζΠ ζ) 1)]
            (swap! bag conj [(ζΔ ζ) (ζδ ζ)])
            (recur :recede (🡡 Ω) (🡧 Ω)))
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; -- CURSOR-IMM! ---------------------------------------------------------
        ;; (@N): assign current cursor position to variable N.  Zero-width.
        CURSOR-IMM!
        (case action
          (:proceed :succeed)
          (do (snobol-set! (second (ζΠ ζ)) (ζδ ζ))
              (recur :succeed (ζ↑ ζ (ζΣ ζ) (ζδ ζ)) Ω))
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; -- CONJ! ---------------------------------------------------------------
        ;; (CONJ P Q): P and Q must both match from same start, same end position.
        CONJ!
        (case action
          :proceed
          (let [P (nth (ζΠ ζ) 1)]
            (recur :proceed (ζ↓ ζ P) (🡥 Ω ζ)))
          :succeed
          (let [conj-frame (🡡 Ω)
                p-end      (ζδ ζ)
                Q          (nth (ζΠ conj-frame) 2)
                pos0       (ζΔ conj-frame)
                Q-frame    [(ζΣ conj-frame) pos0 (ζΣ conj-frame) pos0 Q 1
                            (ζΨ conj-frame)]]
            (recur :proceed Q-frame (🡥 (🡧 Ω) (assoc conj-frame 5 p-end))))
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; -- DEFER! --------------------------------------------------------------
        ;; (*expr): deferred pattern — thunk evaluated at match time.
        DEFER!
        (case action
          :proceed
          (let [thunk (second (ζΠ ζ))
                P     (if (fn? thunk) (thunk) thunk)]
            (recur :proceed (ζ↓ ζ P) (🡥 Ω ζ)))
          :succeed (recur :succeed (ζ↑ ζ) Ω)
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))))))
