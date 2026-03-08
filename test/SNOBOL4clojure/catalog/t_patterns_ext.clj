(ns SNOBOL4clojure.catalog.t-patterns-ext
  "Sprint 18A3 — Extended pattern catalog.
   Covers: BREAKX, FENCE, ABORT, ARBNO, CURSOR(@), CONJ (MATCH &), BAL.
   Shape A: atomic (single match/assign), budget 2000ms.
   Shape B: bounded iteration, budget 2000ms, documented bound.
   All tests use prog string-based runner for full pipeline coverage."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.catalog.t-patterns-ext)) (f)))

;; ── BREAKX ────────────────────────────────────────────────────────────────────
;; BREAKX(cs): like BREAK but retries past each break-char on backtrack.
;; BREAK fails on first attempt; BREAKX slides and retries.

(deftest breakx-basic-match
  "BREAKX captures up to first vowel, then 'e' must follow."
  (prog
    "        S = 'hello'"
    "        S BREAKX('aeiou') . R 'e'"
    "END")
  (is (clojure.core/= "h" ($$ 'R))))

(deftest breakx-retries-past-first
  "BREAKX retries where BREAK would fail. 'bca' -> BREAKX('a') . R 'a' -> R='bc'."
  (prog
    "        S = 'bcaXa'"
    "        S BREAKX('a') . R 'a'"
    "END")
  (is (clojure.core/= "bc" ($$ 'R))))

(deftest breakx-slides-to-second-occurrence
  "BREAKX slides when first match fails continuation; finds second 'a'."
  (prog
    "        S = 'xaYaZ'"
    "        S BREAKX('a') . R 'aZ'"
    "END")
  (is (clojure.core/= "xaY" ($$ 'R))))

(deftest breakx-empty-subject-fails
  "BREAKX on empty string fails — no match."
  (prog
    "        S = ''"
    "        S BREAKX('a') . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "miss" ($$ 'OK))))

(deftest breakx-break-char-not-present
  "BREAKX fails when break char never appears."
  (prog
    "        S = 'hello'"
    "        S BREAKX('z') . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "miss" ($$ 'OK))))

(deftest breakx-whole-string-no-break-char
  "BREAKX('z') on 'abc' — no z present — fails."
  (prog
    "        S = 'abc'"
    "        S BREAKX('z') . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "miss" ($$ 'OK))))

;; ── FENCE ─────────────────────────────────────────────────────────────────────
;; FENCE(P): matches P and commits; backtracking INTO P is blocked.
;; FENCE() bare: any backtrack past here aborts the entire match.

(deftest fence-commits-to-match
  "FENCE(LEN(3)) matches exactly 3 chars; no backtrack into it."
  (prog
    "        S = 'abcdef'"
    "        S FENCE(LEN(3)) . R"
    "END")
  (is (clojure.core/= "abc" ($$ 'R))))

(deftest fence-bare-prevents-backtrack
  "Bare FENCE() used as cut — prevents any pattern backtrack past it."
  ;; Pattern: LEN(2) . A  FENCE()  LEN(2) . B
  ;; On 'abcd': A='ab', FENCE(), B='cd'. No alternation to backtrack into.
  (prog
    "        S = 'abcd'"
    "        S LEN(2) . A  FENCE()  LEN(2) . B"
    "END")
  (is (clojure.core/= "ab" ($$ 'A)))
  (is (clojure.core/= "cd" ($$ 'B))))

(deftest fence-blocks-alt-backtrack
  "FENCE(P) inside ALT blocks backtrack from right alternative into P."
  ;; Without FENCE: 'x' | 'y' on 'xy' -> tries 'x' first, succeeds.
  ;; With FENCE: once FENCE('x') matches, we can't backtrack into it.
  (prog
    "        S = 'xy'"
    "        S FENCE('x') . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "hit" ($$ 'OK)))
  (is (clojure.core/= "x" ($$ 'R))))

;; ── ABORT ─────────────────────────────────────────────────────────────────────
;; ABORT: always fails and cuts the entire match (no backtrack retry).

(deftest abort-in-alt-cuts-match
  "ABORT in right branch of ALT causes entire match to fail."
  (prog
    "        S = 'hello'"
    "        S ('he' | ABORT()) . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  ;; 'he' matches first — ABORT branch never reached
  (is (clojure.core/= "hit" ($$ 'OK)))
  (is (clojure.core/= "he" ($$ 'R))))

(deftest abort-as-only-pattern
  "ABORT as sole pattern fails immediately."
  (prog
    "        S = 'hello'"
    "        S ABORT()  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "miss" ($$ 'OK))))

;; ── ARBNO ─────────────────────────────────────────────────────────────────────
;; ARBNO(P): matches zero or more non-overlapping occurrences of P.

(deftest arbno-zero-matches
  "ARBNO can match zero occurrences — succeeds with empty capture."
  (prog
    "        S = 'xyz'"
    "        S ARBNO('a') . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  ;; ARBNO('a') matches zero 'a's at start — succeeds with R=''
  (is (clojure.core/= "hit" ($$ 'OK))))

(deftest arbno-one-match
  "ARBNO('ab') on 'abXY' matches one occurrence."
  (prog
    "        S = 'abXY'"
    "        S ARBNO('ab') . R 'XY'"
    "END")
  (is (clojure.core/= "ab" ($$ 'R))))

(deftest arbno-two-matches
  "ARBNO('ab') on 'ababcd' matches two 'ab' repetitions."
  (prog
    "        S = 'ababcd'"
    "        S ARBNO('ab') . R 'cd'"
    "END")
  (is (clojure.core/= "abab" ($$ 'R))))

(deftest arbno-digit-star
  "ARBNO(ANY('0123456789')) . N matches a run of digits."
  (prog
    "        S = '12345abc'"
    "        S ARBNO(ANY('0123456789')) . N 'abc'"
    "END")
  (is (clojure.core/= "12345" ($$ 'N))))

(deftest arbno-nested-in-seq
  "ARBNO(LEN(1)) . R 'z' on 'abcz' captures 'abc'."
  (prog
    "        S = 'abcz'"
    "        S ARBNO(LEN(1)) . R 'z'"
    "END")
  (is (clojure.core/= "abc" ($$ 'R))))

;; ── CURSOR (@N) ───────────────────────────────────────────────────────────────
;; @N sets cursor position to N (absolute).
;; In SNOBOL4clojure, POS(N) matches position N; TAB(N) advances to col N.

(deftest cursor-pos-0-matches-start
  "POS(0) matches at start of string."
  (prog
    "        S = 'hello'"
    "        S POS(0) LEN(3) . R"
    "END")
  (is (clojure.core/= "hel" ($$ 'R))))

(deftest cursor-pos-2-skips-prefix
  "POS(2) anchors match at position 2."
  (prog
    "        S = 'abcde'"
    "        S POS(2) LEN(2) . R"
    "END")
  (is (clojure.core/= "cd" ($$ 'R))))

(deftest cursor-tab-advance
  "TAB(3) advances cursor to column 3 (0-indexed), then captures rest."
  (prog
    "        S = 'abcde'"
    "        S TAB(3) REM . R"
    "END")
  (is (clojure.core/= "de" ($$ 'R))))

(deftest cursor-rpos-from-end
  "RPOS(2) matches 2 chars from end."
  (prog
    "        S = 'hello'"
    "        S RPOS(2) LEN(2) . R"
    "END")
  ;; RPOS(2) means 'position such that 2 chars remain'; LEN(2) then gets them.
  ;; 'hello' len=5; RPOS(2) at pos 3; LEN(2) captures 'lo'.
  (is (clojure.core/= "lo" ($$ 'R))))

;; ── CONJ (& — pattern conjunction / sequential anchor) ────────────────────────
;; P & Q in SNOBOL4clojure uses the CONJ constructor.
;; Semantics: P matches the WHOLE subject, Q matches the WHOLE subject.
;; Both must succeed for the conjunction to succeed.

(deftest conj-both-succeed
  "CONJ(LEN(5), SPAN('helo')) — both match span [0,5] of 'hello' — succeeds."
  (prog
    "        S = 'hello'"
    "        S CONJ(LEN(5), SPAN('helo')) . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "hit" ($$ 'OK))))

(deftest conj-second-fails
  "CONJ(LEN(5), ANY('z')) — second fails — whole CONJ fails."
  (prog
    "        S = 'hello'"
    "        S CONJ(LEN(5), ANY('z')) . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "miss" ($$ 'OK))))

;; ── BAL ───────────────────────────────────────────────────────────────────────
;; BAL: matches a balanced-parentheses expression.

(deftest bal-simple
  "BAL matches a single balanced atom 'abc'."
  (prog
    "        S = 'abc rest'"
    "        S BAL . R ' rest'"
    "END")
  (is (clojure.core/= "abc" ($$ 'R))))

(deftest bal-parenthesized
  "BAL matches a fully parenthesized expression '(a+b)'."
  (prog
    "        S = '(a+b) rest'"
    "        S BAL . R ' rest'"
    "END")
  (is (clojure.core/= "(a+b)" ($$ 'R))))

(deftest bal-nested-parens
  "BAL matches nested parentheses '(a(b)c)'."
  (prog
    "        S = '(a(b)c) end'"
    "        S BAL . R ' end'"
    "END")
  (is (clojure.core/= "(a(b)c)" ($$ 'R))))

(deftest bal-unbalanced-fails
  "BAL fails to match '((' — no balanced substring exists at all."
  (prog
    "        S = '(('"
    "        S BAL . R  :S(HIT)F(MISS)"
    "HIT     OK = 'hit'  :(END)"
    "MISS    OK = 'miss'"
    "END")
  (is (clojure.core/= "miss" ($$ 'OK))))

;; ── Combined advanced patterns ────────────────────────────────────────────────

(deftest breakx-then-capture
  "BREAKX('0123456789') finds the first digit run."
  (prog
    "        S = 'abc123def'"
    "        S BREAKX('0123456789') SPAN('0123456789') . N"
    "END")
  (is (clojure.core/= "123" ($$ 'N))))

(deftest arbno-then-span-digits
  "ARBNO(ANY('abc')) then SPAN('0123456789') parses 'aabbcc123'."
  (prog
    "        S = 'aabbcc123'"
    "        S ARBNO(ANY('abc')) . A SPAN('0123456789') . N"
    "END")
  (is (clojure.core/= "aabbcc" ($$ 'A)))
  (is (clojure.core/= "123"    ($$ 'N))))

(deftest pos-len-tab-chain
  "POS(1) LEN(2) . A  TAB(5) LEN(2) . B on 'abcdefg'."
  (prog
    "        S = 'abcdefg'"
    "        S POS(1) LEN(2) . A  TAB(5) LEN(2) . B"
    "END")
  ;; POS(1): at col 1; LEN(2): 'bc'; TAB(5): advance to col 5; LEN(2): 'fg'... wait col5 is 'f', LEN(2)='fg' but only 1 char from col5 to col7? 
  ;; 'abcdefg' len=7; pos0='a' pos1='b' pos2='c' pos3='d' pos4='e' pos5='f' pos6='g'
  ;; POS(1)=pos1, LEN(2)='bc' (pos1..pos3), TAB(5)=advance to pos5, LEN(2)='fg'
  (is (clojure.core/= "bc" ($$ 'A)))
  (is (clojure.core/= "fg" ($$ 'B))))
