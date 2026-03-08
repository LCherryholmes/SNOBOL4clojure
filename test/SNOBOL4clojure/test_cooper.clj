(ns SNOBOL4clojure.test-cooper
  "Tests derived from Jeffrey Cooper's Snobol4.Net test suite.
   Each deftest corresponds to one or more Cooper TestMethod cases.
   Source: Snobol4.Net-feature-msil-trace/TestSnobol4/Function/

   Translation notes:
   - Cooper uses case-folded identifiers (lowercase); we use uppercase (our design rule).
   - Cooper uses &anchor = 0 (unanchored); our SEARCH is unanchored by default — no-op.
   - Cooper checks variable state after run; we use ($$ 'VAR) for the same.
   - Tests that require File I/O, OPSYN, LOAD, or DUPL are skipped with a comment.
   - Error-expecting tests (Assert.AreNotEqual 0 ErrorCodeHistory) are marked
     with (is (thrown? ...)) or skipped if the error path is not yet implemented."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]))

;; ── Helper ────────────────────────────────────────────────────────────────────

(defmacro prog [& lines]
  `(RUN (CODE ~(clojure.string/join "\n" (map str lines)))))

;; ── Pattern/Len ───────────────────────────────────────────────────────────────

(deftest cooper-len-001
  "LEN(3) captures first 3 chars from 'ABCDA' (unanchored)"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = LEN(3) . TEST"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "ABC"     ($$ 'TEST))))

(deftest cooper-len-002
  "LEN(2) then 'A' — skips to CDA, captures CD"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = LEN(2) . TEST 'A'"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "CD"      ($$ 'TEST))))

(deftest cooper-len-003
  "LEN(0) after 'ABCD' captures empty string before 'A'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = 'ABCD' LEN(0) . TEST 'A'"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ""        ($$ 'TEST))))

(deftest cooper-len-004
  "'A' then LEN(0) then 'A' — no two consecutive 'A' so fail"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = 'A' LEN(0) . TEST 'A'"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-len-009
  "LEN(*B) deferred — B=2, captures CD before A"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = LEN(*B) . TEST 'A'"
    "        B = 2"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "CD"      ($$ 'TEST))))

;; ── Pattern/Any ───────────────────────────────────────────────────────────────

(deftest cooper-any-001
  "ANY('aeiou') finds first vowel 'a' in 'vacuum'"
  (prog
    "        &ANCHOR = 0"
    "        VOWEL = ANY('aeiou')"
    "        SUBJECT = 'vacuum'"
    "        SUBJECT VOWEL . V1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "a"       ($$ 'V1))))

(deftest cooper-any-002
  "Double vowel ANY: 'vacuum' has 'uu' pair"
  (prog
    "        &ANCHOR = 0"
    "        DVOWEL = ANY('aeiou') ANY('aeiou')"
    "        SUBJECT = 'vacuum'"
    "        SUBJECT DVOWEL . V1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "uu"      ($$ 'V1))))

(deftest cooper-any-005
  "ANY on empty subject fails"
  (prog
    "        &ANCHOR = 0"
    "        VOWEL = ANY('aeiou')"
    "        SUBJECT = ''"
    "        SUBJECT VOWEL . V1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-any-006
  "ANY(*B) deferred — B changes, pattern re-evaluates"
  (prog
    "        A = ANY(*B)"
    "        B = '123'"
    "        'ABCD3FG' A . R1"
    "        B = 'ABC'"
    "        '1234C56' A . R2"
    "END")
  (is (= "3" ($$ 'R1)))
  (is (= "C" ($$ 'R2))))

(deftest cooper-any-009
  "ANY on empty subject with no match goes to :F(N)"
  (prog
    "        A = ANY('123456')"
    "        '' A . R1  :S(END)F(N)"
    "N       R1 = 'FAIL'"
    "END")
  (is (= "FAIL" ($$ 'R1))))

;; ── Pattern/NotAny ────────────────────────────────────────────────────────────

(deftest cooper-notany-001
  "NOTANY('aeiou') matches first non-vowel 'v' in 'vacuum'"
  (prog
    "        &ANCHOR = 0"
    "        NOTVOWEL = NOTANY('aeiou')"
    "        SUBJECT = 'vacuum'"
    "        SUBJECT NOTVOWEL . V1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "v"       ($$ 'V1))))

(deftest cooper-notany-002
  "ANY vowel then NOTANY vowel — 'ac' from 'vacuum'"
  (prog
    "        &ANCHOR = 0"
    "        DVOWEL = ANY('aeiou') NOTANY('aeiou')"
    "        SUBJECT = 'vacuum'"
    "        SUBJECT DVOWEL . V1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "ac"      ($$ 'V1))))

(deftest cooper-notany-005
  "NOTANY on empty subject fails"
  (prog
    "        &ANCHOR = 0"
    "        VOWEL = NOTANY('aeiou')"
    "        SUBJECT = ''"
    "        SUBJECT VOWEL . V1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-notany-006
  "NOTANY(*B) deferred — picks first non-matching char"
  (prog
    "        A = NOTANY(*B)"
    "        B = '123'"
    "        '123ABC' A . R1"
    "        B = 'ABC'"
    "        'ABC123' A . R2"
    "END")
  (is (= "A" ($$ 'R1)))
  (is (= "1" ($$ 'R2))))

(deftest cooper-notany-009
  "NOTANY on empty subject goes to :F(N)"
  (prog
    "        A = NOTANY('123456')"
    "        '' A . R1  :S(END)F(N)"
    "N       R1 = 'FAIL'"
    "END")
  (is (= "FAIL" ($$ 'R1))))

;; ── Pattern/Span ──────────────────────────────────────────────────────────────

(deftest cooper-span-001
  "SPAN(letters) finds 'one' after ':= '"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        WORD = SPAN(LETTERS)"
    "        SUBJECT = ':= one,,, two,..  three'"
    "        SUBJECT WORD . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "one"     ($$ 'TEMP1))))

(deftest cooper-span-002
  "SPAN(letters) finds 'one' at start of 'one,,, two..'"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        WORD = SPAN(LETTERS)"
    "        SUBJECT = 'one,,, two,..  three'"
    "        SUBJECT WORD . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "one"     ($$ 'TEMP1))))

(deftest cooper-span-003
  "SPAN(letters) fails on all-punctuation subject"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        WORD = SPAN(LETTERS)"
    "        SUBJECT = '86%^'"
    "        SUBJECT WORD . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-span-005
  "SPAN replaces matched portion — 'c' -> '****'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'c'"
    "        PATTERN = SPAN('c')"
    "        SUBJECT PATTERN = '****'   :F(N)"
    "        TEMP1 = '[' SUBJECT ']'"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "[****]"  ($$ 'TEMP1))))

(deftest cooper-span-007
  "SPAN whole string — 'one' matches all of 'one'"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        WORD = SPAN(LETTERS)"
    "        SUBJECT = 'one'"
    "        SUBJECT WORD . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "one"     ($$ 'TEMP1))))

(deftest cooper-span-008
  "SPAN extracts first word from two different subjects"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        WORD = SPAN(LETTERS)"
    "        GAP = BREAK(LETTERS)"
    "        SUBJECT = 'sample line'"
    "        SUBJECT WORD . WORD1  :F(N)"
    "        SUBJECT = 'plus ten degrees'"
    "        SUBJECT WORD . WORD2  :F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "sample"  ($$ 'WORD1)))
  (is (= "plus"    ($$ 'WORD2))))

(deftest cooper-span-009
  "Integer pattern (sign + digits) — captures '-43' twice"
  (prog
    "        &ANCHOR = 0"
    "        DIGITS = '0123456789'"
    "        INTEGER = (ANY('+-') | '') SPAN(DIGITS)"
    "        SUBJECT = 'set -43 volts'"
    "        SUBJECT INTEGER . INTEGER1  :F(N)"
    "        SUBJECT = 'set -43.625 volts'"
    "        SUBJECT INTEGER . INTEGER2  :F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "-43"     ($$ 'INTEGER1)))
  (is (= "-43"     ($$ 'INTEGER2))))

(deftest cooper-span-010
  "SPAN(*B) deferred — B changes between matches"
  (prog
    "        A = SPAN(*B)"
    "        B = '123'"
    "        '333' A . R1"
    "        B = 'ABC'"
    "        'CCC' A . R2"
    "END")
  (is (= "333" ($$ 'R1)))
  (is (= "CCC" ($$ 'R2))))

(deftest cooper-span-012
  "SPAN('123456') fails on 'ABCDEF' — no match -> :S(END) skipped"
  (prog
    "        A = SPAN('123456')"
    "        'ABCDEF' A . R1 :S(END)"
    "        R1 = 'fail'"
    "END")
  (is (= "fail" ($$ 'R1))))

;; ── Pattern/Break ─────────────────────────────────────────────────────────────

(deftest cooper-break-001
  "BREAK(letters) captures ':= ' before first letter"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = BREAK(LETTERS)"
    "        SUBJECT = ':= one,,, two,..  three'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ":= "     ($$ 'TEMP1))))

(deftest cooper-break-002
  "NOTANY then BREAK — captures punctuation gap ',,, '"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = NOTANY(LETTERS) BREAK(LETTERS)"
    "        SUBJECT = 'one,,, two,..  three'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ",,, "    ($$ 'TEMP1))))

(deftest cooper-break-003
  "BREAK(letters) on string starting with letter: captures ''"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = BREAK(LETTERS)"
    "        SUBJECT = 'one,,, two,..  three'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ""        ($$ 'TEMP1))))

(deftest cooper-break-005
  "BREAK replaces matched portion — '' before 'c' becomes '****'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'c'"
    "        PATTERN = BREAK('c')"
    "        SUBJECT PATTERN = '****'   :F(N)"
    "        TEMP1 = '[' SUBJECT ']'"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "[****c]"  ($$ 'TEMP1))))

(deftest cooper-break-006
  "BREAK(letters) fails on all-punct subject (no letters present)"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = BREAK(LETTERS)"
    "        SUBJECT = '1234567890'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-break-007
  "BREAK('3') captures '12' from '12345'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = '12345'"
    "        CHAR = '3'"
    "        GAP = BREAK(CHAR)"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "12"      ($$ 'TEMP1))))

(deftest cooper-break-009
  "BREAK('8') fails — '8' not in '12345'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = '12345'"
    "        CHAR = '8'"
    "        GAP = BREAK(CHAR)"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-break-010
  "BREAK(*B) deferred — B changes between matches"
  (prog
    "        A = BREAK(*B)"
    "        B = '123'"
    "        'ABCD3FG' A . R1"
    "        B = 'ABC'"
    "        '1234C56' A . R2"
    "END")
  (is (= "ABCD" ($$ 'R1)))
  (is (= "1234" ($$ 'R2))))

(deftest cooper-break-012
  "BREAK on empty subject — captures '' (succeeds with empty)"
  (prog
    "        A = BREAK('123456')"
    "        '' A . R1"
    "END")
  (is (= "" ($$ 'R1))))

;; ── Pattern/BreakX ────────────────────────────────────────────────────────────

(deftest cooper-breakx-001
  "BREAKX(letters) captures ':= ' before first letter"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = BREAKX(LETTERS)"
    "        SUBJECT = ':= one,,, two,..  three'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ":= "     ($$ 'TEMP1))))

(deftest cooper-breakx-002
  "NOTANY+BREAKX captures ',,, ' gap"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = NOTANY(LETTERS) BREAKX(LETTERS)"
    "        SUBJECT = 'one,,, two,..  three'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ",,, "    ($$ 'TEMP1))))

(deftest cooper-breakx-003
  "BREAKX on string starting with letter: captures ''"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = BREAKX(LETTERS)"
    "        SUBJECT = 'one,,, two,..  three'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ""        ($$ 'TEMP1))))

(deftest cooper-breakx-006
  "BREAKX fails when no char in set exists in subject"
  (prog
    "        &ANCHOR = 0"
    "        LETTERS = 'abcdefghijklmnopqrstuvwxyz'"
    "        GAP = BREAKX(LETTERS)"
    "        SUBJECT = '1234567890'"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-breakx-007
  "BREAKX('3') captures '12' from '12345'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = '12345'"
    "        CHAR = '3'"
    "        GAP = BREAKX(CHAR)"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "12"      ($$ 'TEMP1))))

(deftest cooper-breakx-009
  "BREAKX('8') fails — '8' not in '12345'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = '12345'"
    "        CHAR = '8'"
    "        GAP = BREAKX(CHAR)"
    "        SUBJECT GAP . TEMP1  :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-breakx-010
  "BREAKX(*B) deferred — B changes between matches"
  (prog
    "        A = BREAKX(*B)"
    "        B = '123'"
    "        'ABCD3FG' A . R1"
    "        B = 'ABC'"
    "        '1234C56' A . R2"
    "END")
  (is (= "ABCD" ($$ 'R1)))
  (is (= "1234" ($$ 'R2))))

(deftest cooper-breakx-012
  "BREAKX on empty subject succeeds with ''"
  (prog
    "        A = BREAKX('123456')"
    "        '' A . R1"
    "END")
  (is (= "" ($$ 'R1))))

(deftest cooper-breakx-014
  "Canonical BreakX_014: POS(0) BREAKX('A') . R2 'AS' on EXCEPTIONS string"
  (prog
    "        SUB = 'EXCEPTIONS-ARE-AS-TRUE-AS-RULES'"
    "        P1 = POS(0) BREAKX('A') . R2 'AS'"
    "        SUB P1    :S(Y)F(N)"
    "Y       R1 = 'SUCCESS' :(END)"
    "N       R1 = 'FAILURE'"
    "END")
  (is (= "SUCCESS"          ($$ 'R1)))
  (is (= "EXCEPTIONS-ARE-"  ($$ 'R2))))

;; ── Pattern/Pos ───────────────────────────────────────────────────────────────

(deftest cooper-pos-001
  "POS(0) then 'b' — 'b' not at pos 0 in 'ABCDA', fails"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = POS(0) . TEST 'b'"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-pos-002
  "LEN(3) then POS(3) — captures first 3 chars"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = LEN(3) . TEST POS(3)"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "ABC"     ($$ 'TEST))))

(deftest cooper-pos-003
  "POS(3) LEN(1) — char at pos 3 is 'D'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = POS(3) LEN(1) . TEST"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "D"       ($$ 'TEST))))

(deftest cooper-pos-004
  "POS(0) 'ABCD' RPOS(0) — 'ABCDA' is 5 chars so fails"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = POS(0) 'ABCD' RPOS(0)"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-pos-009
  "POS(*A) LEN(*B) deferred — A=3 B=1, char at pos 3 is 'D'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = POS(*A) LEN(*B) . TEST"
    "        A = 3"
    "        B = 1"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "D"       ($$ 'TEST))))

;; ── Pattern/RPos ──────────────────────────────────────────────────────────────

(deftest cooper-rpos-001
  "'b' RPOS(0) — 'b' not in 'ABCDA' at all, fails"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = 'b' RPOS(0) . TEST"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

(deftest cooper-rpos-002
  "LEN(3) RPOS(2) — captures first 3 chars, then asserts pos == len-2=3"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = LEN(3) . TEST RPOS(2)"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "ABC"     ($$ 'TEST))))

(deftest cooper-rpos-009
  "LEN(*B) RPOS(*C) deferred — B=3 C=2, captures 'ABC'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = LEN(*B) . TEST RPOS(*C)"
    "        B = 3"
    "        C = 2"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "ABC"     ($$ 'TEST))))

;; ── Pattern/Tab ───────────────────────────────────────────────────────────────

(deftest cooper-tab-001
  "TAB(2) captures first 2 chars 'ab' from 'abcde'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'abcde'"
    "        PATTERN = TAB(2) . TAB1"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "ab"      ($$ 'TAB1))))

(deftest cooper-tab-003
  "TAB(3) LEN(1) — char at col 3 is 'D'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = TAB(3) LEN(1) . TEST"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "D"       ($$ 'TEST))))

;; ── Pattern/RTab ──────────────────────────────────────────────────────────────

(deftest cooper-rtab-001
  "RTAB(2) captures everything except last 2 chars"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'abcde'"
    "        PATTERN = RTAB(2) . TAB1"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "abc"     ($$ 'TAB1))))

(deftest cooper-rtab-003
  "RTAB(3) LEN(1) — char at pos len-3 = 2 is 'C'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'ABCDA'"
    "        PATTERN = RTAB(3) LEN(1) . TEST"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "C"       ($$ 'TEST))))

;; ── Pattern/Arb ───────────────────────────────────────────────────────────────

(deftest cooper-arb-001
  "ARB finds 'rogramm' between 'p' and 'er'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'programmer'"
    "        PATTERN = 'p' ARB . TEST 'er'"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success"  ($$ 'RESULT)))
  (is (= "rogramm"  ($$ 'TEST))))

(deftest cooper-arb-002
  "ARB finds 'UNT' between 'O' and 'A' in MOUNTAIN"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'MOUNTAIN'"
    "        PATTERN = 'O' ARB . TEST 'A'"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "UNT"     ($$ 'TEST))))

(deftest cooper-arb-003
  "ARB finds '' between 'O' and 'U' (shortest)"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'MOUNTAIN'"
    "        PATTERN = 'O' ARB . TEST 'U'"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= ""        ($$ 'TEST))))

(deftest cooper-arb-004
  "CAT|DOG alternation with ARB"
  (prog
    "        CATANDDOG = 'CAT' ARB 'DOG' | 'DOG' ARB 'CAT'"
    "        'CATALOG FOR SEADOGS' CATANDDOG $ R1"
    "        'DOGS HATE POLECATS' CATANDDOG $ R2"
    "        'CATDOG' CATANDDOG $ R3"
    "END")
  (is (= "CATALOG FOR SEADOG" ($$ 'R1)))
  (is (= "DOGS HATE POLECAT"  ($$ 'R2)))
  (is (= "CATDOG"             ($$ 'R3))))

(deftest cooper-arb-005
  "ARB immediate-assign $ inside parens"
  (prog
    "        'MOUNTAIN' 'O' (ARB $ R1) 'A'"
    "        'MOUNTAIN' 'O' (ARB $ R2) 'U'"
    "END")
  (is (= "UNT" ($$ 'R1)))
  (is (= ""    ($$ 'R2))))

;; ── Pattern/ArbNo ─────────────────────────────────────────────────────────────

(deftest cooper-arbno-001
  "ARBNO matches '(12,34,56)' — success"
  (prog
    "        &ANCHOR = 0"
    "        ITEM = SPAN('0123456789')"
    "        PATTERN = POS(0) '(' ITEM ARBNO(',' ITEM) ')' RPOS(0)"
    "        SUBJECT = '(12,34,56)'"
    "        SUBJECT PATTERN :F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT))))

(deftest cooper-arbno-002
  "ARBNO rejects '(12,,56)' — consecutive commas"
  (prog
    "        &ANCHOR = 0"
    "        ITEM = SPAN('0123456789')"
    "        PATTERN = POS(0) '(' ITEM ARBNO(',' ITEM) ')' RPOS(0)"
    "        SUBJECT = '(12,,56)'"
    "        SUBJECT PATTERN :F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

;; ── Pattern/Rem ───────────────────────────────────────────────────────────────

(deftest cooper-rem-001
  "REM after 'gra' captures 'mmer'"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'programmer'"
    "        PATTERN = 'gra' REM . TEST"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT)))
  (is (= "mmer"    ($$ 'TEST))))

;; ── Pattern/Fail ──────────────────────────────────────────────────────────────

(deftest cooper-fail-001
  "FAIL always causes statement to fail"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = 'programmer'"
    "        PATTERN = FAIL"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

;; ── Pattern/Abort ─────────────────────────────────────────────────────────────

(deftest cooper-abort-001
  "ABORT cuts off match when '1' reached; first match with 'ab' succeeds"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = '-ab-1-'"
    "        PATTERN = ANY('ab') | '1' ABORT"
    "        SUBJECT PATTERN :F(N)"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "success" ($$ 'RESULT))))

(deftest cooper-abort-002
  "ABORT after '1' when subject starts with '1' — match aborted, fail"
  (prog
    "        &ANCHOR = 0"
    "        SUBJECT = '-1a-b-'"
    "        PATTERN = ANY('ab') | '1' ABORT"
    "        SUBJECT PATTERN :F(N)"
    "        SUBJECT PATTERN      :S(Y)F(N)"
    "Y       RESULT = 'success'   :(END)"
    "N       RESULT = 'fail'"
    "END")
  (is (= "fail" ($$ 'RESULT))))

;; ── Pattern/Fence ─────────────────────────────────────────────────────────────

(deftest cooper-fence-001
  "FENCE(BREAK(',') | REM) — commits on first non-empty"
  (prog
    "        P = FENCE(BREAK(',') | REM) $ STR *DIFFER(STR)"
    "        'ABC' P . R1"
    "        '123,,456' P . R2"
    "END")
  (is (= "ABC" ($$ 'R1)))
  (is (= "123" ($$ 'R2))))

(deftest cooper-fence-004
  "FENCE('B') — 'ABC' starts at A not B, fails"
  (prog
    "        'ABC'  FENCE('B') :S(Y)F(N)"
    "Y       R1 = 'SUCCESS' :(END)"
    "N       R1 = 'FAILURE'"
    "END")
  (is (= "FAILURE" ($$ 'R1))))

;; ── StringSynthesis/Reverse ───────────────────────────────────────────────────

(deftest cooper-reverse-001
  "REVERSE reverses 'this is a test'"
  (prog
    "        A = 'this is a test'"
    "        B = REVERSE(A)"
    "END")
  (is (= "tset a si siht" ($$ 'B))))

;; ── StringSynthesis/Trim ──────────────────────────────────────────────────────

(deftest cooper-trim-001
  "TRIM removes trailing spaces"
  (prog
    "        A = 'abc   '"
    "        B = TRIM(A)"
    "END")
  (is (= "abc" ($$ 'B))))

(deftest cooper-trim-002
  "TRIM on string with no trailing spaces"
  (prog
    "        A = 'abc'"
    "        B = TRIM(A)"
    "END")
  (is (= "abc" ($$ 'B))))

(deftest cooper-trim-003
  "TRIM only removes trailing spaces, not leading"
  (prog
    "        A = '   abc'"
    "        B = TRIM(A)"
    "END")
  (is (= "   abc" ($$ 'B))))

(deftest cooper-trim-004
  "TRIM removes trailing, keeps leading"
  (prog
    "        A = '   abc         '"
    "        B = TRIM(A)"
    "END")
  (is (= "   abc" ($$ 'B))))

(deftest cooper-trim-005
  "TRIM on integer coerces to string first"
  (prog
    "        A = 1234"
    "        B = TRIM(A)"
    "END")
  (is (= "1234" ($$ 'B))))

;; ── StringSynthesis/Lpad ──────────────────────────────────────────────────────

(deftest cooper-lpad-001
  "LPAD to width 20 with '-+' fill — uses first fill char '-'"
  (prog
    "        S = 'this is a test'"
    "        R = LPAD(S,20,'-+')"
    "END")
  (is (= "------this is a test" ($$ 'R))))

(deftest cooper-lpad-002
  "LPAD to width 20 with default space fill"
  (prog
    "        S = 'this is a test'"
    "        R = LPAD(S,20)"
    "END")
  (is (= "      this is a test" ($$ 'R))))

(deftest cooper-lpad-007
  "LPAD when width < length — returns original string"
  (prog
    "        S = 'this is a test'"
    "        R = LPAD(S,2)"
    "END")
  (is (= "this is a test" ($$ 'R))))

;; ── StringSynthesis/Rpad ──────────────────────────────────────────────────────

(deftest cooper-rpad-001
  "RPAD to width 20 with '-+' fill — uses first fill char '-'"
  (prog
    "        S = 'this is a test'"
    "        R = RPAD(S,20,'-+')"
    "END")
  (is (= "this is a test------" ($$ 'R))))

(deftest cooper-rpad-002
  "RPAD to width 20 with default space fill"
  (prog
    "        S = 'this is a test'"
    "        R = RPAD(S,20)"
    "END")
  (is (= "this is a test      " ($$ 'R))))

(deftest cooper-rpad-007
  "RPAD when width < length — returns original string"
  (prog
    "        S = 'this is a test'"
    "        R = RPAD(S,2)"
    "END")
  (is (= "this is a test" ($$ 'R))))

;; ── Gimpel/ROMAN ──────────────────────────────────────────────────────────────

(deftest gimpel-roman-basic
  "ROMAN function converts Arabic to Roman numerals (Cooper's ROMAN0 test)"
  (prog
    "        DEFINE('ROMAN(N)T')          :(ROMAN_END)"
    "ROMAN   N   RPOS(1)  LEN(1) . T  =  :F(RETURN)"
    "        '0,1I,2II,3III,4IV,5V,6VI,7VII,8VIII,9IX,'"
    "+       T   BREAK(',') . T          :F(FRETURN)"
    "        ROMAN = REPLACE(ROMAN(N), 'IVXLCDM', 'XLCDM**') T"
    "+                                   :S(RETURN)F(FRETURN)"
    "ROMAN_END"
    "        R1 = ROMAN('1776')"
    "        R2 = ROMAN('9')"
    "        R3 = ROMAN('45')"
    "        R4 = ROMAN('2026')"
    "END")
  (is (= "MDCCLXXVI" ($$ 'R1)))
  (is (= "IX"        ($$ 'R2)))
  (is (= "XLV"       ($$ 'R3)))
  (is (= "MMXXVI"    ($$ 'R4))))

;; ── Gimpel/REVERSE.INC ────────────────────────────────────────────────────────
;; (Note: REVERSE is a built-in in our impl — testing it via both built-in
;;  and verifying the algorithm would duplicate, so we just confirm built-in.)

(deftest gimpel-reverse-builtin
  "REVERSE('hello') = 'olleh' via built-in"
  (prog
    "        R = REVERSE('hello')"
    "END")
  (is (= "olleh" ($$ 'R))))

;; ── Gimpel/BSORT.INC ─────────────────────────────────────────────────────────

(deftest gimpel-bsort
  "Bubble sort: BSORT(A,1,5) sorts array in place"
  (prog
    "        DEFINE('BSORT(A,I,N)J,K,V')  :(BSORT_END)"
    "BSORT   J  =  I"
    "BSORT_1 J  =  J + 1  LT(J,N)         :F(RETURN)"
    "        K  =  J"
    "        V  =  A<J>"
    "BSORT_2 K  =  K - 1  GT(K,I)         :F(BSORT_RO)"
    "        A<K + 1>  =  LGT(A<K>,V)  A<K>  :S(BSORT_2)"
    "        A<K + 1>  =  V               :(BSORT_1)"
    "BSORT_RO A<I>  =  V                  :(BSORT_1)"
    "BSORT_END"
    "        ARR = ARRAY(5)"
    "        ARR<1> = 'pear'"
    "        ARR<2> = 'apple'"
    "        ARR<3> = 'mango'"
    "        ARR<4> = 'banana'"
    "        ARR<5> = 'cherry'"
    "        BSORT(.ARR,1,5)"
    "        R1 = ARR<1>"
    "        R2 = ARR<2>"
    "        R3 = ARR<3>"
    "        R4 = ARR<4>"
    "        R5 = ARR<5>"
    "END")
  (is (= "apple"  ($$ 'R1)))
  (is (= "banana" ($$ 'R2)))
  (is (= "cherry" ($$ 'R3)))
  (is (= "mango"  ($$ 'R4)))
  (is (= "pear"   ($$ 'R5))))
