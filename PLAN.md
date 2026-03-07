# SNOBOL4clojure — Stage 6 Plan: Runtime Polish

## Context

This file is the handoff document for a fresh Claude session working in a
fresh container. It summarises the entire project history, the current state
of the codebase, and the specific work to be done in Stage 6.

---

## Repository

```
GitHub: https://github.com/LCherryholmes/SNOBOL4clojure.git
Credentials: LCherryholmes / <PAT — retrieve from repo owner before pushing>
  The PAT was intentionally omitted here. Ask the repo owner for a fresh one.
Build: Leiningen 2.12.0  (lein test, lein run)
```

Clone it first:
```bash
git clone https://github.com/LCherryholmes/SNOBOL4clojure.git
cd SNOBOL4clojure
lein test   # must be: 68 tests, 274 assertions, 0 failures
```

---

## Sibling repositories (reference only — already studied, do NOT modify)

```
SNOBOL4python  — Python 0.5.0,  250 tests pass  (pip / pytest)
SNOBOL4csharp  — C#    0.4.0,  263 tests pass  (dotnet SDK 10)
```

These are by the same author (Lon Jones Cherryholmes) and share the same
authorial signature: Greek-letter naming (ε σ Σ Π δ Δ ζ γ), `cstack` commit
protocol, `SEARCH`/`MATCH`/`FULLMATCH` trio.  If you need to understand how
something *should* work, look at the Python or C# sibling first.

---

## What has already been done (Stages 1–5)

### Stage 1 — Housekeeping (commit 7b38dc3)
- Scratch files moved to `misc/`
- `errors.clj` fixed (missing ns form)

### Stage 2 — Test suite repair (commit 7b38dc3)
- Removed `test-ns-hook` antipattern
- Fixed `@&LCASE` deref bug
- Result: 1 test → 8 tests / 48 assertions

### Stage 3 — Namespace split (commit 0800b02)
The original 764-line monolith `core.clj` was split into 10 namespaces:

| File | Responsibility |
|------|----------------|
| `env.clj` | globals, DATATYPE, NAME deftype, `$$`/`reference`, `num`, arithmetic |
| `primitives.clj` | low-level scanners: LIT$, ANY$, SPAN$, BREAK$, POS#, RPOS#… |
| `match.clj` | MATCH state machine + SEARCH/MATCH/FULLMATCH/REPLACE public API |
| `patterns.clj` | pattern constructors: ANY, SPAN, POS, ARBNO, FENCE… |
| `functions.clj` | built-in fns: REPLACE, SIZE, DATA, DATA!, stubs |
| `grammar.clj` | instaparse grammar + parse-statement/parse-expression |
| `emitter.clj` | AST → Clojure IR transform |
| `compiler.clj` | CODE!/CODE: source text → labeled statement table |
| `operators.clj` | operators (?, =, \|, $, +…), EVAL/EVAL!/INVOKE, comparison primitives |
| `runtime.clj` | RUN: GOTO-driven statement interpreter |
| `core.clj` | thin facade with explicit `def` re-exports of full public API |

### Stage 4 — Test expansion (commit 29c7c0b)
Five new test namespaces, 68 tests total, 274 assertions, 0 failures.
Known bugs documented with `#_` + TODO in tests.

### Stage 5 — Library polish (commit 2a92970)
- `SEARCH`/`MATCH`/`FULLMATCH`/`REPLACE` public API (matching siblings)
- Return type: `[start end]` half-open span or `nil`  (Clojure-native)
- `*trace*` dynamic var gates all animate output (off by default)
- `doc/library.md` written

**Current test baseline: 68 tests / 274 assertions / 0 failures**

---

## Known bugs (already documented in tests with `#_` TODO)

1. **Unanchored substring scan** — `SEARCH` works (Stage 5), but the engine
   itself doesn't support ARB prefix for unanchored patterns in SEQ context
2. **Cross-SEQ backtracking** — ALT doesn't retry when a later SEQ element
   fails after an ALT child already succeeded
3. **EQ guard pruning in ALT branches** — `match-2` in core_test.clj
4. **FENCE + outer ALT interaction** — `match-real` in core_test.clj
5. **ANY(multi-arg) inside EVAL string** — `match-define` in core_test.clj
6. **`(LEN 10)` returns EXPRESSION not PATTERN** — `datatype-test`
7. **ARB, ARBNO, BAL, FENCE** are stubs — they always return `nil`

Bugs 1 and 2 are the most impactful for Stage 6.  Address as encountered.

---

## Stage 6 Goal: Runtime Polish

Make `CODE` + `RUN` work on real SNOBOL4 programs.

### Milestone 6A — Hello, World (the smoke test)

The simplest possible end-to-end program:

```snobol4
         OUTPUT = "Hello, World!"
END
```

What this requires:
- `CODE` parses and loads the two statements
- `RUN` evaluates the assignment `OUTPUT = "Hello, World!"`
- Assignment to the `OUTPUT` special variable triggers `println`
- `RUN` hits `END` and stops cleanly

