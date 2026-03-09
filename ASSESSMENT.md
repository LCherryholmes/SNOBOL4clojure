# SNOBOL4clojure — Current State Assessment

*Last updated: Sprint 19, commit `9811f5e`*

---

## Test suite health

| Metric | Value |
|--------|-------|
| Total tests | 2,017 |
| Total assertions | 4,375 |
| Failures | **0** |
| Baseline (start of project) | 220 / 548 / 0 |

---

## What is solidly working

- **Full arithmetic**: integer, real, mixed-mode, `**` exponentiation, REMDR, division truncation (verified vs v311.sil)
- **String operations**: concatenation, SIZE, TRIM, REPLACE, DUPL, LPAD, RPAD, REVERSE, SUBSTR
- **Pattern engine**: LEN, TAB, RTAB, ANY, NOTANY, SPAN, BREAK, BREAKX, POS, RPOS, BOL, EOL, ARB, ARBNO, FENCE, ABORT, BAL, REM, CONJ, deferred `*var` patterns
- **Capture**: `$` immediate and `.` conditional on match success — both correct
- **Control flow**: GOTO, :S/:F, computed goto, DEFINE/RETURN/FRETURN, recursive functions, APPLY
- **Data structures**: TABLE, ARRAY (multi-dim), DATA/FIELD (PDD)
- **Type system**: DATATYPE, CONVERT, all coercions, INTEGER/REAL/STRING predicates
- **Indirect addressing**: `$sym` read/write, NAME dereference through subscripts
- **I/O**: OUTPUT, INPUT (stdin), TERMINAL (stderr), named file channels (INPUT/OUTPUT with unit+file)
- **Preprocessor**: `-INCLUDE` recursive with cycle detection
- **CODE(src)**: compile and run a SNOBOL4 string in current environment
- **Corpus coverage**: Gimpel (24), Shafto AI (12), SPITBOL testpgms (44)

---

## Known open issues

### ~~1. Variable shadowing bug~~ — FIXED Sprint 19  ✅

**Was**: User programs using `INTEGER`, `REAL`, or any engine built-in name as a variable crashed at runtime.

**Root cause 1**: `snobol-set!` called `intern()` into the active Clojure namespace, mutating engine Vars like `INTEGER`/`REAL`.

**Root cause 2**: `ns-resolve` on `SNOBOL4clojure.core` for `NAME` returns the `NAME` *class* (imported deftype), not a Var. `var-get` on a Class throws `ClassCastException`, silently killing function calls with a parameter named `NAME`.

**Fix**: `<VARS>` atom (plain `{symbol → value}` map) replaces namespace interning. `$$` lookup chain: `<VARS>` first, then engine namespace read-only, with `instance? clojure.lang.Var` guard.

**Acceptance criteria met**: `t4_syntactic_recogniser_no_errors` and `t4_syntactic_recogniser_detects_errors` both pass for real. commit `9811f5e`.

### 2. CAPTURE-COND (`.`) deferred semantics — low priority

`.` assigns immediately like `$`; standard says wait until overall match succeeds. Correct in all tested programs; only matters when a later element fails after `.` matched.

### 3. DETACH / REWIND / BACKSPACE — stubs

Named file I/O channels work. These three lifecycle operations are no-ops.

### 4. OPSYN — not implemented

Needed for full AI-SNOBOL SNOLISPIST library.

---

## Corpus coverage summary

| Corpus | Tests | Status |
|--------|-------|--------|
| Worm T0–T5 bands (catalog) | ~1,400 | All green |
| Gimpel *Algorithms in SNOBOL4* | 24 | All green |
| Shafto *AI Programming in SNOBOL4* | 12 | All green |
| SPITBOL testpgms test1/2/3 | 31 | All green |
| SPITBOL testpgms test4 | 2 | **All green** (Sprint 19) |
| Jeffrey Cooper / Snobol4.Net | partial | `t_cooper.clj` |

---

## Next session entry point

1. `lein test` — confirm 2017/4375/0.
2. **Sprint 25D** — named I/O channels: fix channel-registration bug in `env.clj`/`operators.clj`. See PLAN.md Sprint 25D notes. Unlocks remaining 6 Gimpel programs.
3. **Sprint 25E** — OPSYN — needed for full AI-SNOBOL SNOLISPIST library.
4. **beauty.sno** — the flagship. Needs `-INCLUDE` files from Lon + Sprint 25D I/O.

---

## Backend acceleration — planned stages

Four backends exist (23A–23D). Five more are planned. See PLAN.md Stage 23F–23J for full design notes.

| Stage | What | Status |
|-------|------|--------|
| 23E — Inline EVAL! | Emit arithmetic/assign/cmp directly into JVM bytecode; eliminate IFn.invoke overhead on hot loops | **NEXT after corpus work** |
| 23F — Compiled pattern engine | Compile specific pattern objects to Java methods; short-circuit the 405-line `engine` loop | PLANNED |
| 23G — Integer unboxing | Emit `long` primitives for provably-integer variables; eliminate boxing/GC pressure | PLANNED |
| 23H — AOT .jar corpus cache | Write transpiled programs as `.clj` files, AOT-compile to `.class`; skip re-transpile on repeated runs | PLANNED |
| 23I — Parallel worm runner | `pmap` across worm batch and test suite; near-linear core scaling | PLANNED |
| 23J — GraalVM native-image | Standalone binary, 10ms startup, no JVM; Truffle AST as ultimate vision | VISION |

**Key insight not captured before**: all of 23A–23D are execution-layer
optimisations. None touch `match.clj`. For pattern-heavy SNOBOL4 the `engine`
loop is the real ceiling — 23F addresses this.
