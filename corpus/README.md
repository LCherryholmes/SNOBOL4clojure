# SNOBOL4clojure — Test Corpus

Three corpora used for integration testing and showcasing SNOBOL4clojure.

## `gimpel/`

Programs and library functions from **"Algorithms in SNOBOL4"** by James F. Gimpel
(Wiley, 1976). The definitive SNOBOL4 algorithm library.

- `SNOBOL4/` — CSNOBOL4-compatible versions (`.SNO` programs, `.INC` libraries)
- `SPITBOL/` — SPITBOL-compatible versions (`.SPT` programs, `.INC` libraries)
- `CONTENTS.TXT` — index of all library functions
- `README.TXT` — original readme

**135 library routines** covering sorting, searching, parsing, string processing,
arithmetic, combinatorics, and more.

**Runnable programs:**

| Program | Description | I/O |
|---------|-------------|-----|
| `BCD_EBCD.SNO` | BCD to EBCDIC conversion via REPLACE | stdin/stdout |
| `INFINIP.SNO` | Infinite-precision integer arithmetic | stdin/stdout |
| `L_ONE.SNO` | Compiler for language L1 → machine M assembly | stdin/stdout |
| `L_TWO.SNO` | Compiler for L2 with register optimisation | stdin/stdout |
| `POKER.SNO` | Five-card poker simulator | needs `POKER.IN` |
| `RPOEM.SNO` | Random poem generator | needs `RPOEM.IN` |
| `RSEASON.SNO` | Baseball season simulator (1927 Yankees) | needs `RSEASON.IN` |
| `RSTORY.SNO` | Random story generator | needs `RSTORY.IN` |
| `STONE.SNO` | Game of stones | needs `PHRASES.IN` |
| `ASM.SNO` | Assembler for machine M | needs `ASMTEMP` |

## `aisnobol/`

Programs from **"Artificial Intelligence Programming in SNOBOL4"** by Michael Shafto
(Lawrence Erlbaum Associates, 1983). Implements SNOLISPIST — a complete
Lisp-style list-processing system in SNOBOL4.

| File | Description |
|------|-------------|
| `SNOCORE.INC` | Core SNOLISPIST library (CONS/CAR/CDR, OPSYN, CODE, APPLY, ...) |
| `SNOLIB.INC` / `SNOLIB.IDX` | Extended library, dynamically loaded |
| `TEST.SNO` | Comprehensive test harness for all SNOLISPIST functions |
| `HSORT.SNO` | Hoare quicksort demonstration |
| `ENDING.SNO` | English ending analysis (Winograd 1972) |
| `WANG.SNO` | Wang's theorem-proving algorithm |
| `KALAH.SNO` | Kalah board game |
| `SIR.SNO` | Raphael's SIR semantic information retrieval |
| `ATN.SNO` | Augmented Transition Network compiler |
| `BUILDLIB.SNO` | Build index to function library |
| `*.IN` | Input data files for programs that need them |

## `beauty/`

**beauty.sno** — A SNOBOL4 beautifier/pretty-printer by Lon Cherryholmes (2002–2005).

Reads SNOBOL4 source from stdin, builds a full parse tree, and emits reformatted
SNOBOL4 to stdout. 

**The flagship demo:**

```bash
# Beautify beauty.sno using itself — three copies in memory simultaneously:
# 1. The source being read (stdin = beauty.sno)
# 2. The program executing (beauty.sno running in the interpreter)  
# 3. The parse tree of the input (AST of beauty.sno in the heap)
cat corpus/beauty/beauty.sno | snobol4clojure corpus/beauty/beauty.sno
```

The 19 `-INCLUDE` files (`global.inc`, `is.inc`, `FENCE.inc`, etc.) are part of
Lon's private SNOBOL4 library and are not redistributed here. Place them in
`corpus/beauty/` alongside `beauty.sno` to run.

## `lon/` ← git submodule → [LCherryholmes/SNOBOL4](https://github.com/LCherryholmes/SNOBOL4)

Lon Cherryholmes's personal SNOBOL4 library, maintained as a separate repository
and included here as a git submodule. Clone with:

```bash
git clone --recurse-submodules https://github.com/LCherryholmes/SNOBOL4clojure.git
# or after a plain clone:
git submodule update --init --recursive
```

Key contents:

| Path | Description |
|------|-------------|
| `inc/` | ~90 library `.inc` files |
| `inc/global.inc` … `inc/trace.inc` | The 19 includes required by `beauty.sno` |
| `inc/TZ/` | 66-file timezone/transpiler system |
| `sno/` | ~60 programs: beauty, bootstrap, data tools, SQL, web |
| `rinky/` | 33 social-media listener programs (Twitter/Facebook/etc., ~2010) |
| `ebnf/` | SNOBOL4 EBNF grammar definitions |

The `beauty/` directory above depends on `lon/inc/` for its 19 `-INCLUDE` files.
Once `-INCLUDE` preprocessing is implemented (Sprint 25A), the include path for
beauty.sno is `corpus/lon/inc/`.
