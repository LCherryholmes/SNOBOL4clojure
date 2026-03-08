# SNOBOL4clojure — Master Plan & Handoff Document

> **For a new Claude session**: Read this file first, then run `lein test` to
> confirm the baseline. The tradeoff prompt is at the bottom — read it before
> making any design decisions.

---

## What This Project Is

A complete SNOBOL4 implementation in Clojure. Full semantic fidelity with the
SNOBOL4 standard — not a regex wrapper, but a proper pattern-match engine with
backtracking, captures, alternation, and the full SNOBOL4 pattern calculus.
Data structures (TABLE, ARRAY), a GOTO-driven runtime, and a compiler from
SNOBOL4 source text to a labeled statement table.

**Repository**: https://github.com/LCherryholmes/SNOBOL4clojure.git  
**Location on disk**: `/home/claude/SNOBOL4clojure`  
**Test runner**: `lein test` (Leiningen 2.12.0, Java 21)

---

## Reference Material

### On-disk locations

| Path | Contents |
|------|----------|
| `/home/claude/SNOBOL4python` | Python reference implementation (primary algorithmic reference) |
| `/home/claude/Snobol4.Net/Snobol4.Net-feature-msil-trace/` | .NET implementation + test suite |
| `/home/claude/x64ref/x64-main/` | C SPITBOL x64 reference |
| `/home/claude/snobol4ref/snobol4-2.3.3/` | C SNOBOL4 reference |

### GitHub repositories

| Repo | URL | Notes |
|------|-----|-------|
| SNOBOL4clojure | https://github.com/LCherryholmes/SNOBOL4clojure.git | **This project** |
| SNOBOL4python | https://github.com/LCherryholmes/SNOBOL4python.git | Python port — best source for algorithmic detail |
| SNOBOL4csharp | *(URL unknown — ask user to confirm)* | C# port — confirm URL with user |
| Snobol4.Net | *(local only — no git remote found on disk)* | .NET impl + exhaustive test suite in `TestSnobol4/` |

> **Ground truth for edge cases**: The Snobol4.Net test suite at
> `TestSnobol4/Function/` — one `.cs` file per primitive/function.
> Key subdirs: `Pattern/`, `ArraysTables/`, `ProgramDefinedDataType/`.

---

## Core Design Principle

User code calls `(GLOBALS *ns*)` once. All SNOBOL variables live in that one
user namespace. The library never owns variables; it operates on whatever
namespace the user handed it via `env/GLOBALS`, `env/active-ns`,
`env/snobol-set!`, `env/$$`.

---

## File Map

| File | Responsibility |
|------|----------------|
| `env.clj` | globals, DATATYPE, NAME/SnobolArray deftypes, `$$`/`snobol-set!`, arithmetic, TABLE/ARRAY constructors, `GLOBALS` |
| `primitives.clj` | low-level scanners: LIT$, ANY$, SPAN$, NSPAN$, BREAK$, BREAKX$, POS#, RPOS#, LEN#, TAB#, RTAB#, BOL#, EOL# |
| `match.clj` | MATCH state machine engine + SEARCH/MATCH/FULLMATCH/REPLACE/COLLECT! public API |
| `patterns.clj` | pattern constructors: ANY, SPAN, NSPAN, BREAK, BREAKX, BOL, EOL, POS, ARBNO, FENCE, ABORT, REM, BAL, CURSOR, CONJ, DEFER |
| `functions.clj` | built-in fns: REPLACE, SIZE, DATA, DATA!, ASCII, CHAR, REMDR, INTEGER, REAL, STRING, INPUT, ITEM, PROTOTYPE |
| `grammar.clj` | instaparse grammar + parse-statement/parse-expression |
| `emitter.clj` | AST to Clojure IR transform |
| `compiler.clj` | CODE!/CODE: source text to labeled statement table |
| `operators.clj` | operators (?, =, |, $, ., +...), EVAL/EVAL!/INVOKE, comparison primitives |
| `runtime.clj` | RUN: GOTO-driven statement interpreter |
| `core.clj` | thin facade with explicit def re-exports of full public API |

