# SNOBOL4clojure — Master Plan & Handoff Document

> **For a new Claude session**: Read this file first, then run `lein test` to
> confirm the baseline. The Tradeoff Prompt is at the bottom — read it before
> making any design decisions.

---

## What This Project Is

A complete SNOBOL4/SPITBOL implementation in Clojure. Full semantic fidelity
with the SNOBOL4 standard — not a regex wrapper, but a proper pattern-match
engine with backtracking, captures, alternation, and the full SNOBOL4 pattern
calculus. Data structures (TABLE, ARRAY), a GOTO-driven runtime, and a
compiler from SNOBOL4 source text to a labeled statement table.

**Repository**: https://github.com/LCherryholmes/SNOBOL4clojure.git
**Author**: Larry Cherryholmes (lcherryh@yahoo.com, GitHub: LCherryholmes)
**Location on disk**: `/home/claude/SNOBOL4clojure`
**Test runner**: `lein test` (Leiningen 2.12.0, Java 21, OpenJDK 64-bit)

---

## Session Log

| Date | Baseline | What Happened |
|------|----------|---------------|
| 2026-03-08 | 220/548/0 | Repo cloned fresh; lein installed; baseline confirmed. PLAN.md rewritten. SPITBOL x64-main.zip and CSNOBOL4 snobol4-2.3.3.tar.gz uploaded and extracted to `/home/claude/spitbol-src/` and `/home/claude/csnobol4-src/`. Eureka grammar-worm harness design documented. |
| 2026-03-08 (session 2) | 220/548/0 | Gimpel.zip + Cooper zip extracted. `test_cooper.clj` (82 tests) and `test_bootstrap.clj` (130 tests, 7 length bands) written by hand. **Bug fixed**: PDD field-setter crash — `VAL(N) = x` was evaluating the instance arg to a map, losing the symbol name; fixed in `operators.clj` EVAL! `=` branch to keep raw symbol for fn? containers. **Bug fixed**: REPLACE multi-line continuation. **Key strategy decision**: Claude acts as the worm generator first — hand-write 1000 exhaustive primitive-first tests before building automated generator. `test_worm1000.clj` generated (521 tests, not yet triaged). Oracle binaries not installed — harness runs Clojure side only. **Next session**: run test_worm1000, triage failures, fix bugs to 0, then build automated 100k generator. |

---

## Design Decisions (Immutable)

1. **ALL UPPERCASE keywords.** No case folding. `:S(LABEL)` `:F(LABEL)` `:(RETURN)` `:(END)`. Always case-sensitive.
2. **Single-file engine.** `match.clj` is one `loop/case`. Cannot be split — `recur` requires all targets in the same function body. Do not refactor.
3. **Immutable-by-default, mutable-by-atom.** TABLE and ARRAY use `atom`. All other values passed by value.
4. **Label/body whitespace contract.** Labels flush-left, bodies indented. Compiler does NOT strip leading whitespace. Tests must always indent statement bodies.
5. **INVOKE is the single dispatch point.** All built-in functions go through INVOKE's case table. Add both lowercase and uppercase entries for every new function.
6. **nil means failure; epsilon means empty string.** `nil` = match/statement failure. `""` = valid empty SNOBOL4 value.
7. **`clojure.core/=` inside `operators.clj`.** That file excludes `clojure.core/=`. Bare `=` builds IR lists. Always use `clojure.core/=` or the `equal` alias for value comparisons. This tripped us up badly in Sprint 14 — `(= x 'SEQ)` built the list `(= x SEQ)` (truthy!) instead of returning false.
8. **INVOKE args are pre-evaluated.** EVAL! calls `(map EVAL! parms)` before INVOKE. Args in INVOKE are already evaluated. Never call EVAL! on them again.
9. **Two-tier generator discipline.** `rand-*` = probabilistic, fast, random coverage. `gen-*` = exhaustive lazy sequences, deterministic, complete. Keep both tiers.
10. **Typed pools are canonical fixtures.** Use `I J K L M N` for integers, `S T X Y Z` for strings, `P Q R` for patterns, `L1 L2` for labels. Pre-initialize fixtures at program start. Idiomatic naming makes failures readable.

---

## Reference Material — Source Archives

These two archives are the **ground truth** for the entire implementation.
Both have been uploaded by the user and extracted on disk.

| Archive | Extracted to | Purpose |
|---------|-------------|---------|
| `snobol4-2.3.3.tar.gz` | `/home/claude/csnobol4-src/` | CSNOBOL4 2.3.3 — oracle #1, authoritative C source |
| `x64-main.zip` | `/home/claude/spitbol-src/` | SPITBOL v4.0f x64 — oracle #2, highest-performance reference |

