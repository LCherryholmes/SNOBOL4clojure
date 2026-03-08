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
| Sprint 14  | `f98f779`  | 220/548 | SPITBOL harness, SEQ concat fix, divide guard, trace removal |

**Current baseline**: 220 tests / 548 assertions / 0 failures

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
| 1 | CAPTURE-COND deferred semantics — `.` assigns immediately like `$`; deferred-assign infra not yet built | match.clj |
| 2 | ANY(multi-arg) inside EVAL string — ClassCastException | operators.clj |
| 3 | File I/O — DETACH, REWIND, ENDFILE are stubs | functions.clj |
| 4 | Charset range expansion — `ANY("A-Z")` treats `-` as literal | primitives.clj |
| 6 | PDD field write when accessor name shadows Clojure fn (e.g. REAL) | operators.clj |
| 7 | `1 / 0` — integer divide by zero hangs in harness (zero? on non-numeric) | operators.clj |
| 8 | Pattern replace `S PAT = R` drops unmatched prefix of S | operators.clj / match.clj |

---

## Sprint 12 — Data Types & Conversion  ✅ COMPLETE (206/529)

### 12.1  CONVERT  ✅
Full coercion matrix: STRING/INTEGER/REAL/NAME → STRING/INTEGER/REAL/PATTERN/NAME,
ARRAY(Nx2) ↔ TABLE, same-type identity, all others return nil (failure).
Overflow/parse failures → nil → :F branch.

### 12.2  DATA / FIELD  ✅
PDD instances are maps `{:__type__ "TYPE", "F1" v1, ...}` — no defrecord needed.
DATA registers type in `data-type-registry`, installs constructor+accessor fns
via `snobol-set!` into the active SNOBOL namespace.
FIELD(type, n) returns nth field name. Accessor write `F(X) = val` handled via
INVOKE `=` branch detecting fn container and rebinding the instance variable.
DATATYPE :default updated to recognise `:__type__` maps.

### 12.3  SORT / RSORT  ✅
TABLE → sorted Nx2 ARRAY (ascending/descending by value string representation).

---

## Sprint 13 — I/O & Runtime  ✅ COMPLETE (220/548)

### Design decision (this sprint)
**All SNOBOL4 language keywords are UPPERCASE. No case folding. Ever.**
`:(RETURN)` `:(FRETURN)` `:(NRETURN)` `:(END)` `:S(x)` `:F(x)` — uppercase only.
Variable names are case-sensitive (`x` ≠ `X`) but that is the user's concern.
This was a deliberate simplification that removes an entire class of ambiguity.

### What was done
- `:(RETURN)` / `:(FRETURN)` / `:(NRETURN)` — exception-based signals
  (`snobol-return!` / `snobol-freturn!` / `snobol-nreturn!` in `env.clj`)
