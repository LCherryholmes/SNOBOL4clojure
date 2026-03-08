(ns SNOBOL4clojure.test-bootstrap
  "Grammar-worm bootstrap test suite.

   Philosophy: start at the smallest possible programs and grow by one
   construct at a time.  Every failure at length N is a fundamental bug —
   fix it before moving to N+1.

   Length bands:
     Band 0  (< 10 chars)  : bare END, empty body label
     Band 1  (~10-20)      : single assignment, literal output
     Band 2  (~20-35)      : arithmetic, concatenation, DIFFER/IDENT
     Band 3  (~35-55)      : comparison + conditional goto, EQ/LT/GT/LE/GE/NE
     Band 4  (~55-80)      : pattern match with capture, SPAN/BREAK/ANY/LEN
     Band 5  (~80-120)     : counted loop, DEFINE + RETURN, REPLACE
     Band 6  (~120-200)    : nested calls, recursive functions, ARB/ARBNO

   TDD discipline:
     Each deftest encodes ONE semantic fact.  The expected value is derived
     from the SNOBOL4 specification, Gimpel, or Cooper — not from our impl.
     A failing test means our impl is wrong.
  "
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]))

;; ── Macro ──────────────────────────────────────────────────────────────────────
(defmacro prog [& lines]
  `(RUN (CODE ~(clojure.string/join "\n" (map str lines)))))

;; ── Band 0: Bare programs ──────────────────────────────────────────────────────

(deftest band0-bare-end
  "Smallest valid program: just END.  Should complete without error."
  (is (nil? (prog "end"))))

(deftest band0-end-uppercase
  "END is uppercase in our dialect — also valid."
  (is (nil? (prog "END"))))

(deftest band0-bare-label
  "A label with no body: statement is a no-op, falls through to END."
  (prog
    "L       "
    "end")
  (is (= nil ($$ 'L))))   ; L is a label, not a variable — no binding expected

(deftest band0-comment-only
  "A comment line (starts with *) followed by END."
  (is (nil? (prog "* this is a comment" "end"))))

(deftest band0-output-empty-string
  "OUTPUT = '' emits a blank line — variable holds ''"
  (prog
    "        OUTPUT = ''"
    "end")
  ;; no assertion on stdout capture here, just no crash
  (is (= "" ($$ 'OUTPUT))))

;; ── Band 1: Single assignment + output ────────────────────────────────────────

(deftest band1-assign-int-literal
  "I = 42 — integer variable holds 42"
  (prog
    "        I = 42"
    "end")
  (is (= 42 ($$ 'I))))

(deftest band1-assign-negative-int
  "I = -7 — negative integer"
  (prog
    "        I = -7"
    "end")
  (is (= -7 ($$ 'I))))

(deftest band1-assign-zero
  "I = 0"
  (prog
    "        I = 0"
    "end")
  (is (= 0 ($$ 'I))))

(deftest band1-assign-string-literal
  "S = 'hello' — string variable"
  (prog
    "        S = 'hello'"
    "end")
  (is (= "hello" ($$ 'S))))

(deftest band1-assign-empty-string
  "S = '' — empty string is a valid value"
  (prog
    "        S = ''"
    "end")
  (is (= "" ($$ 'S))))

(deftest band1-assign-string-with-spaces
  "S = 'hello world' — spaces inside string literal"
  (prog
    "        S = 'hello world'"
    "end")
  (is (= "hello world" ($$ 'S))))

(deftest band1-assign-real-literal
  "A = 3.14 — real variable"
  (prog
    "        A = 3.14"
    "end")
  (is (= 3.14 ($$ 'A))))

(deftest band1-assign-var-to-var
  "J = I — copies integer value"
  (prog
    "        I = 7"
    "        J = I"
    "end")
  (is (= 7 ($$ 'J))))

(deftest band1-assign-string-var-to-var
  "T = S — copies string value"
  (prog
    "        S = 'alpha'"
    "        T = S"
    "end")
  (is (= "alpha" ($$ 'T))))

(deftest band1-unassigned-var-is-empty-string
  "Unassigned variable reads as empty string ''"
  (prog
    "        S = X"   ; X unassigned — should be ""
    "end")
  (is (= "" ($$ 'S))))

(deftest band1-output-integer
  "OUTPUT = I prints integer; I holds 5"
  (prog
    "        I = 5"
    "        OUTPUT = I"
    "end")
  (is (= 5 ($$ 'I))))

(deftest band1-assign-string-coercion
  "I = '42' — string coerces to integer 42"
  (prog
    "        I = '42'"
    "        J = I + 1"
    "end")
  (is (= 43 ($$ 'J))))

;; ── Band 2: Arithmetic ────────────────────────────────────────────────────────

(deftest band2-add
  "I = 3 + 4 => 7"
  (prog
    "        I = 3 + 4"
    "end")
  (is (= 7 ($$ 'I))))

(deftest band2-subtract
  "I = 10 - 3 => 7"
  (prog
    "        I = 10 - 3"
    "end")
  (is (= 7 ($$ 'I))))

(deftest band2-multiply
  "I = 6 * 7 => 42"
  (prog
    "        I = 6 * 7"
    "end")
  (is (= 42 ($$ 'I))))

(deftest band2-divide-integer
  "I = 10 / 3 => 3 (integer division)"
  (prog
    "        I = 10 / 3"
    "end")
  (is (= 3 ($$ 'I))))

(deftest band2-divide-exact
  "I = 12 / 4 => 3"
  (prog
    "        I = 12 / 4"
    "end")
  (is (= 3 ($$ 'I))))

(deftest band2-real-divide
  "A = 10.0 / 4.0 => 2.5"
  (prog
    "        A = 10.0 / 4.0"
    "end")
  (is (= 2.5 ($$ 'A))))

(deftest band2-add-real-and-int
  "A = 1.5 + 2 => 3.5 (real wins)"
  (prog
    "        A = 1.5 + 2"
    "end")
  (is (= 3.5 ($$ 'A))))

(deftest band2-chained-arith
  "I = 2 + 3 * 4 — SNOBOL4 is left-to-right: (2+3)*4 = 20"
  (prog
    "        I = 2 + 3 * 4"
    "end")
  ;; SNOBOL4 has NO operator precedence — strictly left to right
  (is (= 20 ($$ 'I))))

(deftest band2-unary-minus
  "I = -5 used in arithmetic"
  (prog
    "        I = -(5)"
    "end")
  (is (= -5 ($$ 'I))))

(deftest band2-unary-plus
  "I = +(3) — unary plus is identity"
  (prog
    "        I = +(3)"
    "end")
  (is (= 3 ($$ 'I))))

(deftest band2-modulo-remdr
  "REMDR(10,3) => 1"
  (prog
    "        I = REMDR(10,3)"
    "end")
  (is (= 1 ($$ 'I))))

(deftest band2-concat-two-literals
  "S = 'hello' ' world' => 'hello world'"
  (prog
    "        S = 'hello' ' world'"
    "end")
  (is (= "hello world" ($$ 'S))))

(deftest band2-concat-three-parts
  "S = 'a' 'b' 'c' => 'abc'"
  (prog
    "        S = 'a' 'b' 'c'"
    "end")
  (is (= "abc" ($$ 'S))))

(deftest band2-concat-with-var
  "S = 'hello'; T = S ' world'"
  (prog
    "        S = 'hello'"
    "        T = S ' world'"
    "end")
  (is (= "hello world" ($$ 'T))))

(deftest band2-concat-int-and-string
  "S = 'item' I — integer coerces to string for concat"
  (prog
    "        I = 42"
    "        S = 'item' I"
    "end")
  (is (= "item42" ($$ 'S))))

(deftest band2-size-of-string
  "SIZE('hello') => 5"
  (prog
    "        I = SIZE('hello')"
    "end")
  (is (= 5 ($$ 'I))))

(deftest band2-size-of-empty
  "SIZE('') => 0"
  (prog
    "        I = SIZE('')"
    "end")
  (is (= 0 ($$ 'I))))

(deftest band2-ident-true
  "IDENT('abc','abc') succeeds — same strings"
  (prog
    "        IDENT('abc','abc') :S(YES)F(NO)"
    "YES     R = 'yes'          :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band2-ident-false
  "IDENT('abc','xyz') fails — different strings"
  (prog
    "        IDENT('abc','xyz') :S(YES)F(NO)"
    "YES     R = 'yes'          :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band2-differ-true
  "DIFFER('abc','xyz') succeeds — they differ"
  (prog
    "        DIFFER('abc','xyz') :S(YES)F(NO)"
    "YES     R = 'yes'           :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band2-differ-false
  "DIFFER('abc','abc') fails — they are the same"
  (prog
    "        DIFFER('abc','abc') :S(YES)F(NO)"
    "YES     R = 'yes'           :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

;; ── Band 3: Conditionals and goto ─────────────────────────────────────────────

(deftest band3-goto-unconditional
  ":(LABEL) skips everything between"
  (prog
    "        R = 'before'"
    "        :(SKIP)"
    "        R = 'SHOULD NOT REACH'"
    "SKIP    "
    "end")
  (is (= "before" ($$ 'R))))

(deftest band3-eq-true
  "EQ(5,5) succeeds"
  (prog
    "        EQ(5,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-eq-false
  "EQ(5,6) fails"
  (prog
    "        EQ(5,6) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band3-ne-true
  "NE(3,7) succeeds"
  (prog
    "        NE(3,7) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-lt-true
  "LT(3,7) succeeds"
  (prog
    "        LT(3,7) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-lt-false
  "LT(7,3) fails"
  (prog
    "        LT(7,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band3-gt-true
  "GT(7,3) succeeds"
  (prog
    "        GT(7,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-le-equal
  "LE(5,5) succeeds (equal counts)"
  (prog
    "        LE(5,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-ge-greater
  "GE(6,5) succeeds"
  (prog
    "        GE(6,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-negation-tilde
  "~EQ(5,5) — negation: EQ succeeds so ~EQ fails"
  (prog
    "        ~EQ(5,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band3-negation-ne-with-tilde
  "~NE(5,5) — ~NE: NE fails so ~NE succeeds"
  (prog
    "        ~NE(5,5) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-lgt-true
  "LGT('b','a') — lexical greater than: 'b' > 'a'"
  (prog
    "        LGT('b','a') :S(YES)F(NO)"
    "YES     R = 'yes'    :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-lgt-false
  "LGT('a','b') fails"
  (prog
    "        LGT('a','b') :S(YES)F(NO)"
    "YES     R = 'yes'    :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band3-s-branch-only
  "Statement with :S but no :F — failure falls through"
  (prog
    "        R = 'start'"
    "        EQ(1,2) :S(SKIP)"     ; fails — falls through
    "        R = 'fell-through'"
    "SKIP    "
    "end")
  (is (= "fell-through" ($$ 'R))))

(deftest band3-f-branch-only
  "Statement with :F but no :S — success falls through"
  (prog
    "        R = 'start'"
    "        EQ(1,1) :F(SKIP)"     ; succeeds — falls through
    "        R = 'fell-through'"
    "SKIP    "
    "end")
  (is (= "fell-through" ($$ 'R))))

(deftest band3-assignment-as-condition
  "Assignment always succeeds — :S branch taken"
  (prog
    "        I = 5   :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band3-failed-assignment-is-nil
  "Failed function result causes :F branch"
  (prog
    "        I = LT(5,3)   :S(YES)F(NO)"  ; LT fails, so whole assignment fails
    "YES     R = 'yes'        :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

;; ── Band 4: Pattern matching ───────────────────────────────────────────────────

(deftest band4-literal-match-success
  "'hello' 'ell' — literal substring match succeeds"
  (prog
    "        'hello' 'ell' :S(YES)F(NO)"
    "YES     R = 'yes'     :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band4-literal-match-failure
  "'hello' 'xyz' — literal not present"
  (prog
    "        'hello' 'xyz' :S(YES)F(NO)"
    "YES     R = 'yes'     :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band4-literal-capture-dot
  "Capture matched substring with ."
  (prog
    "        'hello world' 'world' . CAP"
    "end")
  (is (= "world" ($$ 'CAP))))

(deftest band4-literal-capture-dollar
  "Immediate-assign captured substring with $"
  (prog
    "        'hello world' 'hello' $ CAP"
    "end")
  (is (= "hello" ($$ 'CAP))))

(deftest band4-len-1-match
  "LEN(1) matches any single char"
  (prog
    "        'abc' LEN(1) . C"
    "end")
  (is (= "a" ($$ 'C))))

(deftest band4-len-3-match
  "LEN(3) matches first 3 chars"
  (prog
    "        'abcde' LEN(3) . C"
    "end")
  (is (= "abc" ($$ 'C))))

(deftest band4-len-zero
  "LEN(0) matches empty string at position 0"
  (prog
    "        'abc' LEN(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'        :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R)))
  (is (= ""    ($$ 'C))))

(deftest band4-any-vowel
  "ANY('aeiou') matches first vowel"
  (prog
    "        'hello' ANY('aeiou') . V"
    "end")
  (is (= "e" ($$ 'V))))

(deftest band4-any-at-start
  "ANY matches first char when it is in set"
  (prog
    "        'aeiou' ANY('aeiou') . V"
    "end")
  (is (= "a" ($$ 'V))))

(deftest band4-any-miss
  "ANY fails when no char in set"
  (prog
    "        'xyz' ANY('aeiou') :S(YES)F(NO)"
    "YES     R = 'yes'          :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band4-notany-basic
  "NOTANY('aeiou') matches first consonant"
  (prog
    "        'hello' NOTANY('aeiou') . C"
    "end")
  (is (= "h" ($$ 'C))))

(deftest band4-span-basic
  "SPAN('abc') matches maximal run of a/b/c chars"
  (prog
    "        'abcxyz' SPAN('abc') . W"
    "end")
  (is (= "abc" ($$ 'W))))

(deftest band4-span-full
  "SPAN matches all when all chars are in set"
  (prog
    "        'aaaa' SPAN('a') . W"
    "end")
  (is (= "aaaa" ($$ 'W))))

(deftest band4-break-basic
  "BREAK('xyz') captures up to first x/y/z"
  (prog
    "        'abcxyz' BREAK('xyz') . B"
    "end")
  (is (= "abc" ($$ 'B))))

(deftest band4-break-empty
  "BREAK when first char is break char — captures ''"
  (prog
    "        'xabc' BREAK('xyz') . B :S(YES)F(NO)"
    "YES     R = 'yes'              :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R)))
  (is (= ""    ($$ 'B))))

(deftest band4-pos-anchors
  "POS(0) anchors to start of string"
  (prog
    "        'abc' POS(0) 'a' . C"
    "end")
  (is (= "a" ($$ 'C))))

(deftest band4-pos-nonzero
  "POS(2) skips first 2 chars"
  (prog
    "        'abcde' POS(2) LEN(1) . C"
    "end")
  (is (= "c" ($$ 'C))))

(deftest band4-rpos-zero
  "RPOS(0) anchors to end — pattern must end at end of string"
  (prog
    "        'abcde' RPOS(0) :S(YES)F(NO)"
    "YES     R = 'yes'       :(END)"
    "NO      R = 'no'"
    "END")
  ;; RPOS(0) as entire pattern — always succeeds (matches empty at end)
  (is (= "yes" ($$ 'R))))

(deftest band4-tab-basic
  "TAB(3) matches first 3 chars (positions 0-2)"
  (prog
    "        'abcde' TAB(3) . C"
    "end")
  (is (= "abc" ($$ 'C))))

(deftest band4-rtab-basic
  "RTAB(2) matches all but last 2 chars"
  (prog
    "        'abcde' RTAB(2) . C"
    "end")
  (is (= "abc" ($$ 'C))))

(deftest band4-rem-basic
  "REM captures everything from current position to end"
  (prog
    "        'abcde' 'bc' REM . C"
    "end")
  (is (= "de" ($$ 'C))))

(deftest band4-pattern-replace
  "Subject PAT = REPL — replaces matched portion"
  (prog
    "        S = 'hello world'"
    "        S 'world' = 'SNOBOL'"
    "end")
  (is (= "hello SNOBOL" ($$ 'S))))

(deftest band4-pattern-replace-first-only
  "Replace only replaces first occurrence (SEARCH stops after first match)"
  (prog
    "        S = 'aaa'"
    "        S 'a' = 'b'"
    "end")
  (is (= "baa" ($$ 'S))))

(deftest band4-anchored-match-pos0
  "POS(0) 'abc' RPOS(0) — anchored full match"
  (prog
    "        'abc' POS(0) 'abc' RPOS(0) :S(YES)F(NO)"
    "YES     R = 'yes'                  :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band4-anchored-match-fails-longer
  "POS(0) 'ab' RPOS(0) — fails when string is 'abc' (3 chars not 2)"
  (prog
    "        'abc' POS(0) 'ab' RPOS(0) :S(YES)F(NO)"
    "YES     R = 'yes'                  :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest band4-concat-in-pattern
  "Concatenation of patterns: 'a' 'b' matches 'ab'"
  (prog
    "        'abc' 'a' 'b' :S(YES)F(NO)"
    "YES     R = 'yes'     :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band4-alternation
  "'hello' ('he' | 'wo') . C — alternation, first alternative succeeds"
  (prog
    "        'hello' ('he' | 'wo') . C"
    "end")
  (is (= "he" ($$ 'C))))

(deftest band4-alternation-second
  "'world' ('he' | 'wo') . C — first alt fails, second succeeds"
  (prog
    "        'world' ('he' | 'wo') . C"
    "end")
  (is (= "wo" ($$ 'C))))

(deftest band4-arb-basic
  "ARB matches shortest string between two literals"
  (prog
    "        'hello world' 'h' ARB . C 'o'"
    "end")
  ;; ARB is greedy-shortest: 'ell' is shortest ARB between 'h' and 'o'
  (is (= "ell" ($$ 'C))))

(deftest band4-arb-empty
  "ARB can match empty string"
  (prog
    "        'ho' 'h' ARB . C 'o'"
    "end")
  (is (= "" ($$ 'C))))

(deftest band4-breakx-vs-break
  "BREAKX slides past break-char on backtrack; BREAK does not"
  ;; BREAKX('x') 'x' 'after' on 'aaxbafter':
  ;; BREAK would match up to first 'x', consuming "aa", then 'x' eats it,
  ;; then 'after' must match "bafter" — fails, no retry.
  ;; BREAKX slides: retries after consuming "aax", then 'x' matches second 'x'... etc.
  ;; Simpler: BREAKX('a') . C 'a' on 'bba' — captures 'bb'
  (prog
    "        'bba' BREAKX('a') . C 'a' :S(YES)F(NO)"
    "YES     R = 'yes'                  :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R)))
  (is (= "bb"  ($$ 'C))))

;; ── Band 5: Loops and functions ───────────────────────────────────────────────

(deftest band5-simple-counted-loop
  "Classic SNOBOL4 counted loop: I from 1 to 5"
  (prog
    "        I = 1"
    "        S = ''"
    "LOOP    S = S I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (= "12345" ($$ 'S))))

(deftest band5-loop-zero-iterations
  "Loop condition false from the start — body never executes"
  (prog
    "        I = 10"
    "        S = 'before'"
    "LOOP    S = 'body'"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (= "body" ($$ 'S))))  ; body executes once (goto checked AFTER body)

(deftest band5-accumulate-sum
  "Sum 1..10 = 55"
  (prog
    "        I = 1"
    "        SUM = 0"
    "LOOP    SUM = SUM + I"
    "        I = I + 1"
    "        LE(I,10) :S(LOOP)"
    "end")
  (is (= 55 ($$ 'SUM))))

(deftest band5-define-and-call
  "DEFINE a function, call it"
  (prog
    "        DEFINE('DOUBLE(N)') :(DOUBLE_END)"
    "DOUBLE  DOUBLE = N * 2      :(RETURN)"
    "DOUBLE_END"
    "        R = DOUBLE(7)"
    "end")
  (is (= 14 ($$ 'R))))

(deftest band5-define-recursive
  "Recursive FACTORIAL — FACT(5) = 120"
  (prog
    "        DEFINE('FACT(N)')    :(FACT_END)"
    "FACT    EQ(N,0)              :S(FACT_BASE)"
    "        FACT = N * FACT(N - 1) :(RETURN)"
    "FACT_BASE FACT = 1           :(RETURN)"
    "FACT_END"
    "        R = FACT(5)"
    "end")
  (is (= 120 ($$ 'R))))

(deftest band5-define-with-locals
  "DEFINE with local variable — local is private to call"
  (prog
    "        DEFINE('ADDONE(X)TEMP') :(ADDONE_END)"
    "ADDONE  TEMP = X + 1"
    "        ADDONE = TEMP          :(RETURN)"
    "ADDONE_END"
    "        R = ADDONE(41)"
    "end")
  (is (= 42 ($$ 'R))))

(deftest band5-freturn-failure
  "FRETURN causes calling statement to fail"
  (prog
    "        DEFINE('TRYFAIL(N)') :(TF_END)"
    "TRYFAIL EQ(N,0) :S(TF_FAIL)"
    "        TRYFAIL = 'ok'      :(RETURN)"
    "TF_FAIL                     :(FRETURN)"
    "TF_END"
    "        R = TRYFAIL(0) :S(YES)F(NO)"
    "YES     R = 'success'   :(END)"
    "NO      R = 'failed'"
    "END")
  (is (= "failed" ($$ 'R))))

(deftest band5-replace-basic
  "REPLACE(S, from, to) — character-level translation"
  (prog
    "        R = REPLACE('hello', 'aeiou', 'AEIOU')"
    "end")
  (is (= "hEllO" ($$ 'R))))

(deftest band5-replace-all-chars
  "REPLACE uppercase to lowercase"
  (prog
    "        R = REPLACE('HELLO', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"
    "end")
  (is (= "hello" ($$ 'R))))

(deftest band5-size-function
  "SIZE returns character count"
  (prog
    "        I = SIZE('hello world')"
    "end")
  (is (= 11 ($$ 'I))))

(deftest band5-ascii-and-char
  "ASCII('A') = 65; CHAR(65) = 'A'"
  (prog
    "        N = ASCII('A')"
    "        C = CHAR(65)"
    "end")
  (is (= 65      ($$ 'N)))
  (is (= "A"     ($$ 'C))))

(deftest band5-integer-function
  "INTEGER('42') succeeds and converts; INTEGER('abc') fails"
  (prog
    "        I = INTEGER('42')  :S(YES)F(NO)"
    "YES     R = I              :(END)"
    "NO      R = 'fail'"
    "END")
  (is (= 42 ($$ 'R))))

(deftest band5-trim-function
  "TRIM removes trailing spaces"
  (prog
    "        S = TRIM('hello   ')"
    "end")
  (is (= "hello" ($$ 'S))))

(deftest band5-reverse-function
  "REVERSE('hello') = 'olleh'"
  (prog
    "        S = REVERSE('hello')"
    "end")
  (is (= "olleh" ($$ 'S))))

(deftest band5-dupl-function
  "DUPL('ab',3) = 'ababab'"
  (prog
    "        S = DUPL('ab',3)"
    "end")
  (is (= "ababab" ($$ 'S))))

;; ── Band 6: Data structures and advanced patterns ─────────────────────────────

(deftest band6-table-basic
  "TABLE creates a table; subscript read/write works"
  (prog
    "        T = TABLE()"
    "        T<'key'> = 'value'"
    "        R = T<'key'>"
    "end")
  (is (= "value" ($$ 'R))))

(deftest band6-table-multiple-keys
  "TABLE with multiple distinct keys"
  (prog
    "        T = TABLE()"
    "        T<'a'> = 1"
    "        T<'b'> = 2"
    "        T<'c'> = 3"
    "        R = T<'a'> T<'b'> T<'c'>"
    "end")
  (is (= "123" ($$ 'R))))

(deftest band6-table-overwrite
  "Writing same key twice — second value wins"
  (prog
    "        T = TABLE()"
    "        T<'x'> = 'first'"
    "        T<'x'> = 'second'"
    "        R = T<'x'>"
    "end")
  (is (= "second" ($$ 'R))))

(deftest band6-array-basic
  "ARRAY(3) — index 1..3"
  (prog
    "        A = ARRAY(3)"
    "        A<1> = 'one'"
    "        A<2> = 'two'"
    "        A<3> = 'three'"
    "        R = A<1> ',' A<2> ',' A<3>"
    "end")
  (is (= "one,two,three" ($$ 'R))))

(deftest band6-array-integer-values
  "ARRAY with integer elements"
  (prog
    "        A = ARRAY(5)"
    "        I = 1"
    "LOOP    A<I> = I * I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "        R = A<1> A<2> A<3> A<4> A<5>"
    "end")
  (is (= "14916" ($$ 'R))))

(deftest band6-arbno-basic
  "ARBNO matches zero or more repetitions"
  (prog
    "        PAT = SPAN('0123456789')"
    "        '123,456,789' PAT ARBNO(',' PAT) :S(YES)F(NO)"
    "YES     R = 'yes'                         :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R))))

(deftest band6-datatype-string
  "DATATYPE('hello') = 'string'"
  (prog
    "        R = DATATYPE('hello')"
    "end")
  (is (= "string" ($$ 'R))))

(deftest band6-datatype-integer
  "DATATYPE(42) = 'integer'"
  (prog
    "        R = DATATYPE(42)"
    "end")
  (is (= "integer" ($$ 'R))))

(deftest band6-datatype-pattern
  "DATATYPE(ANY('abc')) = 'pattern'"
  (prog
    "        P = ANY('abc')"
    "        R = DATATYPE(P)"
    "end")
  (is (= "pattern" ($$ 'R))))

(deftest band6-datatype-array
  "DATATYPE(ARRAY(3)) = 'array'"
  (prog
    "        R = DATATYPE(ARRAY(3))"
    "end")
  (is (= "array" ($$ 'R))))

(deftest band6-convert-int-to-string
  "CONVERT(42,'string') = '42'"
  (prog
    "        R = CONVERT(42,'string')"
    "end")
  (is (= "42" ($$ 'R))))

(deftest band6-convert-string-to-int
  "CONVERT('42','integer') = 42"
  (prog
    "        R = CONVERT('42','integer')"
    "end")
  (is (= 42 ($$ 'R))))

(deftest band6-data-pdd
  "DATA/FIELD: program-defined data type"
  (prog
    "        DATA('POINT(X,Y)')"
    "        P = POINT(3,4)"
    "        R1 = X(P)"
    "        R2 = Y(P)"
    "end")
  (is (= 3 ($$ 'R1)))
  (is (= 4 ($$ 'R2))))

(deftest band6-pdd-field-update
  "PDD field assignment via accessor"
  (prog
    "        DATA('NODE(VAL,NEXT)')"
    "        N = NODE(10,'')"
    "        VAL(N) = 99"
    "        R = VAL(N)"
    "end")
  (is (= 99 ($$ 'R))))

(deftest band6-sort-array
  "SORT(T) on table — doesn't error"
  (prog
    "        T = TABLE()"
    "        T<'c'> = 3"
    "        T<'a'> = 1"
    "        T<'b'> = 2"
    "        A = SORT(T)"
    "        R = DATATYPE(A)"
    "end")
  (is (= "array" ($$ 'R))))

(deftest band6-copy-array
  "COPY(A) produces an independent duplicate"
  (prog
    "        A = ARRAY(2)"
    "        A<1> = 'original'"
    "        B = COPY(A)"
    "        B<1> = 'copy'"
    "        R1 = A<1>"
    "        R2 = B<1>"
    "end")
  (is (= "original" ($$ 'R1)))
  (is (= "copy"     ($$ 'R2))))

;; ── Edge cases the worm finds first ───────────────────────────────────────────

(deftest edge-empty-pattern-match-subject
  "Match against empty subject — only LEN(0) and POS(0)/RPOS(0) can match"
  (prog
    "        '' LEN(0) . C :S(YES)F(NO)"
    "YES     R = 'yes'     :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes" ($$ 'R)))
  (is (= ""    ($$ 'C))))

(deftest edge-any-empty-subject-fails
  "ANY on empty subject always fails"
  (prog
    "        '' ANY('abc') :S(YES)F(NO)"
    "YES     R = 'yes'     :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "no" ($$ 'R))))

(deftest edge-span-whole-string
  "SPAN matches entire string when all chars in set"
  (prog
    "        'aaabbb' SPAN('ab') . C :S(YES)F(NO)"
    "YES     R = 'yes'               :(END)"
    "NO      R = 'no'"
    "END")
  (is (= "yes"    ($$ 'R)))
  (is (= "aaabbb" ($$ 'C))))

(deftest edge-integer-string-coercion-concat
  "Integer coerces to string in concatenation context"
  (prog
    "        I = 42"
    "        S = 'answer:' I"
    "end")
  (is (= "answer:42" ($$ 'S))))

(deftest edge-multiple-assignments-overwrite
  "Each assignment overwrites previous value"
  (prog
    "        I = 1"
    "        I = 2"
    "        I = 3"
    "end")
  (is (= 3 ($$ 'I))))

(deftest edge-self-concatenation
  "S = S 'x' — variable appears on both sides"
  (prog
    "        S = 'a'"
    "        S = S 'b'"
    "        S = S 'c'"
    "end")
  (is (= "abc" ($$ 'S))))

(deftest edge-self-arithmetic
  "I = I + 1 — variable on both sides"
  (prog
    "        I = 10"
    "        I = I + 5"
    "end")
  (is (= 15 ($$ 'I))))

(deftest edge-goto-past-end
  "Unconditional goto END label halts program"
  (prog
    "        R = 'set'"
    "        :(END)"
    "        R = 'never'"
    "END")
  (is (= "set" ($$ 'R))))

(deftest edge-label-as-goto-target
  "Forward reference to label works"
  (prog
    "        :(SKIP)"
    "        R = 'never'"
    "SKIP    R = 'reached'"
    "end")
  (is (= "reached" ($$ 'R))))

(deftest edge-pattern-var-reuse
  "Pattern variable can be used multiple times in sequence"
  (prog
    "        P = LEN(1)"
    "        'abc' P . C1"
    "        'xyz' P . C2"
    "end")
  (is (= "a" ($$ 'C1)))
  (is (= "x" ($$ 'C2))))

(deftest edge-nested-parens-expr
  "Nested parentheses in expression"
  (prog
    "        I = (2 + 3) * (4 + 1)"
    "end")
  ;; Left-to-right within each paren group: (2+3)=5, (4+1)=5, 5*5=25
  (is (= 25 ($$ 'I))))

(deftest edge-string-number-arithmetic
  "'3' + '4' — both strings coerce to integer for arithmetic"
  (prog
    "        I = '3' + '4'"
    "end")
  (is (= 7 ($$ 'I))))

(deftest edge-unassigned-in-concat-is-empty
  "Unassigned variable reads as '' in concat"
  (prog
    "        S = 'hello' UNASSIGNED 'world'"
    "end")
  (is (= "helloworld" ($$ 'S))))