---

## Sprint History

| Sprint | Commit | Tests | What Was Done |
|--------|--------|-------|---------------|
| Stage 6 | `6e9683d` | baseline | Runtime polish, milestones 6A-6E |
| Stage 7 | `26a9b25` | -- | FENCE implementation, namespace isolation |
| Stage 7b | `99b2563` | -- | DATATYPE returns PATTERN for list nodes |
| Stage 7c | `4813ae8` | 82/314 | ARB and ARBNO engine implementation |
| Sprint 8 | `69a6f48` | 102/379 | ABORT!, bare FENCE(), REM, ASCII, CHAR, REMDR, INTEGER, REAL, STRING |
| Sprint 9 | `8ddb358` | 120/403 | BREAKX#, NSPAN, BOL, EOL, CAPTURE-IMM/COND, Omega discipline fix |
| Sprint 9b | `1a69b69` | 127/411 | BAL engine node + COLLECT! multi-yield utility |
| Sprint 10 | `5a89477` | 139/431 | ~P optional, @N cursor, CONJ P&Q, *expr deferred |
| Sprint 11a | `506d66f` | 151/447 | TABLE: atom-backed, subscript read/write <>/[], ITEM, PROTOTYPE |
| Sprint 11b | `d75986c` | 166/467 | ARRAY: SnobolArray, multi-dim, bounds-checked, default value, PROTOTYPE |

**Current baseline**: 166 tests / 467 assertions / 0 failures

---

## Key Semantic Notes (hard-won)

### BREAK vs BREAKX
- `BREAK(cs)`: scans to first char in cs, does NOT retry on backtrack.
- `BREAKX(cs)`: scans to first char in cs; on backtrack, slides one char past
  each successive break-char and retries. (BreakX_014 is the canonical test.)

### FENCE semantics
- `FENCE(P)`: commits to P's match; backtracking INTO P blocked; outer ALT OK.
- `FENCE()` bare: any backtrack past this point aborts the entire match (nil).

### $ vs . capture operators
- `P $ V` -- CAPTURE-IMM: assigns V immediately when P matches.
- `P . V` -- CAPTURE-COND: assigns V only when the full MATCH succeeds.
  (Currently both assign immediately -- deferred-assign infra still pending.)

