# SNOBOL4clojure — Feature Assessment & Sprint Plan

**Baseline:** 82 tests / 314 assertions / 0 failures  
**Commit:** `4813ae8` (Stage 7c — ARB/ARBNO)  
**Reference materials:** `_backend_pure.py`, `_backend_c.py`, `SNOBOL4functions.py`, `snobol4ref/`, `x64ref/`

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented and tested |
| 🟡 | Partially implemented / stub exists |
| ❌ | Missing / stub returns nil or wrong value |
| 🔒 | Out of scope (intentional) |

---

## 1. Pattern Engine

### 1a. Primitive Scanners (`primitives.clj`)

| Primitive | Status | Notes |
|-----------|--------|-------|
| `LIT$` | ✅ | |
| `ANY$` | ✅ | |
| `NOTANY$` | ✅ | |
| `SPAN$` | ✅ | |
| `BREAK$` | ✅ | |
| `BREAKX$` | ❌ | Stub. Like BREAK but also yields at each char inside the break set — used for breakout-and-retry patterns. One scanner function needed. |
| `NSPAN$` | ❌ | Missing entirely. Like SPAN but matches zero chars (greedy, longest, no backtrack). Add to primitives + patterns. |
| `LEN#` | ✅ | |
| `TAB#` | ✅ | |
| `RTAB#` | ✅ | |
| `POS#` | ✅ | |
| `RPOS#` | ✅ | |
| `REM!` | ✅ | Matches rest of string to end |
| `SUCCEED!` | ✅ | |
| `FAIL!` | ✅ | |
| `ABORT!` | ❌ | Stub. Must immediately abort the entire match (no backtrack). Engine case needed. |

### 1b. Engine Node Types (`match.clj`)

| Node | Status | Notes |
|------|--------|-------|
| `SEQ` | ✅ | Concatenation with backtrack |
| `ALT` | ✅ | Alternation with backtrack |
| `LIT$` | ✅ | |
| `ANY$…RTAB#` | ✅ | All primitive dispatch |
| `CAPTURE` | ✅ | `P . N` conditional assignment |
| `ARB!` | ✅ | Shortest-first any-string |
| `ARBNO!` | ✅ | Zero-or-more repetitions, lazy expansion |
| `FENCE!` | 🟡 | `FENCE(P)` (fence_function) works. Bare `FENCE()` (fence_simple / abort-on-backtrack) uses recede not abort — semantics incomplete. |
| `BREAKX$` | ❌ | Needs engine case once primitive exists |
| `BAL!` | ❌ | Stub. Matches balanced parenthesised strings. Well-defined algorithm in `_backend_pure.py`. |
| `ABORT!` | ❌ | Stub. Must return `nil` immediately from engine loop. |
| `X` | ✅ | Indirect pattern lookup via `$$` |
| Immediate assign `$` | 🟡 | `CAPTURE` handles conditional (`.`). The `$` (immediate during match) path is not differentiated — both use the same CAPTURE node. `$` should assign on each match attempt; `.` only on overall success. |

### 1c. Pattern Constructors (`patterns.clj`)

| Constructor | Status | Notes |
|-------------|--------|-------|
| `ANY`, `NOTANY`, `SPAN`, `BREAK` | ✅ | |
| `BREAKX` | 🟡 | Constructor exists, calls stub primitive |
| `NSPAN` | ❌ | Not in patterns.clj at all |
| `LEN`, `TAB`, `RTAB`, `POS`, `RPOS` | ✅ | |
| `ARB`, `REM` | ✅ | |
| `ARBNO` | ✅ | |
| `FENCE` | 🟡 | `FENCE(P)` correct. Bare `FENCE()` abort semantics missing |
| `FAIL` | ✅ | |
| `BAL` | ❌ | Constructor exists, calls stub engine node |
| `ABORT` | ❌ | Constructor exists, engine stub |
| `α` / BOL | ❌ | Not in patterns.clj. Matches at start of line (pos=0 or after `\n`) |
| `ω` / EOL | ❌ | Not in patterns.clj. Matches at end of line (pos=end or before `\n`) |

---

## 2. Pattern Operators (`operators.clj` / `emitter.clj`)