- DEFINE — full local variable save/restore; parses `'F(params)locals'`
- APPLY — call any function (built-in or DEFINE'd) by name-string
- `:(END)` / bare `end` label — halts execution cleanly
- `num` guarded against nil input (→ `##NaN`) — fixes NPE on wrong-arity calls
- Reverted grammar case-insensitive change (uppercase-only is the rule)

---

## Sprint 14 — SPITBOL Harness  ✅ COMPLETE (220/548)

### What was done
- `harness.clj`: `run-spitbol` / `run-clojure` / `diff-run` / `save-corpus!` / `load-corpus`
- `run-spitbol`: shells to `/usr/local/bin/spitbol -b -`; error output (exit≠0) goes to `:stderr`
- `run-clojure`: `with-out-str` + `future`/`deref` wall-clock timeout (5s); `reset-runtime!` clears compiler state between runs
- Status classification: `:pass` `:pass-class` `:fail` `:timeout` `:skip`
- SEQ concat fixed: `(SEQ "foo" "bar")` → `"foobar"` in expression context
- Removed spurious `(trace r)` from `?=` handler
- `divide` guarded against zero → nil (statement failure, matches SPITBOL)
- `num` guarded against nil → `##NaN`

### Smoke test results (8 cases)
| Program | Status |
|---------|--------|
| `OUTPUT = 'hello world'` | ✅ :pass |
| `OUTPUT = 3 + 4` | ✅ :pass |
| `OUTPUT = 'foo' 'bar'` | ✅ :pass |
| `GT(1,5)` (silent fail) | ✅ :pass |
| `OUTPUT = 'a' / OUTPUT = 'b'` | ✅ :pass |
| `DEFINE SQ / OUTPUT = SQ(7)` | ✅ :pass |
| Loop 1..5 | ✅ :pass |
| `OUTPUT = 1 / 0` | ⚠️ :timeout (div-by-zero hangs — bug #7) |
| `S 'world' = 'SNOBOL4'` | ❌ :fail (replace drops prefix — bug #8) |

### New bugs found by harness
| # | Issue | File |
|---|-------|------|
| 7 | `1 / 0` — integer divide by zero hangs (zero? fails on non-numeric result) | operators.clj |
| 8 | Pattern replace `S PAT = R` drops unmatched prefix of S | operators.clj / match.clj |

---

## Sprint 15 — Gimpel Corpus  ⬜ PLANNED

### Goal
Run the ~100 Gimpel SPITBOL algorithms through the harness; record
pass/fail; fix regressions found.

### Context
- Source: `/tmp/gimpel/SPITBOL/*.INC` and `*.SPT`
- These use `-INCLUDE` directives — harness must inline includes before
  passing to SNOBOL4clojure (SNOBOL4clojure has no preprocessor yet)
- Many programs use `OPSYN`, `LOAD`, file I/O — these will fail; that is
  expected and should be recorded as known-skip, not regression

### Tasks
- [ ] 15.1  Include inliner — `tools/inline-includes.sh` or Clojure fn
      that recursively expands `-INCLUDE 'file'` directives
- [ ] 15.2  Batch runner — iterate all `*.SPT` files; run harness; write
      `reports/gimpel-results.edn` with `{:file :spitbol-out :clojure-out :status}`
- [ ] 15.3  Triage results — categorise each failure:
      (a) missing built-in, (b) I/O, (c) OPSYN/LOAD, (d) genuine bug
- [ ] 15.4  Fix category (d) bugs; re-run; iterate
- [ ] 15.5  Simple standalone programs that pass become permanent regression tests
- [ ] 15.6  Commit corpus results + any fixes

### Programs most likely to work early (no I/O, no OPSYN)
`HSORT.INC`, `BSORT.INC`, `MSORT.INC`, `SSORT.INC`, `LSORT.INC`,
`TSORT.INC`, `FRSORT.INC`, `REVERSE.INC`, `ROMAN.INC`, `HEX.INC`,
`BASE10.INC`, `BASEB.INC`, `COMB.INC`, `PERM.INC`, `FACTORIAL` variants

---

## Sprint 16 — Shafto AI Corpus  ⬜ PLANNED

### Goal
Run the Shafto AI programs (SNOLISPIST, Wang, ATN, Kalah, HSORT) through
the harness with sample input files.

### Context
- Source: `/tmp/aisnobol/*.SPT`
- Input files: `*.IN` (e.g. `WANG.IN`, `ATN.IN`, `HSORT.IN`)
- These are larger, more complex programs; many use SNOLISPIST core/library
- SPITBOL versions (`.SPT`) are the ones to target

### Tasks
- [ ] 16.1  Run `WANG.SPT` + `WANG.IN` through harness (theorem prover — pure computation)
- [ ] 16.2  Run `HSORT.SPT` + `HSORT.IN` (standalone sort — simplest)
- [ ] 16.3  Run `ENDING.SPT` + `ENDING.IN` (word endings — string manipulation)
- [ ] 16.4  Attempt `TEST.SPT` (SNOLISPIST test suite — needs core + lib inlined)
- [ ] 16.5  Fix regressions found; commit

---

## Sprint 17 — Grammar-Based Test Generator  ⬜ PLANNED

### Design — two-tier (from Expressions.py reference)

The generator mirrors the structure of `Expressions.py` (L. Cherryholmes):

**Tier 1 — `rand-*` probabilistic sampling** (weighted toward terminals):
```clojure
(rand-expr)    ; random expression, biased 70% terminal / 30% recurse
(rand-stmt)    ; random statement
(rand-program) ; 1..N statements + "end"
```

**Tier 2 — `gen-*` exhaustive lazy sequences** (systematic, every form):
```clojure
(gen-literal)  ; "42", "3.14", "'hello'", "''"
(gen-varname)  ; "A", "B", "X", "Y"  (short uppercase names)
(gen-expr)     ; lazy-seq of all expressions at increasing depth
(gen-pattern)  ; LEN(n), ANY('abc'), literal, concatenation
(gen-stmt)     ; assignment | match | match-replace | bare subject
(gen-program)  ; lazy-seq of complete programs
```

**Outcome schema** — captures ALL possible outcomes, not just pass/value:
```clojure
{:src        "        X = 1/0\nend"
 :spitbol    {:stdout "" :stderr "...division by zero..." :exit 1}
 :clojure    {:stdout "" :thrown "ArithmeticException" :exit :error}
 :status     :pass          ; :pass :pass-class :fail :timeout :skip
 :length     7
 :depth      2}
```

**Status classes:**
- `:pass`       — stdout identical
- `:pass-class` — both errored (messages may differ)
- `:fail`       — one succeeded, one failed (or different output)
- `:timeout`    — either side exceeded `&STLIMIT` (recorded, not a failure)
- `:skip`       — SPITBOL itself crashed (discard from corpus)

**Termination guard**: inject `&STLIMIT = 10000` at top of every generated program.

### Tasks
- [ ] 17.1  `src/SNOBOL4clojure/generator.clj` — `rand-*` + `gen-*` fns
- [ ] 17.2  `src/SNOBOL4clojure/harness.clj` — `run-spitbol` / `run-clojure` / `diff-run`
- [ ] 17.3  Length-filtered sampler — `(sample-programs depth-range length-bands K)`
- [ ] 17.4  Corpus store — `resources/golden-corpus.edn`
- [ ] 17.5  Corpus test loader — auto-generates `deftest` per golden entry
- [ ] 17.6  Initial run: depth 1-4, all length bands, K=50 → ~1000 programs
- [ ] 17.7  Commit corpus + generator

---

## Sprint 18 — I/O & File Channels  ⬜ PLANNED

### Goal
Real file I/O: INPUT/OUTPUT channel association, ENDFILE, REWIND, DETACH.
Required before many Gimpel programs can run end-to-end.

### Tasks
- [ ] 18.1  `INPUT(varname, channel, filename)` — associate var with file read
- [ ] 18.2  `OUTPUT(varname, channel, filename)` — associate var with file write
- [ ] 18.3  Channel read — on `$$ 'varname'` for an input-associated var,
      read next line from file
- [ ] 18.4  `ENDFILE(channel)` — close + signal EOF
- [ ] 18.5  `REWIND(channel)` — seek to start of file
- [ ] 18.6  `DETACH(varname)` — disassociate variable from channel
- [ ] 18.7  Re-run Gimpel batch; record new pass count
- [ ] 18.8  Commit

---

## Sprint 19 — OPSYN & LOAD  ⬜ PLANNED

### Goal
`OPSYN` (operator/function aliasing) and `LOAD` (external function load).
Required for several Gimpel programs that redefine operators.

### Tasks
- [ ] 19.1  `OPSYN(new, old, nargs)` — create alias in INVOKE dispatch table
- [ ] 19.2  `LOAD(spec, libpath)` — stub that fails gracefully (real DL load
      is out of scope; many Gimpel SPT versions avoid OPSYN already)
- [ ] 19.3  Re-run Gimpel batch; record improvement
- [ ] 19.4  Commit

---

## Sprint 20 — Full Validation & Release  ⬜ PLANNED

### Goal
Passing rate ≥ 80% of Gimpel programs that don't require LOAD.
Golden corpus ≥ 5 000 programs, all green.
PLAN.md complete. README written. Tag v1.0.

### Tasks
- [ ] 20.1  Final Gimpel batch run; triage all remaining failures
- [ ] 20.2  Final Shafto batch run
- [ ] 20.3  Generator: depth 5-6, expand corpus to ~6 000
- [ ] 20.4  All open issues resolved or explicitly deferred with rationale
- [ ] 20.5  README.md written: what it is, how to run, known limitations
- [ ] 20.6  Tag v1.0, push

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