**The OUTPUT hook**: Currently `OUTPUT$` in `env.clj` is an inert atom.
It needs to be a watched atom (or the `=` operator in INVOKE needs a
special case) so that assigning to `OUTPUT` calls `(println value)`.

The Python sibling intercepts `OUTPUT` by name in its assignment operator.
The C# sibling lists `OUTPUT` as a `BuiltinVar`. Both print the assigned
value immediately.

The simplest Clojure approach: in `INVOKE` (operators.clj), when the `=`
case sees `N = 'OUTPUT`, call `(println r)` in addition to the normal
`def`.  Mirror for `INPUT`: reading `INPUT` should call `(read-line)`.

### Milestone 6B — Counter loop

```snobol4
         N = 0
LOOP     N = N + 1
         OUTPUT = N
         EQ(N, 5)         :S(END)
                          :(LOOP)
END
```

What this exercises:
- Assignment (`N = 0`, `N = N + 1`)
- Arithmetic via `+` operator
- `OUTPUT` hook
- `EQ` comparison function as a statement body (succeeds/fails → S/F goto)
- `:S(END)` success goto, `:(LOOP)` unconditional goto
- `END` label termination

### Milestone 6C — Fibonacci

```snobol4
         A = 0
         B = 1
LOOP     OUTPUT = A
         C = A + B
         A = B
         B = C
         LE(A, 100)       :S(LOOP)
END
```

Exercises everything from 6B plus multiple variable assignments in sequence.

### Milestone 6D — Pattern match in a program

```snobol4
         SUBJECT = "The quick brown fox"
         SUBJECT  (SPAN(&LCASE)) . WORD
         OUTPUT = WORD
END
```

What this exercises:
- Pattern match statement form (`SUBJECT pattern`)
- Conditional assignment operator `.` (captures matched text into `WORD`)
- `&LCASE` keyword reference inside a pattern

### Milestone 6E — User-defined function

```snobol4
         DEFINE('DOUBLE(N)')
DOUBLE   DOUBLE = N + N                            :(RETURN)
         OUTPUT = DOUBLE(3)
         OUTPUT = DOUBLE(7)
END
```

Exercises `DEFINE`, function call syntax, `RETURN` label.

---

## The runtime pipeline (current state)

```
source text (string)
    │
    ▼ CODE! (compiler.clj)
[CODES NOS LABELS]  — map of statement# → [body goto]
    │
    ▼ CODE (compiler.clj)
loads into global atoms: <CODE>, <LABL>, <STNO>
    │
    ▼ RUN (runtime.clj)
loop: fetch stmt → EVAL! body → dispatch on success/fail goto
```

### How RUN currently works

```clojure
(defn RUN [at]          ; at = start label or statement number
  (letfn [...]
    (loop [current (saddr at)]
      (when-let [key (skey current)]
        (when-let [stmt (@<CODE> key)]
          (let [body (if (map? ferst) seqond ferst)
                goto (if (map? ferst) ferst  seqond)]
            (if (EVAL! body)
              ;; success path: :G > :S > next
              (if (contains? goto :G) (recur ...) ...)
              ;; fail path: :G > :F > next
              (if (contains? goto :G) (recur ...) ...))))))))
```

`EVAL!` returns a truthy value on success, `nil` on failure.
The goto map has keys `:G` (unconditional), `:S` (on success), `:F` (on fail).

### What END does

`END` is a label.  When `RUN` reaches it (or an explicit `:(END)` goto),
the `when-let [stmt (@<CODE> key)]` returns `nil` because there is no
statement body at `END`, so the loop terminates naturally.
**Verify this is actually what happens** — it may need an explicit check.

---

## Key files for Stage 6

### `src/SNOBOL4clojure/runtime.clj` — primary focus

Current state is minimal and probably broken on real programs.
Key things to verify/fix:

1. **END termination** — does `(when-let [stmt (@<CODE> :END)] ...)` stop?
2. **Numeric statement stepping** — `(recur (saddr (inc (current 0))))` —
   does this correctly walk statement 1 → 2 → 3?
3. **EVAL! truthiness contract** — `EVAL!` returns the result value;
   in SNOBOL4 a statement *fails* if its body expression returns `nil`.
   Verify this contract is consistently maintained.
4. **Infinite loop guard** — `&STLIMIT` atom exists in env.clj; RUN should
   decrement `&STCOUNT` and stop when it hits `&STLIMIT`.

### `src/SNOBOL4clojure/operators.clj` — INVOKE `=` case

The `=` operator in INVOKE needs OUTPUT/INPUT/TERMINAL special-casing:

```clojure
=  (let [[N r] args]
     (let [val (if (not (list? r)) r (do (eval (list 'def N)) ...))]
       (when (= N 'OUTPUT)   (println val))
       (when (= N 'TERMINAL) (println val))
       val))
```

### `src/SNOBOL4clojure/functions.clj` — DEFINE stub

Currently:
```clojure
(defn DEFINE [] nil)
```

