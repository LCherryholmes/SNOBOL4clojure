# SNOBOL4clojure — Master Plan & Handoff Document

> **For a new Claude session**: Read this file, then `ASSESSMENT.md` for the
> full feature matrix. Transcripts are at `/mnt/transcripts/journal.txt`.
> Start with `lein test` to confirm baseline (should be 120 tests / 403 assertions).

---

## What This Project Is

A complete SNOBOL4 implementation in Clojure. Full semantic fidelity with the
SNOBOL4 standard — not a regex wrapper, but a proper pattern-match engine with
backtracking, captures, alternation, and the full SNOBOL4 pattern calculus.

**Repository**: https://github.com/LCherryholmes/SNOBOL4clojure.git  
**Location on disk**: `/home/claude/SNOBOL4clojure`  
**Test runner**: `lein test` (Leiningen 2.12.0, Java 21)

---

## Core Design Principle

User code calls `(GLOBALS *ns*)` once. All SNOBOL variables live in that one
user namespace. The library never owns variables; it operates on whatever
namespace the user handed it.

Key env functions: `GLOBALS`, `active-ns`, `snobol-set!`, `$$`

---

## File Map

| File | Responsibility |
|------|----------------|
| `env.clj` | globals, DATATYPE, NAME deftype, `$$`/`snobol-set!`, arithmetic, `GLOBALS` |
| `primitives.clj` | low-level scanners: LIT$, ANY$, SPAN$, NSPAN$, BREAK$, BREAKX$, POS#, RPOS#, LEN#, TAB#, RTAB#, BOL#, EOL# |
| `match.clj` | MATCH state machine + SEARCH/MATCH/FULLMATCH/REPLACE public API |
| `patterns.clj` | pattern constructors: ANY, SPAN, NSPAN, BREAK, BREAKX, BOL, EOL, POS, ARBNO, FENCE, ABORT, REM… |
| `functions.clj` | built-in fns: REPLACE, SIZE, DATA, DATA!, ASCII, CHAR, REMDR, INTEGER, REAL, STRING, INPUT |
| `grammar.clj` | instaparse grammar + parse-statement/parse-expression |
| `emitter.clj` | AST → Clojure IR transform |
| `compiler.clj` | CODE!/CODE: source text → labeled statement table |
| `operators.clj` | operators (?, =, \|, $, ., +…), EVAL/EVAL!/INVOKE, comparison primitives |
| `runtime.clj` | RUN: GOTO-driven statement interpreter |
| `core.clj` | thin facade — explicit `def` re-exports of full public API |

---

## Reference Material (on disk)

| Path | Contents |
|------|----------|
| `/home/claude/SNOBOL4python` | Python reference implementation |
| `/home/claude/x64ref/x64-main/` | C SPITBOL reference |
| `/home/claude/snobol4ref/snobol4-2.3.3/` | C SNOBOL4 reference |
| `/home/claude/Snobol4.Net/Snobol4.Net-feature-msil-trace/TestSnobol4/` | .NET reference test suite (ground truth) |

The Snobol4.Net test suite is the **primary ground truth** for edge cases.
Key subdirectory: `Function/Pattern/` — one `.cs` file per pattern primitive.

---

## Sprint History

| Sprint | Commit | Tests | What Was Done |
|--------|--------|-------|---------------|
| Stage 6 | `6e9683d` | baseline | Runtime polish, milestones 6A–6E |
| Stage 7 | `26a9b25` | — | FENCE implementation, namespace isolation |
| Stage 7b | `99b2563` | — | DATATYPE returns PATTERN for list nodes |
| Stage 7c | `4813ae8` | 82/314 | ARB and ARBNO engine implementation |
| Sprint 8 | `69a6f48` | 102/379 | ABORT!, bare FENCE(), REM engine node, ASCII, CHAR, REMDR, INTEGER, REAL, STRING |
| Sprint 9 | `8ddb358` | 120/403 | BREAKX#, NSPAN, BOL, EOL, CAPTURE-IMM/CAPTURE-COND + Ω discipline fix |

---

## Sprint 9 — COMPLETE ✅

All items done:
- **BREAKX#** engine node with correct backtrack/retry Ω discipline
- **NSPAN** (0-or-more span)
- **BOL / EOL** zero-width anchors
- **CAPTURE-IMM ($) / CAPTURE-COND (.)** split

