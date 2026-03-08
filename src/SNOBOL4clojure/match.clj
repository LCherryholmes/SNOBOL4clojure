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
             [LIT$ ANY$ NOTANY$ SPAN$ NSPAN$ BREAK$ BREAKX$
              POS# RPOS# LEN# TAB# RTAB# BOL# EOL#
              SUCCEED! FAIL! ARB! BAL! ARBNO! ABORT!]]
            [SNOBOL4clojure.patterns   :refer [POS RPOS]]
            [SNOBOL4clojure.trace      :refer [pattern-trace-active?]])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Tracing (off by default) ──────────────────────────────────────────────────
(def ^:dynamic *trace* false)

;; ── Pending conditional captures (for the . operator) ────────────────────────
;; A volatile! list of [var-sym matched-text] pairs, populated during a match
;; by CAPTURE-COND nodes and committed to variables only when the overall match
;; succeeds.  Mirrors the NAMICL "name list" mechanism in CSNOBOL4/v311.sil.
;; Bound fresh by SEARCH/MATCH/FULLMATCH around each engine call.
(def ^:dynamic *pending-cond* nil)

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
    (list?       ζ)  (first ζ)        ; raw list node on Ω (e.g. COND-UNDO!)
    (nil?    (ζΠ ζ)) nil
    (string? (ζΠ ζ)) 'LIT$
    (symbol? (ζΠ ζ)) (ζΠ ζ)
    (vector? (ζΠ ζ)) 'SEQ    ; vector = inline SEQ (e.g. CAPTURE-COND expansion)
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
  (when (and *trace* full-subject ζ (vector? ζ))
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
        ;; Conditional assignment — mirrors CSNOBOL4/v311 NME + ENME + FNME nodes.
        ;;
        ;; Reference: v311.sil lines ~3885-3930 (NME/ENME/FNME procedures).
        ;; Architecture: NME fires BEFORE P (saves start cursor onto history stack);
        ;; ENME fires AFTER P (computes span, adds to NAMICL "name list");
        ;; FNME is the backtrack/undo node (removes from name list).
        ;; The name list is committed to variables only on overall match success.
        ;;
        ;; Our implementation: CAPTURE-COND is a wrapper around child P.
        ;; On :proceed — descend into P, push self onto Ω (recording entry cursor ζΔ).
        ;; On :succeed — P matched; span is [ζΔ, ζδ].  Conj [var-sym text] onto
        ;;               *pending-cond* (the name list).  Also push an UNDO frame
        ;;               onto Ω so backtracking past this point removes the entry.
        ;; On :recede  — backtracking into us; pop our frame, pass recede upward.
        ;; The UNDO frame (COND-UNDO!) cleans up *pending-cond* on further backtrack.
        ;; SEARCH/MATCH/FULLMATCH commit *pending-cond* when engine returns success.
        CAPTURE-COND
        ;; Expands into SEQ[COND-MARK!(start), child-P, COND-ASSIGN!(var)]
        ;; mirroring CSNOBOL4/v311 NME+ENME architecture.
        (case action
          :proceed
          (let [[_ child-P var-sym] (ζΠ ζ)
                start-pos           (ζΔ ζ)
                expanded            (vector (list 'COND-MARK! start-pos)
                                            child-P
                                            (list 'COND-ASSIGN! var-sym start-pos))]
            (recur :proceed (ζ↓ ζ expanded) (🡥 Ω ζ)))
          :succeed (recur :succeed (ζ↑ ζ) Ω)
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; ── COND-MARK! ────────────────────────────────────────────
        ;; Zero-width NME node: records start cursor in COND-ASSIGN! (baked in).
        ;; Always succeeds immediately.
        COND-MARK!
        (case action
          :proceed        (recur :succeed (ζ↑ ζ) Ω)
          :succeed        (recur :succeed (ζ↑ ζ) Ω)
          (:fail :recede) (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; ── COND-ASSIGN! ─────────────────────────────────────────
        ;; Post-P ENME node: fires after P matches.
        ;; Computes span [start-pos baked-in, entry-cursor = end of P],
        ;; adds [var-sym text] to *pending-cond*, pushes COND-UNDO! onto Ω.
        COND-ASSIGN!
        (case action
          :proceed
          (let [[_ var-sym start-pos] (ζΠ ζ)
                end-pos              (ζΔ ζ)
                matched-text         (subs full-subject start-pos end-pos)]
            (when *pending-cond*
              (vswap! *pending-cond* conj [var-sym matched-text]))
            (recur :succeed (ζ↑ ζ) (🡥 Ω (list 'COND-UNDO! var-sym))))
          :succeed        (recur :succeed (ζ↑ ζ) Ω)
          (:fail :recede) (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; ── COND-UNDO! (internal) ────────────────────────────────────────────
        ;; Backtrack undo node for CAPTURE-COND.  When the engine recedes past
        ;; a completed CAPTURE-COND, this removes the most recent pending entry
        ;; for var-sym from *pending-cond*, mirroring CSNOBOL4 DNME/FNME undo.
        COND-UNDO!
        ;; This node lives on Ω, not as a child frame.  It is reached only via
        ;; :recede → (🡡 Ω).  The ζ here IS the COND-UNDO! list itself.
        ;; On recede: pop the most-recently-added pending entry for this var,
        ;; then continue receding.
        (let [[_ var-sym] ζ]
          (when *pending-cond*
            (vswap! *pending-cond*
                    (fn [lst]
                      (let [[kept _]
                            (reduce (fn [[acc done?] [vs _ :as e]]
                                      (if (and (not done?) (clojure.core/= vs var-sym))
                                        [acc true]
                                        [(conj acc e) done?]))
                                    [[] false] lst)]
                        kept))))
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
        ;; (@N): assign the current cursor position (integer) to variable N.
        ;; Zero-width: succeeds without consuming input.  No backtrack alternative.
        CURSOR-IMM!
        (case action
          (:proceed :succeed)
          (do (snobol-set! (second (ζΠ ζ)) (ζδ ζ))
              (recur :succeed (ζ↑ ζ (ζΣ ζ) (ζδ ζ)) Ω))
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; -- CONJ! ---------------------------------------------------------------
        ;; (CONJ! P Q): both P and Q must succeed from the same cursor position.
        ;; P determines the span advanced; Q is a pure assertion (must succeed
        ;; from the same start but its matched length is irrelevant).
        ;; This is the most general "AND" for patterns.  Length-equality is NOT
        ;; required — use an immediate-action node (e.g. CHECK!) after CONJ to
        ;; enforce additional span constraints if needed.
        ;;
        ;; CONJ is NOT in SPITBOL, CSNOBOL4, or standard SNOBOL4 — it is a
        ;; SNOBOL4clojure extension.  No reference source exists; semantics are
        ;; defined here by design decision.
        CONJ!
        (case action
          :proceed
          (let [[_ P Q] (ζΠ ζ)
                pos0    (ζΔ ζ)
                chars0  (ζΣ ζ)
                p-result (engine chars0 pos0 P pos0 full-subject)]
            (if p-result
              (let [p-end    (second p-result)
                    q-result (engine chars0 pos0 Q pos0 full-subject)]
                (if q-result                          ; Q must succeed (assertion)
                  (recur :succeed (ζ↑ ζ chars0 p-end) Ω)   ; span = P's span
                  (recur :fail    (ζ↑ ζ (ζΣ ζ) pos0) Ω)))
              (recur :fail (ζ↑ ζ (ζΣ ζ) pos0) Ω)))
          (:succeed :fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))

        ;; -- DEFER! --------------------------------------------------------------
        ;; (*expr): deferred pattern — thunk is a (fn [] pattern) evaluated at
        ;; match time, not build time.  Fixes eager-evaluation of guards like
        ;; (EQ N 2) inside ALT branches.
        DEFER!
        (case action
          :proceed
          (let [thunk (second (ζΠ ζ))
                P     (if (fn? thunk) (thunk) thunk)]
            (recur :proceed (ζ↓ ζ P) (🡥 Ω ζ)))
          :succeed (recur :succeed (ζ↑ ζ) Ω)
          (:fail :recede)
          (recur :recede (🡡 Ω) (🡧 Ω)))))))

;; ── Public API ────────────────────────────────────────────────────────────────

(defn- commit-pending!
  "Commit all pending conditional (.) assignments to their variables.
   Called by SEARCH/MATCH/FULLMATCH when the engine returns success.
   Mirrors CSNOBOL4/v311 NMD procedure that walks the NAMICL name list."
  []
  (doseq [[var-sym text] (reverse @*pending-cond*)]
    (snobol-set! var-sym text)))

(defn SEARCH
  "Slide pattern P across string S, trying each start position 0..n.
   Returns [start end] for the first match found, nil if none.
   (subs S start end) extracts the matched substring.
   Binds *trace* true when a PATTERN trace is active (via trace.clj)."
  [S P]
  (binding [*trace*        (or *trace* (pattern-trace-active? S))
            *pending-cond* (volatile! [])]
    (let [chars (seq S)
          n     (count S)]
      (loop [i 0]
        (when (<= i n)
          (vswap! *pending-cond* (constantly []))          ; reset for each start pos
          (let [result (engine (drop i chars) i P i S)]
            (if result
              (do (commit-pending!) result)
              (recur (inc i)))))))))

(defn MATCH
  "Match pattern P against string S anchored at position 0.
   Returns [start end] on success, nil on failure.
   Equivalent to SEARCH with POS(0) prepended."
  [S P]
  (binding [*pending-cond* (volatile! [])]
    (let [result (engine (seq S) 0 (list 'SEQ (POS 0) P) 0 S)]
      (when result (commit-pending!))
      result)))

(defn FULLMATCH
  "Match pattern P against the entirety of string S (anchored both ends).
   Returns [0 (count S)] on success, nil on failure.
   Equivalent to SEARCH with POS(0) prepended and RPOS(0) appended."
  [S P]
  (binding [*pending-cond* (volatile! [])]
    (let [result (engine (seq S) 0 (list 'SEQ (POS 0) P (RPOS 0)) 0 S)]
      (when result (commit-pending!))
      result)))

(defn REPLACE
  "Match pattern P against S; if matched, replace the matched span with R.
   Returns the new string on success, nil on failure.
   Example: (REPLACE \"hello world\" \"world\" \"Clojure\") => \"hello Clojure\""
  [S P R]
  (when-let [[start end] (SEARCH S P)]
    (str (subs S 0 start) R (subs S end))))

(defn COLLECT!
  "Exhaustively collect every match [start end] of pattern P across string S,
   including all multi-yield backtrack alternatives (e.g. BAL, ARB, ARBNO).
   Mirrors the SNOBOL4 idiom  P $ X FAIL  — each match is captured then the
   engine is forced to recede, driving P through all its Omega retry frames.
   Returns a vector of [start end] pairs in scan order."
  [S P]
  (let [chars (seq S)
        n     (count S)
        bag   (atom [])
        node  (list 'COLLECT! bag P)]
    (doseq [i (range (inc n))]
      (engine (drop i chars) i node i S))
    @bag))