> **In a new session**: these archives are in `/mnt/user-data/uploads/`. Extract
> with `tar xzf snobol4-2_3_3_tar.gz -C /home/claude/csnobol4-src/` and
> `unzip x64-main.zip -d /home/claude/spitbol-src/` before consulting source.

### Additional Attachments Needed (ask user to upload when starting relevant sprint)

| Attachment | Purpose | Sprint |
|------------|---------|--------|
| `Snobol4.Net-feature-msil-trace.zip` | Jeffrey Cooper's test suite — gold mine of low/high level tests | Sprint 16 |
| `gimpel.zip` | Gimpel SPITBOL algorithm programs | Sprint 15 |
| `aisnobol.zip` | Shafto AI SNOBOL4 corpus (Wang, ATN, Kalah, SNOLISPIST) | Sprint 17 |
| `SNOCONE.zip` | Additional test corpus | Sprint 18+ |

> **Protocol**: At the start of each sprint that needs an archive, ask:
> *"Please upload `<file>` so I can proceed with Sprint N."*

### On-Disk Locations (persistent between sessions if container is warm)

| Path | Contents |
|------|----------|
| `/home/claude/csnobol4-src/` | CSNOBOL4 2.3.3 C source |
| `/home/claude/spitbol-src/` | SPITBOL v4.0f x64 source |
| `/home/claude/Snobol4.Net/Snobol4.Net-feature-msil-trace/` | Cooper .NET test suite (if uploaded) |
| `/tmp/gimpel/SPITBOL/` | Gimpel programs (if uploaded) |
| `/tmp/aisnobol/` | Shafto AI corpus (if uploaded) |

### Installed Oracles (if previously built/installed)

| Binary | Version | Invocation |
|--------|---------|------------|
| `/usr/local/bin/spitbol` | SPITBOL v4.0f | `spitbol -b -` (batch, stdin) |
| `/usr/local/bin/snobol4` | CSNOBOL4 2.3.3 | `snobol4 -` (stdin) |

Both are used by `harness.clj` for three-oracle triangulation.

---

## File Map

| File | Responsibility |
|------|----------------|
| `env.clj` | globals, DATATYPE, NAME/SnobolArray deftypes, `$$`/`snobol-set!`, arithmetic, TABLE/ARRAY constructors, `GLOBALS` |
| `primitives.clj` | scanners: LIT$, ANY$, SPAN$, NSPAN$, BREAK$, BREAKX$, POS#, RPOS#, LEN#, TAB#, RTAB#, BOL#, EOL# |
| `match.clj` | MATCH state machine engine + SEARCH/MATCH/FULLMATCH/REPLACE/COLLECT! public API |
| `patterns.clj` | pattern constructors: ANY, SPAN, NSPAN, BREAK, BREAKX, BOL, EOL, POS, ARBNO, FENCE, ABORT, REM, BAL, CURSOR, CONJ, DEFER |
| `functions.clj` | built-in fns: REPLACE, SIZE, DATA, DATA!, ASCII, CHAR, REMDR, INTEGER, REAL, STRING, INPUT, ITEM, PROTOTYPE |
| `grammar.clj` | instaparse grammar + parse-statement/parse-expression |
| `emitter.clj` | AST to Clojure IR transform |
| `compiler.clj` | CODE!/CODE: source text to labeled statement table |
| `operators.clj` | operators (?, =, \|, $, ., +...), EVAL/EVAL!/INVOKE, comparison primitives |
| `runtime.clj` | RUN: GOTO-driven statement interpreter |
| `core.clj` | thin facade with explicit re-exports of full public API |
| `harness.clj` | Three-oracle diff harness: SPITBOL / CSNOBOL4 / SNOBOL4clojure |
| `generator.clj` | Worm test generator: typed variable pools, rand-* and gen-* tiers |

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
| Sprint 12 | `3af1ffb` | 206/529 | CONVERT, DATA/FIELD, SORT/RSORT, COPY, DATATYPE |
| Sprint 13 | `1d88587` | 220/548 | RETURN/FRETURN/END, DEFINE locals, APPLY, uppercase-only rule |
| Sprint 14 | `8b75205` | 220/548 | Harness, CSNOBOL4 oracle, worm generator, 4 operator bugs fixed |
| Sprint 14.5 | `(pending)` | 220/548 | PLAN.md rewrite; source archives extracted; no code changes |

**Current baseline**: 220 tests / 548 assertions / 0 failures
**Harness baseline**: 80/80 generated programs passing
**Last confirmed**: 2026-03-08

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
- `P $ V` — CAPTURE-IMM: assigns V immediately when P matches.
- `P . V` — CAPTURE-COND: assigns V only when the full MATCH succeeds.
  (Currently both assign immediately — deferred-assign infra still pending.)

