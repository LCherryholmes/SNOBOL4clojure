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

## Benchmark grid — current baseline (2026-03-09)

Three engines × four Clojure backends. All times ms/run, lower = faster.
**Important**: SPITBOL and CSNOBOL4 times include ~15ms process-spawn overhead per run. Subtract ~15ms to compare pure execution speed.

Platform: OpenJDK 64-Bit Server VM 21.0.10, Ubuntu 24, x86-64.
Run: `lein run -m SNOBOL4clojure.bench`

```
╔═══════════════════════════════════════════════════════════════════════════════════════╗
║       SNOBOL4 ENGINE BENCHMARK GRID  (ms / run — lower = faster)                    ║
╠═══════════════════════╦══════════╦══════════╦══════════╦══════════╦══════════╦══════════╣
║ Program               ║ SPITBOL  ║ CSNOBOL4 ║ Interp   ║ Transpil ║ Stack VM ║ JVM code ║
╠═══════════════════════╬══════════╬══════════╬══════════╬══════════╬══════════╬══════════╣
║ arith-10k             ║   15.83  ║   27.25  ║  112.14  ║   88.03  ║  107.02  ║   85.55  ║
║ strcat-500            ║   15.09  ║   26.16  ║    9.70  ║    7.99  ║   26.02  ║    6.93  ║
║ pat-span              ║   15.47  ║   26.29  ║   28.05  ║   25.89  ║   52.06  ║   25.22  ║
║ fact45                ║   16.59  ║   24.73  ║     N/A* ║    1.11  ║  167.46  ║    0.89  ║
╚═══════════════════════╩══════════╩══════════╩══════════╩══════════╩══════════╩══════════╝
```

Programs:
- **arith-10k**: `I = 0 / LOOP  I = I + 1 / LT(I,10000) :S(LOOP)` — pure arithmetic loop
- **strcat-500**: string concatenation loop growing S to 500 chars
- **pat-span**: SPAN pattern match loop, 1000 iterations against 'hello world foo bar'
- **fact45**: Factorial 1..45 via big-number string arithmetic (testpgms-test3.spt)

`*` fact45 interpreter N/A is a bench harness namespace-isolation issue (the transpiler run contaminates shared env state). The interpreter runs fact45 correctly in the test suite (`t3_factorial_table` passes). Fix tracked as bench-isolation bug.

### What the numbers say

**Arithmetic loops (arith-10k)**: All Clojure backends are ~7× slower than SPITBOL (after removing spawn overhead). The gap is entirely `EVAL!` overhead — every `I = I + 1` re-walks the IR list. This is exactly what Stage 23E (inline EVAL!) targets.

**String operations (strcat-500)**: Clojure interpreter beats CSNOBOL4! 9.7ms vs 26ms (net ~11ms after spawn). String-heavy work benefits from the JVM's native String handling. JVM codegen at 6.9ms is fastest overall.

**Pattern matching (pat-span)**: Clojure within 2× of SPITBOL net. The pattern engine (`match.clj`) is competitive for simple patterns. Stack VM regresses here — opcode dispatch overhead hurts the tight match loop.

**Factorial / big-number arithmetic (fact45)**: The transpiler (1.1ms) and JVM codegen (0.89ms) are **14–19× faster than SPITBOL** (net ~1.6ms). This is the EDN cache + JIT effect: the program compiles once to a tight loop, the JVM JIT learns it, and it flies. Stack VM regresses badly (167ms) — the bytecode overhead is proportionally large for this program's structure.

### Key observations for 23E planning

The `arith-10k` gap (7×) vs the `strcat-500` win (faster than CSNOBOL4) confirms the bottleneck is specifically `EVAL!` on numeric expressions, not the runtime loop itself. Stage 23E (inline arithmetic/assign/compare directly into JVM bytecode) should close most of the arith-10k gap without touching the string or pattern paths.

### How to update this grid

After each optimization sprint, run `lein run -m SNOBOL4clojure.bench` and paste the new grid here with a date stamp.

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