| Feature | Status | Notes |
|---------|--------|-------|
| Concatenation (juxtaposition → SEQ) | ✅ | |
| Alternation `\|` | ✅ | |
| Immediate assign `P $ N` | 🟡 | Emitter maps to CAPTURE; `$` and `.` treated same |
| Conditional assign `P . N` | 🟡 | Same as above |
| Immediate cursor `@N` / `Θ` | ❌ | `@N` in grammar as `uop`; not emitted as cursor-assign pattern |
| Conditional cursor `%N` / `θ` | ❌ | Grammar has `%`; emitter maps to `pct`; not a pattern cursor |
| Guard `*expr` / `Λ` | ❌ | Not emitted as a deferred guard node. EQ guards evaluated eagerly. |
| Optional `~P` / `π` | 🟡 | `~` in grammar as `ttl`/`uop`; emitter maps to `tilde`; no `INVOKE 'tilde` |
| Conjunction `P & Q` / `ρ` | 🟡 | `&` parsed; emitter maps to `and`; `INVOKE 'and` not in INVOKE table |
| Regex `Φ`/`φ` | ❌ | Not planned — Python-specific |

---

## 3. Built-in Functions

### 3a. String Functions

| Function | Status | Notes |
|----------|--------|-------|
| `SIZE` | ✅ | |
| `REPLACE` | ✅ | Char-by-char translation |
| `DUPL` | ✅ | |
| `TRIM` | ✅ | |
| `REVERSE` | ✅ | |
| `LPAD` | ✅ | |
| `RPAD` | ✅ | |
| `SUBSTR` | ✅ | |
| `CHAR` | ✅ | |
| `ASCII` | ❌ | Not in functions.clj. Trivial: `(int (first s))` |
| `REMDR` | 🟡 | Macro stub exists, not wired through INVOKE |

### 3b. Comparison Functions (numeric)

| Function | Status | Notes |
|----------|--------|-------|
| `EQ`, `NE`, `LT`, `LE`, `GT`, `GE` | ✅ | In INVOKE table |

### 3c. Comparison Functions (string/lexicographic)

| Function | Status | Notes |
|----------|--------|-------|
| `LEQ`, `LNE`, `LLT`, `LLE`, `LGT`, `LGE` | ✅ | Defined via `primitive` macro |
| `IDENT`, `DIFFER` | ✅ | Defined via `primitive` macro |

### 3d. Type Conversion

| Function | Status | Notes |
|----------|--------|-------|
| `INTEGER` | ❌ | Not in functions.clj. Convert to integer or fail |
| `REAL` | ❌ | Not in functions.clj. Convert to real or fail |
| `STRING` | ❌ | Not in functions.clj. Convert to string |
| `CONVERT` | 🟡 | Stub — returns arg unchanged |
| `DATATYPE` | ✅ | Fixed to return `"PATTERN"` for pattern list nodes |

### 3e. I/O

| Feature | Status | Notes |
|---------|--------|-------|
| `OUTPUT =` (write line) | ✅ | `println` on assignment |
| `TERMINAL =` (write line) | ✅ | Same as OUTPUT |
| `INPUT` read | ❌ | `INPUT$` atom exists but never bound. Reading `INPUT` variable should call `read-line` |
| `INPUT(n,u,...)` | ❌ | File I/O association stub |
| `OUTPUT(n,u,...)` | ❌ | File I/O association stub |
| `ENDFILE`, `REWIND`, `DETACH`, `BACKSPACE` | ❌ | All stubs |

### 3f. Math

| Function | Status | Notes |
|----------|--------|-------|
| `REMDR` | 🟡 | Macro exists, not wired |
| `ABS` | ❌ | Not present. Trivial. |
| `SQRT`, `EXP`, `LOG` etc. | ❌ | Not present. Low priority. |

### 3g. Table / Array

| Feature | Status | Notes |
|---------|--------|-------|
| `TABLE` | 🟡 | Returns empty hashmap; indexed access via `ndx` not wired |
| `ARRAY` | 🟡 | Returns Object array; indexed access via `ndx` not wired |
| `ITEM` | ❌ | Stub |
| `SORT`, `RSORT` | ❌ | Stubs |
| `PROTOTYPE` | ❌ | Stub |