### Omega discipline for wrapper nodes
Any engine node that wraps a child must NOT pop Omega on :succeed. Only pop on
:fail/:recede to remove the wrapper's own frame. Popping on :succeed discards
child retry frames (e.g. BREAKX#+CAPTURE bug, Sprint 9).

### Engine frame structure
Frame zeta is a 7-vector: [Sigma Delta sigma delta Pi phi Psi]
Omega = backtrack choice stack. Accessors: zetaSigma zetaDelta zetasigma zetadelta zetaPi zetaphi

### TABLE semantics (Sprint 11a)
- `(TABLE)` returns `(atom {})` — mutable identity, reference semantics.
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
This is not a bug — all SNOBOL4 statement bodies are indented in practice.

### Goto case-sensitivity note
The grammar requires uppercase `:S(label)` and `:F(label)`.
The Snobol4.Net test suite uses lowercase `:s(label)` — these will emit
parse errors in our compiler. Known gap (Open Issue #6), not yet fixed.

### Namespace isolation
`GLOBALS` must be called once in the user's namespace before any match or
variable operations. Tests call it in a `:each` fixture:

```clojure
(use-fixtures :each (fn [f] (GLOBALS (find-ns 'my.test.ns)) (f)))
```

### Division by zero
`INVOKE /` throws `ExceptionInfo {:snobol/error 14}` on integer or real
divide-by-zero. This matches SPITBOL's fatal error 014. The harness classifies
both sides erroring as `:pass-class`.

### Pattern variables and double-EVAL trap
When EVAL! dispatches a list `(op ...)` through the `true` branch, it
pre-evaluates args with `(map EVAL! parms)` before calling INVOKE. So INVOKE
receives **already-evaluated** values. Do NOT call `EVAL!` again inside INVOKE
on values that came from args.

---

## Open Issues / Known Gaps

| # | Issue | File |
|---|-------|------|
| 1 | CAPTURE-COND deferred semantics — `.` assigns immediately like `$`; deferred-assign infra not yet built | match.clj |
| 2 | ANY(multi-arg) inside EVAL string — ClassCastException | operators.clj |
| 3 | File I/O — DETACH, REWIND, ENDFILE are stubs | functions.clj |
| 4 | Charset range expansion — `ANY("A-Z")` treats `-` as literal | primitives.clj |
| 5 | PDD field write when accessor name shadows Clojure fn (e.g. REAL) | operators.clj |
| 6 | Goto case folding — Snobol4.Net uses lowercase `:s(label)` which fails our parser | grammar.clj |

Issues 7 and 8 (div-by-zero, pattern replace prefix) were fixed in Sprint 14.

---

## Datatype Convention (Clojure -> SNOBOL4)

| Clojure type | SNOBOL4 DATATYPE | Notes |
|---|---|---|
| `java.lang.String` / `Character` | `"STRING"` | all text values |
| `java.lang.Long` | `"INTEGER"` | integer arithmetic |
| `java.lang.Double` | `"REAL"` | floating point |
| `clojure.lang.Atom` (wrapping a map) | `"TABLE"` | mutable associative table |
| `SNOBOL4clojure.env.SnobolArray` | `"ARRAY"` | multi-dim integer-subscripted |
| `clojure.lang.Symbol` | `"NAME"` | indirect reference (`.` operator result) |
| `SNOBOL4clojure.env.NAME` deftype | `"NAME"` | mutable named reference |
| `PersistentList` whose `first` is a pattern op | `"PATTERN"` | `(SEQ ...)`, `(ALT ...)`, etc. |
| `PersistentList` whose `first` is NOT a pattern op | `"EXPRESSION"` | unevaluated IR |
| `PersistentVector` | `"PATTERN"` | SEQ of pattern nodes |
| `PersistentTreeMap` / `Keyword` | `"CODE"` | compiled statement table entries |
| `PersistentHashSet` / `TreeSet` | `"SET"` | character sets (ANY, SPAN etc.) |
| `java.util.regex.Pattern` | `"REGEX"` | Java regex (internal) |
| `java.lang.Class` | `"DATA"` | type descriptor |
| map with `:__type__` key | user-defined type name | PDD instances from `DATA` |

Pattern op suffixes:
- `!` suffix (`FENCE!`, `ARBNO!`) — ops with side effects / special backtrack
- `#` suffix (`LEN#`, `POS#`) — numeric-argument primitives
- `$` suffix (`ANY$`, `SPAN$`) — character-set primitives

---

## Sprint 12 — Data Types & Conversion  COMPLETE (206/529)

CONVERT, DATA/FIELD, SORT/RSORT, COPY, DATATYPE. PDD instances are maps
`{:__type__ "TYPE", "F1" v1, ...}`. Full coercion matrix in CONVERT.

---

## Sprint 13 — Control Flow  COMPLETE (220/548)

RETURN/FRETURN/NRETURN/END signals, DEFINE with local save/restore,
APPLY, uppercase-only keyword rule.

---

## Sprint 14 — Harness, Oracles, Generator  COMPLETE (220/548, 80/80)

### Three-oracle harness (`harness.clj`)

```
SPITBOL v4.0f  (/usr/local/bin/spitbol -b -)   <- primary oracle
CSNOBOL4 2.3.3 (/usr/local/bin/snobol4 -)      <- secondary oracle
SNOBOL4clojure                                  <- our implementation
```

**Triangulation logic** (`oracle-stdout`):
- Both agree -> `:oracle :both` — use agreed stdout as reference
- Both agree but both errored -> `:oracle :both-error` — classify as `:pass-class` if we also error
- Only SPITBOL succeeded -> `:oracle :spitbol`
- Only CSNOBOL4 succeeded -> `:oracle :csnobol4`
- Both succeeded but disagree -> `:oracle :disagree` — use SPITBOL, flag for human review

**Status classes**:
- `:pass` — stdout identical to oracle
- `:pass-class` — both errored (messages may differ)
- `:fail` — genuine divergence
- `:timeout` — wall-clock timeout (5s, via `future`/`deref`)
- `:skip` — both oracles crashed (bad input — discard)

**Corpus record schema**:
```clojure
{:src      "...snobol4 source..."
 :spitbol  {:stdout "" :stderr "" :exit 0}
 :csnobol4 {:stdout "" :stderr "" :exit 0}
 :clojure  {:stdout "" :stderr "" :exit :ok/:error/:timeout :thrown "..."}
 :oracle   :both | :spitbol | :csnobol4 | :disagree | :both-error
 :status   :pass | :pass-class | :fail | :timeout | :skip
 :length   (count src)
 :depth    nil}
```

**`reset-runtime!`** — must be called between harness runs; clears:
`env/STNO`, `env/<STNO>`, `env/<LABL>`, `env/<CODE>`.

**Key API**:
- `(run-spitbol src)` -> outcome map
- `(run-csnobol4 src)` -> outcome map
- `(run-clojure src)` -> outcome map (with wall-clock timeout)
- `(diff-run src)` -> full corpus record
- `(diff-run src depth)` -> same, with depth tag
- `(save-corpus! records)` -> appends to `resources/golden-corpus.edn`
- `(load-corpus)` -> vector of all records

### Worm generator (`generator.clj`)

**Typed variable pools**:

| Pool | Variables | Literals |
|------|-----------|---------|
| Integers | `I J K L M N` | `1 2 3 4 5 6 7 8 9 10 25 100` |
| Reals | `A B C D E F` | `1.0 1.5 2.0 2.5 3.0 3.14 0.5 10.0` |
| Strings | `S T X Y Z` | `'alpha' 'beta' 'gamma' 'hello' 'world' 'foo' 'bar' 'baz' 'SNOBOL' 'test'` |
| Patterns | `P Q R` | `'a' ANY('aeiou') SPAN('abc...z') LEN(1) LEN(2) LEN(3)` |
| Labels | `L1 L2 L3 ...` | (generated sequentially) |

**Worm state**:
```clojure
{:lines    []     ; accumulated source lines
 :live-int #{}    ; int vars assigned (safe to reference)
 :live-str #{}    ; string vars assigned
 :live-pat #{}    ; pattern vars assigned
 :labels   #{}    ; labels that exist (safe to branch to)
 :next-lbl 1}     ; counter for L1, L2, ...
```

**14 weighted move types**:

| Move | Weight | Needs | What it emits |
|------|--------|-------|---------------|
| `move-assign-int` | 10 | nothing | `I = 42` |
| `move-assign-real` | 8 | nothing | `A = 3.14` |
| `move-assign-str` | 10 | nothing | `S = 'hello'` |
| `move-output-lit` | 6 | nothing | `OUTPUT = 'foo'` |
| `move-pat-assign` | 5 | nothing | `P = LEN(3)` |
| `move-arith` | 8 | live int | `I = I + 3` |
| `move-concat` | 8 | live str | `S = S 'world'` |
| `move-output-int` | 7 | live int | `OUTPUT = I` |
| `move-output-str` | 7 | live str | `OUTPUT = S` |
| `move-cmp-branch` | 5 | live int | `GT(I,5) :S(L1)F(L2)` + label stubs |
| `move-pat-match` | 4 | live str | `S LEN(2) :S(L1)F(L2)` + label stubs |
| `move-pat-replace` | 4 | live str | `S LEN(2) = 'x'` then `OUTPUT = S` |
| `move-size` | 3 | live str | `OUTPUT = SIZE(S)` |
| `move-loop` | 3 | nothing | full counted loop |

**Tier 1 - `rand-*` probabilistic**:
```clojure
(rand-program)        ; 3-8 moves, weighted random
(rand-program n)      ; exactly n moves
(rand-batch n)        ; n independent programs
```

**Tier 2 - `gen-*` exhaustive lazy sequences**:
```clojure
(gen-assign-int)   ; all (var = lit) for every int-var x int-lit
(gen-assign-str)   ; all (var = lit) for every str-var x str-lit
(gen-arith)        ; all (var = lhs op rhs) for every combo
(gen-concat)       ; all (var = s1 s2) for every str-lit pair
(gen-cmp)          ; all (OP(lhs,rhs) :S(YES)F(NO)) programs
(gen-pat-match)    ; all (S pat :S(HIT)F(MISS)) programs
(systematic-batch) ; lazy concat of all gen-* sequences
```

---

## Eureka — Grammar-Worm Bootstrap Test Suite

> **Inspiration**: `Expressions.py` in project knowledge demonstrates the
> two-tier approach for arithmetic expressions — probabilistic `rand_*` and
> exhaustive lazy `gen_*`. Apply the same idea to SNOBOL4 statements, growing
> from 0 characters up through length 64 one character at a time, covering
> every grammatical construct at each length.

### Canonical Variable Pools for Generated Programs

| Pool | Names | Initial fixture values |
|------|-------|------------------------|
| Labels | `L1 L2` | branch targets only |
| Integers | `I J K L M N` | `I=0 J=1 K=2 L=3 M=4 N=5` |
| Reals | `A B C D E F` | `A=1.0 B=2.0 C=3.14 D=0.5 E=10.0 F=1.5` |
| Patterns | `P Q R` | `P=LEN(1)  Q=ANY('aeiou')  R=SPAN('abc')` |
| Strings | `S T X Y Z` | `S='hello' T='world' X='foo' Y='bar' Z='baz'` |

Do not generate total randomness. Idiomatic naming makes failures readable.

### Length-Band Test Strategy

Generate SNOBOL4 programs systematically by source length (0-64 chars),
incrementing complexity practically one character at a time:

| Length | Constructs |
|--------|-----------|
| 0 | empty program (just `END`) |
| 1 | `\n` only |
| 3 | `" S\n"` — subject, no pattern |
| 4 | `"L S\n"` — label, no body |
| ~10 | `" S = 'x'\n"` — assignment |
| ~16 | `" S PAT\n"` — subject + pattern |
| ~20 | `" S PAT :S(L1)\n"` — conditional goto |
| ~30 | `" S PAT = REP\n"` — pattern replace |
| ~40 | `"L1 S PAT :S(L1)F(END)\n"` — loop |
| ~50 | multiple statements, control flow |
| ~64 | `" I = LT(I,10) I+1 :S(L1)\n"` style |

### Error Classes to Exercise at Each Length Band

- Syntax error (malformed label, invalid op)
- Semantic error (undefined variable in pattern, undefined GOTO target)
- Runtime exception (division by zero, out-of-bounds subscript)
- Normal success

All four classes are valid tests — the oracle determines expected behavior.

### Grammar-Worm Algorithm

```
for length in 0..64:
    generate all syntactically valid programs of exactly `length` chars
    (or up to N samples for longer lengths where the space is too large)
    run each through three-oracle harness
    record :pass/:fail/:skip in corpus with :length band
```

Produces the **bootstrap corpus** — thousands of tiny programs that cover
every grammatical path systematically, running fast enough for CI.

---

## Sprint 15 — Gimpel Corpus  PARTIAL

### Goal
Run the ~100 Gimpel SPITBOL algorithms through the harness; record
pass/fail; fix regressions found.

**Prerequisites**: Upload `gimpel.zip` at session start.

### Tasks
- [ ] 15.1  Include inliner — Clojure fn that recursively expands `-INCLUDE 'file'`
- [ ] 15.2  Batch runner — iterate all `*.SPT`; run harness; write `reports/gimpel-results.edn`
- [ ] 15.3  Triage — (a) missing built-in, (b) I/O, (c) OPSYN/LOAD, (d) genuine bug
- [ ] 15.4  Fix category (d) bugs; iterate
- [ ] 15.5  Simple standalones that pass -> permanent regression tests
- [ ] 15.6  Commit

### Programs most likely to work (no I/O, no OPSYN)
`HSORT.INC`, `BSORT.INC`, `MSORT.INC`, `SSORT.INC`, `LSORT.INC`,
`TSORT.INC`, `FRSORT.INC`, `REVERSE.INC`, `ROMAN.INC`, `HEX.INC`,
`BASE10.INC`, `BASEB.INC`, `COMB.INC`, `PERM.INC`, factorial variants

---

## Sprint 16 — Cooper / Snobol4.Net Test Suite  PARTIAL

### Goal
Mine Jeffrey Cooper's test suite for low/high-level test coverage. Generate
Clojure `deftest` entries from each `.cs` test class.

**Prerequisites**: Upload `Snobol4.Net-feature-msil-trace.zip` at session start.

### Tasks
- [ ] 16.1  Unpack; survey `TestSnobol4/Function/` — one `.cs` per primitive/fn
- [ ] 16.2  Translate selected `.cs` test methods to Clojure `deftest` blocks
- [ ] 16.3  Run; triage; fix
- [ ] 16.4  Commit `test/SNOBOL4clojure/test_cooper.clj`
- [ ] 16.5  Fix Open Issue #6 (goto case folding) during this sprint

---

## Sprint 17 — Shafto AI Corpus  PLANNED

`/tmp/aisnobol/` — Wang theorem prover, ATN parser, Kalah, SNOLISPIST.
Target `WANG.SPT`, `HSORT.SPT`, `ENDING.SPT` first (pure computation).

**Prerequisites**: Upload `aisnobol.zip` at session start.

---

## Sprint 18 — Grammar-Worm Bootstrap Suite  IN PROGRESS

### Goal
Implement the Eureka length-band grammar-worm generator. Build a corpus of
1000+ tiny programs covering all constructs at lengths 0-64.

### Strategy (revised)

**Phase A — Claude as worm (current)**: Hand-generate 1000+ exhaustive
primitive-first tests covering every SNOBOL4 construct at its simplest.
Fix every failure before moving to automation. This flushes fundamental
bugs that would otherwise corrupt the automated corpus.

**Phase B — Automated generator**: Once Phase A is green, build a generator
in `generator.clj` that produces 100,000 programs on the fly, runs them
through the Clojure runtime, and pins every novel failure as a regression test.

### Tasks
- [x] 18.6  `test_bootstrap.clj` — 130 hand-written tests, 7 length bands, 0 failures
- [x] 18.A  `test_worm1000.clj` — 521 tests generated (Python worm); needs run + triage
- [ ] 18.A2 Run test_worm1000; fix all failures to 0
- [ ] 18.A3 Expand to true 1000 tests (fill gaps: BREAKX, FENCE, ABORT, CURSOR, CONJ, BAL, ARBNO deeper)
- [ ] 18.1  `gen-by-length` in `generator.clj` — lazy seq of programs per char count
- [ ] 18.2  `rand-statement` — random walk of statement grammar using canonical pools
- [ ] 18.3  Error-class programs per length band (syntax/semantic/runtime/normal)
- [ ] 18.4  Automated batch runner — 100k programs, pin failures as regression tests
- [ ] 18.5  Fix all failures; iterate to 100% pass (or documented known-skip)
- [ ] 18.7  Commit

---

## Sprint 19 — Generator Depth Expansion  PLANNED

- Depth parameter on `rand-program`
- New move types: `move-define-call`, `move-table-ops`, `move-array-ops`, `move-convert`, `move-anchored-match`
- Shrinking / minimizer for failing programs
- Seeded runs for reproducible corpus
- Target: depth 1-6, corpus >= 5000 programs all green

---

## Sprint 20 — File I/O  PLANNED

`INPUT(var,channel,file)`, `OUTPUT(var,channel,file)`, `ENDFILE`, `REWIND`, `DETACH`.
Required before Gimpel programs that read input files can run.

---

## Sprint 21 — OPSYN & LOAD  PLANNED

`OPSYN(new,old,nargs)` — alias in INVOKE dispatch table.
`LOAD` — graceful stub (real DL load out of scope).

---

## Sprint 22 — Full Validation & Release  PLANNED

>=80% Gimpel pass rate (excluding LOAD programs).
Golden corpus >= 5000, all green. README written. Tag v1.0.

---

## Corpus / Harness Ideas Backlog

- Regression corpus — every `:fail` that gets fixed becomes a permanent `:pass`
- Oracle-disagree corpus — programs where SPITBOL and CSNOBOL4 differ
- Depth-stratified corpus — separate edn files per depth band
- Auto-minimizer — binary-search each `:fail` to fewest statements
- Parallel runner — `pmap` over programs with thread-local compiler state
- stdin injection — pass `INPUT` lines to programs that read them
- Diff display — coloured unified diff of oracle vs clojure stdout for `:fail`
- HTML report — `(generate-report corpus)` -> `report.html`

---

## Tradeoff Prompt

> **Read this before every design decision.**

**1. Single-file engine.**
`match.clj` is one loop/case. Cannot be split — `recur` requires all targets
in the same function body. Do not attempt to refactor.

**2. Immutable-by-default, mutable-by-atom.**
TABLE and ARRAY use `atom`. All other values passed by value.

**3. The label/body whitespace contract.**
Labels flush-left, bodies indented. Compiler does NOT strip leading whitespace.
Tests must always indent statement bodies.

**4. INVOKE is the single dispatch point.**
All built-in functions go through INVOKE's case table. Add both lowercase and
uppercase entries for every new function. Do not rely on `($$ op)` fallthrough.

**5. nil means failure; epsilon means empty string.**
nil = match/statement failure. epsilon (`""`) = valid empty SNOBOL4 value.

**6. ALL keywords UPPERCASE.**
`:S(LABEL)` `:F(LABEL)` `:(RETURN)` `:(END)` — uppercase only, no case folding.

**7. `clojure.core/=` inside `operators.clj`.**
`operators.clj` excludes `clojure.core/=`. Always use `clojure.core/=` or the
`equal` alias for value comparisons. Bare `=` builds IR lists.

**8. INVOKE args are pre-evaluated.**
The EVAL! `true` branch calls `(map EVAL! parms)` before `(apply INVOKE op args)`.
Args arriving in INVOKE are already evaluated. Never call `EVAL!` on them again.

**9. Two-tier generator discipline.**
`rand-*` = probabilistic, fast, random coverage.
`gen-*` = exhaustive lazy sequences, deterministic, complete coverage.
Keep both tiers. Use `rand-*` for smoke tests; `gen-*` for regression suites.

**10. Typed pools are canonical fixtures.**
`I J K L M N` integers, `S T X Y Z` strings, `P Q R` patterns, `L1 L2` labels.
Pre-initialize fixtures at program start. Do not generate total randomness.

---

## Sprint 18B — Per-Statement Timeout Architecture  DESIGN (2026-03-08)

### The Two Problems Identified This Session

#### Problem 1: `lein test` hangs forever on infinite-looping programs  ✅ FIXED
**Root cause**: The bare `prog` macro called `(RUN (CODE ...))` with no timeout.
Any SNOBOL4 program containing an infinite loop (`:S(SELF)`, unbounded GOTO,
broken exit condition) would block the entire `lein test` process forever.
It is **mathematically impossible** (Rice's theorem / Halting Problem) to
statically determine whether a SNOBOL4 program terminates.

**Fix implemented**: `test_helpers.clj` — `prog-timeout`, `prog`, `prog-infinite`
macros wrap `RUN` in a `future`+`deref` with a configurable ms budget and
1 retry. Timeout is reported as a `clojure.test/is` failure, not a hang.
All test files now import `prog` from `test-helpers`.

#### Problem 2: Timeout granularity is at the wrong level  ⚠️ DESIGN NEEDED
**Root cause**: Even with per-`deftest` timeouts, a `deftest` that runs a
10-statement SNOBOL4 program still has only ONE timeout for the whole program.
If statement 7 loops infinitely, the test times out — but statements 1-6
gave no signal about what passed or failed.

Worse: in the SNOBOL4 runtime, `RUN` is a `loop/recur` — a tight, single-
threaded Clojure loop. **There is no hook point between statement executions**
where a timer can be checked without modifying the core engine. The future/
deref kill-from-outside approach can only fire once per future, not per
statement.

### The Correct Architecture

**Principle**: Each `deftest` should cover exactly ONE semantic fact.
That semantic fact may require a few setup statements (assign fixtures,
define a function) but must have exactly ONE statement that is the subject
of the test — and that is the statement that can loop.

**Three test shapes**:

```
Shape A — Atomic (no risk of infinite loop):
  Single assignment or single match. Budget: 100ms.
  (deftest assign-int-literal
    (prog-timeout 100 "        I = 42" "END")
    (is (= 42 ($$ 'I))))

Shape B — Bounded loop (terminates by construction):
  Loop with a provably finite bound (e.g. LT(I,10)).
  Budget: 500ms. Document the bound in the test docstring.
  (deftest loop-count-10
    \"Loop I=1..10, bounded by LT(I,10). Max 10 iters.\"
    (prog-timeout 500 ...))

Shape C — Algorithm (multi-statement, risk of infinite loop):
  These MUST be split. Each sub-algorithm step gets its own deftest.
  Never bundle a 15-statement algorithm into one deftest.
  The gimpel-bsort test is the canonical example of what NOT to do.
```

**The statement-level timeout problem (runtime architecture)**:

The `RUN` loop is `loop/recur` — uninterruptible from inside. Options:

| Option | Mechanism | Cost | Granularity |
|--------|-----------|------|-------------|
| A. Future-per-deftest | `future`+`deref` wraps whole `prog` | Already done | Per-test |
| B. Step counter in RUN | Add `(when (> steps MAX) (throw :step-limit))` | ~5% overhead | Per-statement (approx) |
| C. Future-per-statement | Wrap each statement dispatch in its own future | High overhead | True per-statement |
| D. Thread interrupt | Set interrupt flag; check in RUN loop | Medium | Per-statement |

**Recommendation: Option B + A combined.**

- Keep Option A (future-per-deftest) as the outer safety net.
- Add Option B (step counter) inside `RUN` as the inner per-statement guard.
  A `&STEPLIMIT` keyword (default 100,000 steps) kills runaway programs fast
  and tells you exactly how many statements executed before the kill.
  This is cheap — one integer increment and comparison per loop iteration.
  It also gives the automated worm generator a way to classify programs:
  `:ok`, `:step-limit`, `:timeout`, `:error`.

**Step counter design (runtime.clj)**:
```clojure
;; In RUN, add step counter to loop:
(loop [current (saddr at) steps 0]
  (when (> steps @<STEPLIMIT>)   ; &STEPLIMIT default 100_000
    (throw (ex-info "Step limit exceeded"
                    {:snobol/signal :step-limit :steps steps})))
  ...
  (recur (saddr next) (inc steps)))
```

**`&STEPLIMIT` keyword** (add to env.clj):
- Default: `100000` (fast programs finish in microseconds; 100k is generous)
- Tests can set it low for infinite-loop detection: `(snobol-set! '&STEPLIMIT 50)`
- Harness uses it: classify `:step-limit` same as `:timeout` (both = divergence)

### Test Catalog Redesign

**Current state**: tests bundled by sprint/feature into large files.
Files grow unbounded. A single hang in a large test file blocks the whole file.

**Target state**: a test catalog organized as:
```
test/
  SNOBOL4clojure/
    catalog/
      t_assign.clj        ; Shape A only — atomic assignments
      t_arith.clj         ; Shape A — arithmetic, all operators  
      t_compare.clj       ; Shape A — EQ/LT/GT/etc
      t_string.clj        ; Shape A — concat, SIZE, TRIM, etc
      t_patterns_prim.clj ; Shape A — LEN/ANY/SPAN/BREAK/etc, single match
      t_patterns_cap.clj  ; Shape A — $ and . capture
      t_patterns_adv.clj  ; Shape B — ARB, ARBNO, FENCE, CONJ, BAL
      t_goto.clj          ; Shape A+B — unconditional/conditional goto
      t_loops.clj         ; Shape B — bounded loops, documented bound
      t_define.clj        ; Shape B — DEFINE/RETURN/FRETURN
      t_table.clj         ; Shape A — TABLE read/write
      t_array.clj         ; Shape A — ARRAY read/write
      t_convert.clj       ; Shape A — CONVERT/DATATYPE
      t_algorithms.clj    ; Shape C — split algorithms, one step per deftest
```

**Naming convention**: `t_<feature>_<nnn>` where nnn is zero-padded.
Each test file stays under 200 `deftest`s. Files are independent — one
hanging file does not block another.

**worm1000 migration**: existing `test_worm1000.clj` (521 tests) is already
at the right granularity (one semantic fact per deftest) but should be
migrated into the catalog structure above, one section at a time.

### Tasks (Sprint 18B)

- [ ] 18B.1  Add `&STEPLIMIT` to `env.clj` (default 100,000)
- [ ] 18B.2  Add step counter + `:step-limit` signal to `runtime.clj`
- [ ] 18B.3  Handle `:step-limit` in `test-helpers/run-with-timeout` —
             report as `:timeout` in test output with step count
- [ ] 18B.4  Handle `:step-limit` in `harness.clj` — classify same as `:timeout`
- [ ] 18B.5  Split `gimpel-bsort` (and any other Shape C tests) into per-step Shape B tests
- [ ] 18B.6  Create `test/SNOBOL4clojure/catalog/` directory structure
- [ ] 18B.7  Migrate `test_worm1000.clj` sections into catalog files, one file per section
- [ ] 18B.8  Document budget conventions in each catalog file header
- [ ] 18B.9  Update `project.clj` test paths to include `catalog/`
- [ ] 18B.10 Confirm full suite runs in < 60s wall clock with 0 hangs

### Session Log Entry (2026-03-08, this session)

| Date | What Happened |
|------|---------------|
| 2026-03-08 (session 3) | Oracles re-uploaded. Baseline confirmed 220/548/0. Diagnosed `lein test` hang: `test-cooper` taking 90s due to `cooper-span-010` and `cooper-notany-006` looping. Root cause: unary `*` (deferred pattern) captured variable value at build time instead of doing live lookup at match time. Fixed in `operators.clj` (special-case `(* sym)` in EVAL! to build live thunk) and `patterns.clj` (charset constructors detect DEFER! arg and wrap in outer DEFER). Created `test_helpers.clj` with `prog-timeout`/`prog`/`prog-infinite` macros — every test now runs under a 2000ms wall-clock budget with 1 retry. Wired all three main test files to use it. Full suite now completes in ~60s instead of hanging. One real failure exposed: `gimpel-bsort` genuinely times out (real runtime bug). Two architectural problems documented in PLAN.md: (1) fixed — outer timeout; (2) needs design — per-statement step limit + test catalog reorganisation. |