### Omega discipline for wrapper nodes
Any engine node that wraps a child must NOT pop Omega on :succeed. Only pop on
:fail/:recede to remove the wrapper's own frame. Popping on :succeed discards
child retry frames (e.g. BREAKX#+CAPTURE bug, Sprint 9).

### Engine frame structure
Frame zeta is a 7-vector: [Sigma Delta sigma delta Pi phi Psi]
Omega = backtrack choice stack. Accessors: zetaSigma zetaDelta zetasigma zetadelta zetaPi zetaphi

### TABLE semantics (Sprint 11a)
- `(TABLE)` returns `(atom {})` -- mutable identity, reference semantics.
- `A<key>` / `A[key]` parse correctly when indented (body, not label).
- Subscript write `(= (A key) val)` detected in INVOKE = branch.
- `ITEM(t, k)` is an alias for subscript read.
- `DATATYPE` dispatches on `clojure.lang.Atom` -> "TABLE".

### ARRAY semantics (Sprint 11b)
- `SnobolArray` defrecord: `dims` (vec of [lo hi]), `dflt`, `data` (atom).
- `array(N)` -> dim [1..N]; `array('lo:hi,N')` -> explicit bounds.
- Out-of-bounds subscript returns nil -> statement fails -> :F branch taken.
- `PROTOTYPE(arr)` returns normalized "lo:hi,..." string.
- Multi-dimensional: `A<i,j>` works via multi-arg subscript grammar.

### Subscript assignment grammar note
`A<3> = val` only parses correctly when **indented** (leading whitespace).
Without leading whitespace, the label regex grabs `A<3>` as a label.
This is not a bug -- all SNOBOL4 statement bodies are indented in practice.

### goto case-sensitivity note
The grammar requires uppercase `:S(label)` and `:F(label)`.
The Snobol4.Net test suite uses lowercase `:s(label)` -- these will emit
parse errors in our compiler. This is a known gap (not yet fixed).

### Namespace isolation
`GLOBALS` must be called once in the user's namespace before any match or
variable operations. Tests call it in a `:each` fixture:

```clojure
(use-fixtures :each (fn [f] (GLOBALS (find-ns 'my.test.ns)) (f)))
```

---

## Open Issues / Known Gaps

| # | Issue | File |
|---|-------|------|
| 1 | CAPTURE-COND deferred semantics -- . assigns immediately like $; deferred-assign infra not yet built | match.clj |
| 2 | ANY(multi-arg) inside EVAL string -- ClassCastException | operators.clj |
| 3 | File I/O -- DETACH, REWIND, ENDFILE are stubs | functions.clj |
| 4 | charset range expansion -- (charset "A-Za-z") treats - as literal | primitives.clj |
| 5 | goto case-insensitive -- :s(x) / :f(x) lowercase not parsed | grammar.clj |

---

## Sprint 12 Plan -- Data Types & Conversion

### 12.1  CONVERT
`CONVERT(x, type)` -- coerce a value to a named SNOBOL4 type.
Currently a stub returning x. Needs dispatch on type string:
"STRING", "INTEGER", "REAL", "ARRAY", "TABLE", "PATTERN".

### 12.2  DATA / FIELD (program-defined data types)
`DATA('POINT(X,Y)')` defines a constructor POINT and accessors X, Y.
DATA! already emits the deftype -- needs FIELD to wire accessors and
DATATYPE to recognise instances.
Reference: TestSnobol4/Function/ProgramDefinedDataType/

### 12.3  SORT / RSORT
Sort a TABLE by value (or key). Currently stubs returning nil.

---

## Sprint 13 Plan -- I/O & Runtime

FRETURN, NRETURN, APPLY, ENDFILE/REWIND/DETACH, END label,
goto case-insensitive fix.

---

## Sprint 14 Plan -- Full Program Validation

Port a non-trivial reference SNOBOL4 program end-to-end and validate output
against the C or .NET reference implementation.

---

## Tradeoff Prompt

> **Read this before every design decision.**

This codebase makes consistent choices that future sessions must honour:

**1. Single-file structure is intentional.**
`match.clj` contains the entire engine in one loop/case. The `engine`
function cannot be split across files because `recur` requires all targets
to be in the same function body. An attempt to refactor was made in Sprint 10
and immediately reverted. Do not attempt to split the engine again.

**2. Immutable-by-default, mutable-by-atom.**
Clojure values are immutable. SNOBOL4 mutable containers (TABLE, ARRAY) use
`atom` as the single point of mutation. All other values (strings, integers,
patterns) are passed by value. This is correct and intentional.

**3. The label/body whitespace contract.**
SNOBOL4 source is whitespace-sensitive at the statement level: labels are
flush-left (or at column 1), bodies are indented. Our compiler does NOT strip
leading whitespace before passing to `parse-statement`. This means subscript
expressions like `A<3>` only parse as statement subjects when indented.
Tests must always indent statement bodies.

**4. INVOKE is the single dispatch point.**
All SNOBOL4 built-in functions go through INVOKE's case table. If you add
a new function to functions.clj, you must also add a case (both lowercase
and uppercase) in operators.clj's INVOKE. Do not rely on the default
`($$ op)` fallthrough for built-ins -- it looks in the user namespace, not
the library.

**5. nil means failure; epsilon means empty string.**
Throughout the engine and runtime, nil signals pattern match failure or
statement failure. epsilon (empty string "") is a valid SNOBOL4 value.
Never confuse the two. array-get returning nil means out-of-bounds;
table-get returning epsilon means key-not-found.
