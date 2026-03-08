# SNOBOL4clojure — Feature Assessment

**Baseline:** 1865 tests / 4018 assertions / 0 failures  
**Commit:** `875762c` (Stage 23D — JVM bytecode generation)  
**Last updated:** Session 13d

---

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Implemented, tested, catalog coverage |
| 🟡 | Implemented but no dedicated catalog tests, or minor gaps |
| ⚠️  | Exists but wiring/dispatch gap or no tests at all |
| ❌ | Stub — returns nil / ε silently |

---

## 1. Arithmetic Operators

| Operator | Arity | Status | Notes |
|---|---|---|---|
| `+` `-` `*` `/` | binary | ✅ | Integer-preserving; `/` throws on zero |
| `+` `-` | unary | ✅ | Numeric sign |
| `**` | binary | ✅ | Power via `Math/pow` |
| `~` | unary | ✅ | Logical negation — flips S/F |

---

## 2. Numeric Comparison Functions

| Function | Status | Test Coverage |
|---|---|---|
| `EQ` `NE` `LT` `LE` `GT` `GE` | ✅ | `t_compare.clj` — 9 cases each, all sign combinations |
| `IDENT` `DIFFER` | ✅ | `t_compare.clj` — 5 cases each |
| `LGT` | ✅ | `t_compare.clj` — 5 cases |
| `LEQ` `LNE` `LLE` `LLT` `LGE` | ⚠️ | Defined via `primitive` macro but **absent from INVOKE dispatch** — reachable only via accidental namespace fallthrough; no catalog tests |

---

## 3. String Functions

| Function | Status | Test Coverage |
|---|---|---|
| `SIZE` | ✅ | `t_string.clj` — 5 cases |
| `TRIM` | ✅ | `t_string.clj` — 3 cases |
| `REVERSE` | ✅ | `t_string.clj` — 3 cases |
| `DUPL` | ✅ | `t_string.clj` — 3 cases |
| `LPAD` `RPAD` | ✅ | `test_cooper.clj` — 3 cases each; 3-arg fill-char form |
| `SUBSTR` | 🟡 | Works; used throughout `t_worm_*` but no dedicated catalog file |
| `REPLACE` | ✅ | `t_convert.clj` — 5 cases |

---

## 4. Conversion & Type Functions

| Function | Status | Test Coverage |
|---|---|---|
| `INTEGER` | ✅ | `t_convert.clj` — 3 cases (str, int, real) |
| `REAL` | ✅ | `t_convert.clj` — 2 cases |
| `STRING` | ✅ | `t_convert.clj` — 2 cases |
| `ASCII` | ✅ | `t_convert.clj` — 7 cases |
| `CHAR` | ✅ | `t_convert.clj` — 5 cases |
| `CONVERT` | ✅ | `t_convert.clj` — via DATATYPE |
| `REMDR` | ✅ | `t_arith.clj` |
| `DATATYPE` | ✅ | `t_convert.clj` — 7 types (string, integer, real, pattern, array, table, user-defined) |

---

## 5. Pattern Primitives

| Pattern | Status | Test Coverage |
|---|---|---|
| `LEN` `POS` `RPOS` `TAB` `RTAB` | ✅ | `t_patterns_prim.clj` |
| `ANY` `NOTANY` `SPAN` `BREAK` | ✅ | `t_patterns_prim.clj` |
| `NSPAN` | 🟡 | Implemented; exercised in worm tests; no dedicated catalog cases |
| `BREAKX` | ✅ | `t_patterns_ext.clj` — 7 cases |
| `BOL` `EOL` | ✅ | `t_patterns_prim.clj` |
| `ARB` | ✅ | `t_patterns_prim.clj` |
| `ARBNO` | ✅ | `t_patterns_ext.clj` — 6 cases |
| `REM` | ✅ | `t_patterns_prim.clj` |
| `BAL` | ✅ | `t_patterns_ext.clj` + `test_sprint9_bal.clj` |
| `FENCE` bare + `FENCE(P)` | ✅ | `t_patterns_ext.clj` — 3 cases |
| `ABORT` | ✅ | `t_patterns_ext.clj` — 2 cases |
| `FAIL` `SUCCEED` | ✅ | `t_patterns_prim.clj` |

---

## 6. Pattern Operators