For Milestone 6E it needs to parse the prototype string and install a
`defn` in the namespace.  The Python sibling's `DEFINE` shows the pattern.

---

## Test strategy for Stage 6

Add `test/SNOBOL4clojure/test_runtime.clj`.

Each milestone gets its own `deftest`.  Use `with-out-str` to capture
`println` output and assert on it:

```clojure
(deftest test-hello-world
  (is (= "Hello, World!\n"
         (with-out-str
           (CODE "         OUTPUT = \"Hello, World!\"\nEND")
           (RUN :END)))))
```

Wait — `RUN` needs to start at statement 1, not at `:END`.  Fix the test:

```clojure
(deftest test-hello-world
  ;; CODE returns the start label/number; RUN from there
  (let [start (CODE "         OUTPUT = \"Hello, World!\"\nEND")]
    (is (= "Hello, World!\n"
           (with-out-str (RUN start))))))
```

**Important**: `CODE` loads into global atoms.  Tests must reset them between
runs or use a fixture.  Add a `reset-runtime!` helper:

```clojure
(defn reset-runtime! []
  (reset! env/STNO 0)
  (reset! env/<STNO> {})
  (reset! env/<LABL> {})
  (reset! env/<CODE> {}))

(use-fixtures :each (fn [t] (reset-runtime!) (t)))
```

---

## Implementation order

1. **Read and run** the existing test suite to confirm baseline.
2. **Instrument** `CODE!` + `RUN` with a simple Hello World attempt — see
   exactly where it breaks.
3. **Fix OUTPUT hook** in `INVOKE =` (operators.clj).
4. **Fix END termination** in `RUN` if needed (runtime.clj).
5. **Add `test_runtime.clj`** with `reset-runtime!` fixture and Milestones
   6A–6C tests.
6. **Iterate** through each milestone until green.
7. **Milestone 6D** (pattern match in program) — likely surfaces EVAL!
   issues with the `?` statement form.
8. **Milestone 6E** (DEFINE) — implement DEFINE stub properly.
9. **Commit each milestone** as it turns green.
10. **Update CHANGELOG.md** and **README.md** with runtime status.

---

## Commit convention (match existing history)

```
Stage 6A: runtime — Hello World end-to-end
Stage 6B: runtime — counter loop (N = N + 1, EQ goto)
Stage 6C: runtime — Fibonacci
Stage 6D: runtime — pattern match statement
Stage 6E: runtime — DEFINE / user functions
Stage 6: runtime polish (omnibus commit if milestones merged)
```

---

## Things NOT in scope for Stage 6

- `ARB` / `ARBNO` / `BAL` / `FENCE` engine implementation (stubs are fine)
- Cross-SEQ backtracking fix
- `INPUT` reading from stdin (stub returning ε is fine for now)
- File I/O (`DETACH`, `REWIND`, `ENDFILE`)
- `LOAD` / `UNLOAD` (dynamic Clojure namespace loading)
- The Python/C# `cstack` capture mechanism (assignment operators `$` and `.`
  inside patterns) — these are stubs in `operators.clj` and can stay stubs

---

## Quick orientation commands

```bash
# Clone and baseline
git clone https://github.com/LCherryholmes/SNOBOL4clojure.git
cd SNOBOL4clojure
lein test                         # 68 tests, 274 assertions, 0 failures

# REPL exploration
lein repl
(require '[SNOBOL4clojure.core :refer :all] :reload)
(CODE "OUTPUT = \"hello\"\nEND")  ; compile
(RUN 1)                            ; run from statement 1

# See what CODE! produces for a simple program
(require '[SNOBOL4clojure.compiler :refer [CODE!]])
(CODE! "N = 0\nLOOP N = N + 1\n     EQ(N,3) :S(END)\n     :(LOOP)\nEND")
```

---

## Architecture diagram

```
SNOBOL4 source text
        │
  ┌─────▼──────┐
  │  grammar   │  instaparse PEG → parse tree
  └─────┬──────┘
        │
  ┌─────▼──────┐
  │  emitter   │  parse tree → Clojure IR (lists/vectors)
  └─────┬──────┘
        │
  ┌─────▼──────┐
  │ compiler   │  IR → {CODES NOS LABELS}, loaded into global atoms
  └─────┬──────┘
        │  atoms: <CODE> <LABL> <STNO>
  ┌─────▼──────┐
  │  runtime   │  RUN: fetch → EVAL! → goto dispatch → loop
  └─────┬──────┘
        │
  ┌─────▼──────┐
  │ operators  │  EVAL! / INVOKE — evaluates each IR expression
  └─────┬──────┘
        │
  ┌─────▼──────┐
  │   match    │  SEARCH/MATCH/FULLMATCH engine
  └────────────┘
```

---

*This plan was written at the end of Stage 5 (commit 2a92970) by the Claude
instance that carried out Stages 1–5.  The next Claude should read this file
first, clone the repo, run the tests to confirm the baseline, then work
through the milestones in order.*