### 3h. Data Structures (DATA)

| Feature | Status | Notes |
|---------|--------|-------|
| `DATA(proto)` | 🟡 | `DATA!` parses prototype, generates deftype. `DATA` wraps it. Dispatch in DATATYPE not wired for user types. |
| `FIELD` | 🟡 | Returns `__slots__[i]` — needs proper accessor |
| Custom DATATYPE dispatch | ❌ | `#_(is (= (DATATYPE (tree. ...)) "tree"))` still disabled |

### 3i. Miscellaneous Functions

| Function | Status | Notes |
|----------|--------|-------|
| `DATE` | ✅ | |
| `TIME` | ✅ | |
| `COPY` | 🟡 | Stub — returns arg |
| `APPLY` | ❌ | Stub. Should call user function by name with args |
| `COLLECT` | 🟡 | Stub — GC hint, acceptable |
| `DUMP` | 🟡 | Stub |

---

## 4. Runtime (`runtime.clj`)

| Feature | Status | Notes |
|---------|--------|-------|
| `GOTO` / `:(LABEL)` | ✅ | |
| `:S(LABEL)` success goto | ✅ | |
| `:F(LABEL)` failure goto | ✅ | |
| `:S(L1) :F(L2)` combined | ✅ | |
| `DEFINE` + user functions | ✅ | `:(RETURN)` goto works |
| `FRETURN` | ❌ | Failure return from function |
| `NRETURN` | ❌ | Null return |
| `END` label termination | 🟡 | Works via GOTO `:END` convention |
| Multi-statement labels | ✅ | |
| Statement-level pattern match + replace | ✅ | |

---

## 5. Grammar / Emitter

| Feature | Status | Notes |
|---------|--------|-------|
| Labels, GOTOs | ✅ | |
| String/Integer/Real literals | ✅ | |
| All arithmetic operators | ✅ | |
| Pattern operators `$`, `.`, `\|` | ✅ | Parsed; `$`/`.` emit CAPTURE |
| `@N` cursor assign | ❌ | Parsed as `uop`; emitter emits `(at N)` but no INVOKE handler |
| `*expr` guard | ❌ | Parsed as unary `*`; emitter emits `(* expr)` but no guard handling |
| `~P` optional | 🟡 | Parsed; emitter emits `(tilde P)` but no INVOKE handler |
| `&` conjunction | 🟡 | Parsed; emitter emits `(and P Q)` but no INVOKE handler |
| `#` hash operator | ❌ | Parsed; emitter emits `(sharp x)`; semantics unclear |
| Conditional expr `(P, Q)` | 🟡 | Parsed; emits `[comma P Q]`; not in INVOKE |
| Indirect ref `*N` | 🟡 | Unary `*` — probably means "value of variable named by N" |

---

## Proposed Sprints

### Sprint 8 — Quick Wins (1 session)
Small isolated fixes with high correctness payoff.

1. **`ABORT!` engine node** — 3 lines: return `nil` from engine loop on `:proceed`
2. **Bare `FENCE()` abort** — differentiate abort vs recede in `:recede` handler
3. **`ASCII` function** — 1 line in functions.clj
4. **`REMDR` in INVOKE** — wire the existing macro to INVOKE table
5. **`INTEGER`, `REAL`, `STRING` conversion functions** — type coercion with SNOBOL failure semantics
6. **`INPUT` variable read** — wire `INPUT$` atom to `read-line`; handle in `=` INVOKE

**Deliverable:** ~6 targeted fixes. No new architecture.

---

### Sprint 9 — Pattern Completeness (1–2 sessions)
Finish the scanner and engine gap.

1. **`BREAKX`** — scanner: like BREAK but also yields each char in the break set as a position; engine case
2. **`NSPAN`** — scanner + constructor: greedy span with no backtrack (matches 0+)
3. **`BAL`** — engine node: balanced parentheses, multi-yield per Python reference
4. **`α` / BOL and `ω` / EOL** — two zero-width positional nodes (line-aware POS/RPOS equivalents)
5. **`$` vs `.` assign differentiation** — immediate (during match) vs conditional (on overall success); needs CAPTURE split into CAPTURE-IMM and CAPTURE-COND

