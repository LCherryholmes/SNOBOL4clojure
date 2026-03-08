# SNOBOL4clojure — Master Plan & Handoff Document

> **For a new Claude session**: Read this file, then `ASSESSMENT.md` for the
> full feature matrix. Transcripts are at `/mnt/transcripts/journal.txt`.
> Start with `lein test` to confirm baseline.

---

## What This Project Is

A complete SNOBOL4 implementation in Clojure. The goal is full semantic
fidelity with the SNOBOL4 standard — not a regex wrapper, but a proper
pattern-match engine with backtracking, captures, alternation, and the full
SNOBOL4 pattern calculus.

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
| Sprint 9 | WIP | 120/403 | BREAKX#, NSPAN, BOL, EOL, CAPTURE-IMM/CAPTURE-COND — 1 test failing |

---

## Sprint 9 — Current Status (WIP, UNCOMMITTED)

### What was implemented this session

1. **`BREAKX#` engine node** (`match.clj`) — backtracking form of BREAK.
   On `:proceed`: scans to first break-set char, pushes retry frame onto Ω
   with next-position stored in slot 5 (φ).
   On `:recede`: pops retry frame, advances one past the previous break char,
   scans again. This is the SNOBOL4 "slide" behaviour.

2. **`NSPAN$` primitive** (`primitives.clj`) — 0-or-more span (SPAN requires ≥1).

3. **`BOL#` / `EOL#` primitives** (`primitives.clj`) — zero-width anchors.
   BOL succeeds only at Δ=0; EOL succeeds only when Σ is empty.

4. **`CAPTURE-IMM` engine node** (`match.clj`) — immediate assignment (`$` operator).
   Assigns matched text to a variable as soon as inner pattern P matches,
   even if the overall match later fails.

5. **`CAPTURE-COND` engine node** (`match.clj`) — conditional assignment (`.` operator).
   Currently behaves same as CAPTURE-IMM (deferred-assign infra not yet built).
   True semantics: assign ONLY on overall match success.

6. **`operators.clj`** — `$` now emits `CAPTURE-IMM`, `.` now emits `CAPTURE-COND`.

7. **`patterns.clj`** — added `NSPAN`, `BOL`, `EOL`; `BREAKX` now emits `BREAKX#`.

8. **`core.clj`** — re-exports `NSPAN`, `BOL`, `EOL`.

### Known failing test: `breakx-014-canonical-discriminator`

The canonical BREAK vs BREAKX discriminator from Snobol4.Net BreakX_014:

```
subject = "EXCEPTIONS-ARE-AS-TRUE-AS-RULES"
pattern = POS(0) BREAKX('A') . R2  'AS'
expected = SUCCESS, match = [0,17], R2 = "EXCEPTIONS-ARE-"
```

BREAKX# is returning nil. The `:recede` retry logic in `match.clj` may have a
frame-bookkeeping bug. The `:recede` branch rebuilds `Σ-retry` from
`(drop retry-Δ (seq full-subject))` — verify that `ζ↑` is being called with
the correct frame and that `full-subject` is in scope at that point.

**To debug**: add `println` to the BREAKX# `:proceed` and `:recede` cases in
the engine loop in `match.clj`.

---

## Sprint 9 — Remaining Work

1. **Fix BREAKX# `:recede` bug** — make `breakx-014-canonical-discriminator` pass.
2. **CAPTURE-COND deferred semantics** — collect pending assigns in an atom,
   commit on overall match success, discard on failure. Low priority; document
   as known gap until Sprint 10.
3. **BAL** — still a stub. Multi-yield balanced parentheses matching.

---

## Sprint 10 Plan — Operator Completeness

1. `~P` optional — `(ALT P ε)` in INVOKE table
2. `P & Q` conjunction — both P and Q must match the same span
3. `@N` cursor assignment — immediate position capture during match
4. `*expr` deferred guard — fixes `(EQ N 2)` eager-evaluation bug in ALT

**Known open issue**: `(EQ N 2)` inside an ALT branch is evaluated eagerly
at pattern-construction time. Requires Sprint 10 `*expr` deferred guard.

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
  (Not yet fully distinguished; deferred-assign infrastructure pending.)

### Engine frame structure
Frame ζ is a 7-vector: `[Σ Δ σ δ Π φ Ψ]`
- Σ = remaining subject chars at entry to this node
- Δ = cursor position at entry
- σ = remaining subject chars now (after match)
- δ = cursor position now
- Π = pattern node (the list/symbol)
- φ = child index (slot 5) — BREAKX# reuses this as retry-position
- Ψ = parent backtrack stack

Accessors: `ζΣ ζΔ ζσ ζδ ζΠ ζφ`  
`full-subject` = the complete original subject string (closed over by the engine loop).

### Namespace isolation
`GLOBALS` must be called once in the user's namespace before any match or
variable operations. Tests call it in a `:each` fixture:
```clojure
(use-fixtures :each (fn [f] (GLOBALS (find-ns 'my.test.ns)) (f)))
```
