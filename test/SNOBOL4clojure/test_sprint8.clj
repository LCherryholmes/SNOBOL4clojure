(ns SNOBOL4clojure.test-sprint8
  "Sprint 8 — ABORT, bare FENCE(), REM, ASCII, CHAR, REMDR, INTEGER, REAL, STRING.
   Test cases derived from Snobol4.Net reference suite (jcooper0/Snobol4.Net)."
  (:require [clojure.test             :refer :all]
            [SNOBOL4clojure.match-api :refer [SEARCH FULLMATCH MATCH]]
            [SNOBOL4clojure.patterns  :refer [SPAN BREAK FENCE LEN ARB ARBNO
                                              ABORT FAIL REM POS RPOS]]
            [SNOBOL4clojure.functions :refer [ASCII CHAR REMDR INTEGER REAL STRING]]
            [SNOBOL4clojure.invoke :refer [INVOKE]]
            [SNOBOL4clojure.operators :refer [|]]
            [SNOBOL4clojure.env       :refer [GLOBALS ε]]))

(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint8)) (f)))

;; Helper: extract matched substring from a SEARCH result
(defn- matched [s result]
  (when result (subs s (first result) (second result))))

;; Helper: build a SEQ pattern node
(defn- SEQ [& parts] (apply list 'SEQ parts))

;; ─── 8.1  ABORT ──────────────────────────────────────────────────────────────
;; Ref: Snobol4.Net Pattern/Abort.cs TEST_Abort_001 / _002
;;
;; TEST_001: subject '-ab-1-'; pattern ANY('ab') | '1' ABORT
;;   Unanchored scan: position 1 hits 'a' via ANY → success before ABORT branch
;;
;; TEST_002: subject '-1a-b-'; same pattern
;;   Anchored at 0: '-' is not ANY('ab') and '-1' is not '1' at pos 0 exactly.
;;   We use FULLMATCH with a pattern that forces the abort branch to fire.

(deftest abort-001-any-matches-before-abort
  (testing "ANY('ab') succeeds; ABORT branch never reached"
    (let [pat (| (INVOKE 'ANY "ab") (SEQ "1" ABORT))]
      (is (some? (SEARCH "-ab-1-" pat))))))

(deftest abort-002-abort-fires-when-its-branch-is-taken
  (testing "Pattern that forces ABORT branch: entire match returns nil"
    ;; FULLMATCH so there is only one attempt.
    ;; '1' matches at pos 0, then ABORT fires.
    (let [pat (SEQ "1" ABORT)]
      (is (nil? (FULLMATCH "1xyz" pat))))))

(deftest abort-terminates-regardless-of-remaining-alternatives
  (testing "ABORT inside SEQ kills match even when ALT has more alternatives"
    ;; pattern: ALT[ SEQ['a' ABORT], 'ax' ]
    ;; 'a' matches, ABORT fires — second alternative never tried
    (is (nil? (FULLMATCH "ax" (| (SEQ "a" ABORT) "ax"))))))

;; ─── 8.2  REM ────────────────────────────────────────────────────────────────
;; Ref: Snobol4.Net Pattern/Rem.cs TEST_Rem_001 / _002 / _003

(deftest rem-matches-rest-of-subject
  (testing "REM matches everything after preceding pattern — TEST_Rem_001"
    (let [s "programmer"
          r (SEARCH s (SEQ "gra" REM))]
      ;; full match is "grammer"; everything from 'gra' to end
      (is (some? r))
      (is (= "grammer" (subs s (first r) (second r))))
      ;; and the REM portion alone is "mmer"
      (is (= "mmer" (subs s (+ (first r) 3) (second r)))))))

(deftest rem-simple-capture
  (testing "REM captures to end of string"
    ;; The match starts at 'WIN', REM gets everything after
    (let [s   "THE WINTER WINDS"
          pat (SEQ "WIN" REM)
          r   (SEARCH s pat)]
      (is (some? r))
      ;; full match from first position of 'WIN' to end
      (is (= "TER WINDS" (subs s (+ (first r) 3) (second r)))))))

(deftest rem-at-end-matches-empty
  (testing "REM at end of string matches empty string — TEST_Rem_003"
    (let [s   "THE WINTER WINDS"
          pat (SEQ "WINDS" REM)
          r   (SEARCH s pat)]
      (is (some? r))
      ;; end of WINDS is end of string, REM consumes nothing extra
      (is (= (count s) (second r))))))

;; ─── 8.3  Bare FENCE() and FENCE(P) ─────────────────────────────────────────
;; Ref: Snobol4.Net Pattern/Fence.cs

(deftest fence-p-succeeds-when-child-matches
  (testing "FENCE(P) succeeds when child P matches"
    (is (some? (SEARCH "hello" (FENCE (SPAN "hel")))))))

(deftest fence-p-first-token
  (testing "FENCE(P) commits to first match, prevents retry — Fence.cs TEST_Abort_001"
    ;; 'ABC'     — REM matches whole string, result = 'ABC'
    ;; '123,,456' — BREAK(',') matches '123', FENCE prevents retry
    (let [tok (FENCE (| (BREAK ",") REM))]
      (is (= "ABC"  (let [r (SEARCH "ABC"      tok)] (when r (subs "ABC"      (first r) (second r))))))
      (is (= "123"  (let [r (SEARCH "123,,456" tok)] (when r (subs "123,,456" (first r) (second r)))))))))

(deftest fence-bare-aborts-match
  (testing "Bare FENCE() aborts entire match when backtrack crosses it — Fence.cs TEST_Abort_003/005"
    ;; ALT[ SEQ['a', FENCE(), 'z'],  SEQ['a', 'x'] ]
    ;; 'a' matches, FENCE() committed, 'z' fails → backtrack hits bare FENCE → abort
    ;; Second alternative never reached
    (is (nil? (FULLMATCH "ax"
                (| (SEQ "a" (FENCE) "z")
                   (SEQ "a" "x")))))))

;; ─── 8.4  ASCII / CHAR ────────────────────────────────────────────────────────

(deftest ascii-returns-code
  (testing "ASCII returns integer code of first character"
    (is (= 65 (ASCII "A")))
    (is (= 97 (ASCII "a")))
    (is (= 48 (ASCII "0")))
    (is (= 32 (ASCII " ")))))

(deftest char-returns-string
  (testing "CHAR converts integer to single-character string"
    (is (= "A" (CHAR 65)))
    (is (= "a" (CHAR 97)))
    (is (= " " (CHAR 32)))))

(deftest ascii-char-roundtrip
  (testing "ASCII and CHAR are mutual inverses for printable ASCII"
    (doseq [c "ABCDEFabcdef0123456789"]
      (is (= (str c) (CHAR (ASCII (str c))))))))

;; ─── 8.5  REMDR ───────────────────────────────────────────────────────────────

(deftest remdr-basic
  (testing "REMDR returns integer remainder"
    (is (= 1 (REMDR 10 3)))
    (is (= 0 (REMDR  9 3)))
    (is (= 2 (REMDR 17 5)))
    (is (= 0 (REMDR 12 4)))))

;; ─── 8.6  INTEGER ─────────────────────────────────────────────────────────────

(deftest integer-from-string
  (testing "INTEGER parses numeric strings"
    (is (= 42 (INTEGER "42")))
    (is (= 0  (INTEGER "0")))
    (is (= -7 (INTEGER "-7")))))

(deftest integer-from-number
  (testing "INTEGER truncates reals"
    (is (= 42 (INTEGER 42)))
    (is (= 42 (INTEGER 42.9)))
    (is (= -3 (INTEGER -3.1)))))

(deftest integer-fail
  (testing "INTEGER returns ε on non-numeric input"
    (is (= ε (INTEGER "hello")))
    (is (= ε (INTEGER "")))))

;; ─── 8.7  REAL ────────────────────────────────────────────────────────────────

(deftest real-from-string
  (testing "REAL parses numeric strings to double"
    (is (= 3.14 (REAL "3.14")))
    (is (= 0.5  (REAL "0.5")))
    (is (= 1.0  (REAL "1")))))

(deftest real-from-number
  (testing "REAL promotes integers"
    (is (= 42.0 (REAL 42)))))

(deftest real-fail
  (testing "REAL returns ε on non-numeric input"
    (is (= ε (REAL "abc")))
    (is (= ε (REAL "")))))

;; ─── 8.8  STRING ──────────────────────────────────────────────────────────────

(deftest string-conversion
  (testing "STRING converts any value to string"
    (is (= "42"    (STRING 42)))
    (is (= "3.14"  (STRING 3.14)))
    (is (= "hello" (STRING "hello")))
    (is (= ""      (STRING "")))))
