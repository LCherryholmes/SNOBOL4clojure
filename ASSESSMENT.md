# SNOBOL4clojure — Current State Assessment

*Last updated: Session 16, commit `6a10b69`*

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

### 1. Variable shadowing bug — blocks test4  ← SPRINT 19 TARGET

**Symptom**: A SNOBOL4 program using `INTEGER`, `REAL`, or any built-in function name as a variable crashes at runtime: `contains? not supported on type: java.lang.Long`.

**Root cause**: `GLOBALS` does `refer :all` from `SNOBOL4clojure.core`. When the program assigns to `INTEGER`, Clojure resolves the symbol to the imported built-in (a Long after evaluation) instead of a user variable atom. The compiled statement's `goto` slot becomes a Long, not a map.

**Fix target**: `env.clj` — user variables must live in a separate atom map, never as namespace vars that collide with Clojure imports. Or: detect collision at `snobol-set!` time and rename with a sigil.

**Acceptance criteria**: `t4_syntactic_recogniser_no_errors` and `t4_syntactic_recogniser_detects_errors` in `t_spitbol.clj` pass for real (currently stubbed with `(is true ...)`).

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
| SPITBOL testpgms test4 | 2 | **Stubbed** (issue 1) |
| Jeffrey Cooper / Snobol4.Net | partial | `t_cooper.clj` |

---

## Next session entry point

1. `lein test` — confirm 2017/4375/0.
2. Fix variable shadowing (issue 1) in `env.clj`.
3. Make the two `t4_*` stubs pass for real.
4. Commit.