**Key bug fixed**: CAPTURE's `:succeed` was calling `(🡧 Ω)`, which discarded
child retry frames (e.g. from BREAKX#). Fix: leave Ω untouched on `:succeed`;
only pop on `:fail/:recede` to remove the CAPTURE frame itself.

**Still pending from Sprint 9**: **BAL** — balanced parentheses. Still a stub.
Carrying forward into Sprint 10.

---

## Sprint 10 — NEXT (Operator Completeness + BAL)

### 10.1  BAL (carried from Sprint 9)
Balanced parentheses matcher. Multi-yield: succeeds at each position where
the text from the start to that point has balanced `(` and `)`.
- Needs a new engine node `BAL!` that iterates forward, tracking depth.
- On `:recede`, advances to the next balanced position.
- Reference: Snobol4.Net `Function/Pattern/Bal.cs`

### 10.2  `~P` optional
`INVOKE 'tilde` → `(ALT P ε)`. Wire in the INVOKE dispatch table.

### 10.3  `P & Q` conjunction
Both P and Q must match the same span from the same position.
Emit as `(CONJ P Q)` engine node.

### 10.4  `@N` cursor assignment (immediate)
Capture the current cursor position into variable N during the match.
Emit as `(CURSOR-IMM N)` engine node.

### 10.5  `*expr` deferred guard
Wrap an expression in a pattern node that evaluates at match time, not
construction time. Fixes the known **EQ guard pruning bug**:
`(EQ N 2)` inside an ALT branch is currently evaluated eagerly at
pattern-construction time instead of match time.

---

## Sprint 11 Plan — Data Structures

TABLE/ARRAY indexed access, CONVERT, PROTOTYPE, DATA DATATYPE dispatch, FIELD.

---

## Sprint 12 Plan — I/O & Runtime

FRETURN, NRETURN, APPLY, ENDFILE/REWIND/DETACH, END label.

---

## Sprint 13 Plan — Full Program Validation

Port a reference SNOBOL4 program end-to-end and validate output.

---

## Key Semantic Notes (hard-won)

### BREAK vs BREAKX
- `BREAK(cs)`: scans to first char in cs, does NOT retry on backtrack.
- `BREAKX(cs)`: scans to first char in cs; on backtrack, slides one char past
  each successive break-char and retries. (BreakX_014 is the canonical test.)

### FENCE semantics
- `FENCE(P)`: commits to P's match; backtracking INTO P blocked; outer ALT OK.
- `FENCE()` bare: any backtrack past this point aborts the entire match (nil).
  Implemented by pushing `:ABORT` sentinel onto Ω.

### $ vs . capture operators
- `P $ V` — CAPTURE-IMM: assigns V immediately when P matches, unconditionally.
- `P . V` — CAPTURE-COND: assigns V only when the full MATCH succeeds.
  (Currently both assign immediately — deferred-assign infra still pending.)

### Ω discipline for wrapper nodes
Any engine node that wraps a child and pushes itself onto Ω at `:proceed`
must NOT pop Ω on `:succeed`. Popping on `:succeed` discards retry frames
that the child may have pushed (e.g. BREAKX#). Only pop Ω on `:fail/:recede`
to remove the wrapper's own frame. This is the lesson from the BREAKX#+CAPTURE bug.

### Engine frame structure
Frame ζ is a 7-vector: `[Σ Δ σ δ Π φ Ψ]`
- Σ = remaining subject chars at entry to this node
- Δ = cursor position at entry
- σ = remaining subject chars now (after match)
- δ = cursor position now
- Π = pattern node (the list/symbol)
- φ = child index (slot 5) — BREAKX# reuses this as retry-position
- Ψ = parent node stack (for returning to parent via ζ↑)

Ω = backtrack choice stack (separate from Ψ).

Accessors: `ζΣ ζΔ ζσ ζδ ζΠ ζφ`
`full-subject` = complete original subject string (closed over by engine loop).

### Namespace isolation
`GLOBALS` must be called once in the user's namespace before any match or
variable operations. Tests call it in a `:each` fixture:
```clojure
(use-fixtures :each (fn [f] (GLOBALS (find-ns 'my.test.ns)) (f)))
```