**Deliverable:** Pattern engine reaches parity with Python pure backend on all non-regex primitives.

---

### Sprint 10 — Operator Completeness (1 session)
Wire the parsed-but-unhandled operators.

1. **`~P` optional** — `INVOKE 'tilde` → `(ALT P ε)` in INVOKE table
2. **`P & Q` conjunction** — `INVOKE 'and` → match P and Q at same position, succeed if both match same span (ρ semantics)
3. **`@N` cursor assign** — immediate position capture during match (Θ semantics); emit as `(CURSOR-IMM N)` node
4. **`%N` conditional cursor** — conditional position capture (θ semantics)
5. **`*expr` deferred guard** — wrap expression in a pattern node that evaluates at match time (Λ semantics); fixes EQ guard pruning

**Deliverable:** All grammar operators have working INVOKE dispatch. Enables the commented-out `match-2` wolf test.

---

### Sprint 11 — Data Structures & CONVERT (1 session)
Make TABLE, ARRAY, and DATA production-ready.

1. **TABLE indexed access** — `T<KEY>` and `T[KEY]` via `ndx` emitter path; INVOKE for table get/set
2. **ARRAY indexed access** — `A[I]` integer-indexed; bounds check
3. **`CONVERT`** — full implementation: string→integer, integer→string, etc. using SNOBOL failure semantics
4. **`PROTOTYPE`** — return the prototype string of a DATA or ARRAY
5. **`DATA` DATATYPE dispatch** — wire user-defined types into the DATATYPE multimethod
6. **`FIELD`** — proper field accessor for DATA instances

**Deliverable:** TABLE and ARRAY usable in real programs. DATA types fully introspectable.

---

### Sprint 12 — I/O & Runtime Polish (1 session)
Bring the runtime up to usable for real SNOBOL4 programs.

1. **`INPUT` read** — `INPUT =` reads next line from `*in*`; `INPUT(N,U)` file association
2. **`FRETURN`** — failure return from user function (jumps to caller's `:F` branch)
3. **`NRETURN`** — null return (function fails, but not as FRETURN)
4. **`APPLY`** — call user function by name string with arg list
5. **`ENDFILE` / `REWIND` / `DETACH`** — basic file I/O lifecycle stubs upgraded to real streams
6. **`END` label** — explicit end-of-program detection in `RUN`

**Deliverable:** Standard SNOBOL4 programs that read stdin and use functions work end-to-end.

---

### Sprint 13 — Full Program Validation (1–2 sessions)
Port and run a real SNOBOL4 reference program.

Candidates from `snobol4ref/`:
- `test/atn.sno` — ATN parser (uses FENCE, ARBNO, DATA, tables, recursion)
- `demos/eliza.txt` — ELIZA chatbot (pattern-heavy)
- `snolib/not.sno` — NOT(P) implementation (uses FENCE, UNIQUE, DEFINE)

**Deliverable:** At least one non-trivial reference program runs correctly, end-to-end, in SNOBOL4clojure.

---

## Priority Matrix

| Sprint | Effort | Value | Risk |
|--------|--------|-------|------|
| 8 — Quick Wins | Low | High | Low |
| 9 — Pattern Completeness | Medium | High | Medium |
| 10 — Operator Completeness | Medium | High | Medium (guard eval is architectural) |
| 11 — Data Structures | Medium | Medium | Low |
| 12 — I/O & Runtime | Medium | High | Low |
| 13 — Full Program | High | Very High | Medium |

**Recommended order:** 8 → 9 → 10 → 12 → 11 → 13

Sprint 10's deferred guard (`*expr`) is the most architecturally interesting piece — it requires a new engine node type that evaluates a Clojure expression at match time, but the pattern for it (Λ in Python) is clear from the reference.

---

## Currently Disabled Tests (re-enable as sprints complete)

| Test | Sprint | Condition |
|------|--------|-----------|
| `match-2` wolf guard | 10 | `*expr` deferred guard (Λ) |
| `match-define` ANY multi-arg | 9/10 | EVAL resolves `&UCASE &LCASE` concatenation |
| `datatype-test` user DATA | 11 | DATA DATATYPE dispatch |