| Operator | Status | Notes |
|---|---|---|
| Concatenation (juxtaposition) | ✅ | `SEQ` node |
| `\|` alternation | ✅ | `ALT` node |
| `~P` optional | ✅ | Sugar for `ALT P ε` |
| `P . N` conditional capture | ✅ | `t_patterns_cap.clj` |
| `P $ N` immediate capture | ✅ | `t_patterns_cap.clj` |
| `@N` cursor assign | ✅ | `t_patterns_ext.clj` — 4 cases (`CURSOR-IMM!` node) |
| `P & Q` conjunction / `CONJ` | ✅ | `t_patterns_ext.clj` — 2 cases |
| `*expr` deferred eval | ✅ | `t_patterns_adv.clj` (`DEFER!` node) |

---

## 7. Data Structures

| Feature | Status | Test Coverage |
|---|---|---|
| `TABLE` | ✅ | `t_array.clj`, `test_sprint12.clj` |
| `ARRAY` (1D, multi-dim, non-1 origin) | ✅ | `test_sprint11_array.clj` |
| `DATA` / `FIELD` (programmer-defined datatypes) | ✅ | `test_sprint12.clj` |
| `ITEM` | ✅ | Subscript read dispatch |
| `PROTOTYPE` | ✅ | `test_sprint11_array.clj` |
| `SORT` / `RSORT` | ✅ | `test_sprint12.clj` |
| `COPY` | ✅ | `test_bootstrap.clj` |

---

## 8. User-Defined Functions

| Feature | Status | Test Coverage |
|---|---|---|
| `DEFINE` | ✅ | `t_define.clj` — 16 cases |
| `RETURN` / `FRETURN` / `NRETURN` | ✅ | `t_define.clj` |
| Recursion | ✅ | `t_algorithms.clj` |
| `APPLY` | ✅ | In INVOKE dispatch |
| `ARG` | ❌ | Stub — `(defn ARG [] nil)` |
| `LOCAL` | ❌ | Stub — `(defn LOCAL [] nil)` |

---

## 9. System & I/O Functions

| Function | Status | Notes |
|---|---|---|
| `OUTPUT` / `TERMINAL` | ✅ | Core print mechanism |
| `INPUT` | ✅ | `READ-LINE!` |
| `DATE` | ✅ | Returns `java.util.Date.toString()` |
| `TIME` | ✅ | Returns `System/currentTimeMillis` |
| `BACKSPACE` `DETACH` `EJECT` `ENDFILE` `REWIND` | ❌ | Stub — returns `ε` silently; no file I/O |
| `COLLECT` `DUMP` `CLEAR` | ❌ | Stub — returns `ε` silently |
| `EXIT` `HOST` | ❌ | Stub — returns `nil` |
| `SETEXIT` `STOPTR` `TRACE` (fn form) | ❌ | Stub — returns `nil`; tracing handled separately via `trace.clj` |
| `LOAD` `UNLOAD` `OPSYN` | ❌ | Stub — returns `nil`; dynamic loading not supported |

---

## 10. Special Variables / Keywords

| Feature | Status |
|---|---|
| `&STLIMIT` / `&STCOUNT` | ✅ |
| `&ANCHOR` | ✅ |
| `&TRIM` | ✅ |
| `&FULLSCAN` | ✅ |
| `&TRACE` | ✅ |
| `&ERRTYPE` | ✅ |
| `&MAXLNGTH` | ✅ |

---

## 11. Performance Backends (Sprint 23)

| Stage | File | Speedup | Status |
|---|---|---|---|
| 23A — EDN IR cache (`CODE-memo`) | `compiler.clj` | 22× per-program, 33% suite wall-time | ✅ |
| 23B — IR→Clojure transpiler | `transpiler.clj` | 3.5–6× | ✅ |
| 23C — Clojure stack machine | `vm.clj` | 2–6× | ✅ |
| 23D — JVM bytecode generation | `jvm_codegen.clj` | 1.3–7.6× (7.6× dispatch, bounded by EVAL!) | ✅ |
| 23E — Inline EVAL! (AOT expr codegen) | — | Target 10–50× on loops | **NEXT** |

---

## Gap Summary

### Needs INVOKE wiring (exists, just not dispatched)
`LEQ` `LNE` `LLE` `LLT` `LGE` — defined via `primitive` macro, not in the `INVOKE` case table.
Any SNOBOL4 program calling these falls through to user-function lookup (accidentally works but fragile).

### Genuine stubs (no-ops, silently accepted)
`ARG`, `LOCAL`, `BACKSPACE`, `DETACH`, `EJECT`, `ENDFILE`, `REWIND`, `COLLECT`, `DUMP`, `CLEAR`, `EXIT`, `HOST`, `SETEXIT`, `STOPTR`, `LOAD`, `UNLOAD`, `OPSYN`

### No dedicated catalog tests (works, only incidentally exercised)
`SUBSTR`, `NSPAN`, `LEQ`/`LNE`/`LLE`/`LLT`/`LGE`
