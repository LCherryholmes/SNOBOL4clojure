# SNOBOL4clojure — Pattern Library

A Clojure implementation of SNOBOL4 pattern matching.
Patterns are first-class values built from composable constructors,
driven by an explicit iterative state machine.

---

## Quick start

```clojure
(require '[SNOBOL4clojure.core :refer :all
           :exclude [= + - * / num]])

;; Build a pattern
(def digits (SPAN "0123456789"))

;; Match it
(SEARCH    "abc123def" digits)   ;=> [3 6]   (found at 3..6)
(MATCH     "123def"    digits)   ;=> [0 3]   (anchored at 0)
(FULLMATCH "12345"     digits)   ;=> [0 5]   (entire string)
(FULLMATCH "12x45"     digits)   ;=> nil     (no match)

;; Extract the matched substring
(let [[start end] (SEARCH "abc123def" digits)]
  (subs "abc123def" start end))  ;=> "123"

;; Replace a match
(REPLACE "hello world" "world" "Clojure")  ;=> "hello Clojure"
```

---

## Entry points

All three entry points return `[start end]` (a half-open span) on success,
or `nil` on failure. `(subs subject start end)` extracts the matched text.

| Function | Behaviour |
|----------|-----------|
| `(SEARCH s pat)` | Slide pattern across subject; return first match |
| `(MATCH s pat)` | Anchor at position 0; equivalent to `POS(0)` + pat |
| `(FULLMATCH s pat)` | Anchor both ends; equivalent to `POS(0)` + pat + `RPOS(0)` |
| `(REPLACE s pat r)` | SEARCH + substitute replacement string; return new string or nil |

---

## Pattern constructors

### Character class

| Constructor | Matches |
|-------------|---------|
| `(ANY "abc")` | One character from the set |
| `(NOTANY "abc")` | One character NOT in the set |
| `(SPAN "abc")` | One or more characters from the set (greedy) |
| `(BREAK "abc")` | Zero or more characters NOT in the set, stopping before a member |

```clojure
(SEARCH "hello" (ANY "aeiou"))       ;=> [1 2]  ("e")
(SEARCH "hello" (SPAN "helo"))       ;=> [0 5]  ("hello")
(SEARCH "hi.there" (BREAK "."))      ;=> [0 2]  ("hi")
```

### Position

| Constructor | Matches |
|-------------|---------|
| `(POS n)` | Succeeds only at position n from left |
| `(RPOS n)` | Succeeds only at position n from right |
| `(LEN n)` | Exactly n characters |
| `(TAB n)` | Advance cursor to position n |
| `(RTAB n)` | Advance cursor to n characters from right |

```clojure
(SEARCH "hello" (POS 0))   ;=> [0 0]
(SEARCH "hello" (RPOS 0))  ;=> [5 5]  (end of string)
(SEARCH "hello" (LEN 3))   ;=> [0 3]  ("hel")
```

### Structural

| Constructor | Matches |
|-------------|---------|
| `(ARBNO pat)` | Zero or more repetitions of pat *(stub — not yet implemented)* |
| `(FENCE)` | Prevents backtracking past this point *(stub)* |
| `(FENCE pat)` | Match pat without backtracking *(stub)* |
| `ARB` | Zero or more of any character *(stub)* |
| `BAL` | Balanced parentheses *(stub)* |
| `REM` | Remainder of subject |
| `SUCCEED` | Always succeeds (zero-length) |
| `FAIL` | Always fails |

### Combination

Patterns are combined with Clojure data structures:

```clojure
;; Sequence — vector evaluates as SEQ
(EVAL '["hel" "lo"])                         ;=> SEQ pattern

;; Alternation — | operator
(EVAL '(| "cat" "dog" "bird"))               ;=> ALT pattern

;; Or build directly
(list 'SEQ (POS 0) (SPAN "0123456789") (RPOS 0))  ; anchored digits
(list 'ALT "yes"  "no"  "maybe")                   ; three choices
```

---

## Using EVAL

`EVAL` accepts either a quoted Clojure expression or a SNOBOL4 source string:

```clojure
;; From a quoted expression:
(def ident (EVAL '[(POS 0) (ANY &LCASE) (SPAN (str &LCASE &UCASE &DIGITS "-._"))]))

;; From a SNOBOL4 source string (parsed by the instaparse grammar):
(def real  (EVAL "POS(0) SPAN(&DIGITS) '.' SPAN(&DIGITS) RPOS(0)"))
```

---

## SNOBOL4 keywords

These are pre-defined strings/atoms:

| Name | Value |
|------|-------|
| `&LCASE` | `"abcdefghijklmnopqrstuvwxyz"` |
| `&UCASE` | `"ABCDEFGHIJKLMNOPQRSTUVWXYZ"` |
| `&DIGITS` | `"0123456789"` |
| `&ANCHOR` | atom; `0` = unanchored (default), non-zero = anchored |

---

## Tracing

Bind `SNOBOL4clojure.match/*trace*` to `true` to enable per-step animation:

```clojure
(binding [SNOBOL4clojure.match/*trace* true]
  (FULLMATCH "BED" (list 'SEQ (POS 0) (ANY "BFR") (SPAN "EA") "D" (RPOS 0))))
```

---

## Known limitations (work in progress)

- `ARB`, `ARBNO`, `BAL`, `FENCE` are stubs — they always fail
- Cross-SEQ backtracking: when an `ALT` child succeeds but a later `SEQ`
  element fails, the engine does not yet retry the `ALT`
- `EQ`/`NE`/etc. guards inside `ALT` branches do not prune alternatives
