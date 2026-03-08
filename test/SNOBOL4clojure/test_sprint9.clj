(ns SNOBOL4clojure.test-sprint9
  "Sprint 9 — BREAKX backtracking, NSPAN, BOL/EOL, $ vs . capture semantics.
   Test cases derived from Snobol4.Net reference suite (jcooper0/Snobol4.Net)."
  (:require [clojure.test             :refer :all]
            [SNOBOL4clojure.match-api :refer [SEARCH FULLMATCH MATCH]]
            [SNOBOL4clojure.patterns  :refer [SPAN NSPAN BREAK BREAKX NOTANY
                                              FENCE LEN ARB ARBNO POS RPOS
                                              ABORT FAIL REM BOL EOL]]
            [SNOBOL4clojure.invoke :refer [INVOKE]]
            [SNOBOL4clojure.operators :refer [| .]]
            [SNOBOL4clojure.env       :refer [GLOBALS ε snobol-set! $$]]))

(use-fixtures :each (fn [f] (GLOBALS (find-ns 'SNOBOL4clojure.test-sprint9)) (f)))

(defn- m [s pat] (when-let [[a b] (SEARCH s pat)] (subs s a b)))
(defn- SEQ [& ps] (apply list 'SEQ ps))

;; ─── 9.1  BREAKX backtracking ────────────────────────────────────────────────
;; Ref: Snobol4.Net Pattern/BreakX.cs
;;
;; The key difference from BREAK:
;;   BREAK stops at the FIRST occurrence of a break char and does NOT retry.
;;   BREAKX stops at each break char in turn, retrying until something succeeds.
;;
;; BreakX_014 (the canonical discriminator):
;;   SUB = 'EXCEPTIONS-ARE-AS-TRUE-AS-RULES'
;;   BREAKX: P1 = POS(0) BREAKX('A') . R2 'AS'  → SUCCESS, R2='EXCEPTIONS-ARE-'
;;   BREAK:  P1 = POS(0) BREAK('A')  . R2 'AS'  → FAILURE

(deftest breakx-001-basic-match
  (testing "BREAKX matches up to first break char — same as BREAK on first attempt"
    (is (= ":= " (m ":= one,,, two" (SEQ (BREAKX "abcdefghijklmnopqrstuvwxyz")))))))

(deftest breakx-014-canonical-discriminator
  (testing "BREAKX retries past each break char until continuation matches — BreakX_014"
    (let [sub "EXCEPTIONS-ARE-AS-TRUE-AS-RULES"
          captured (atom nil)
          pat (SEQ (POS 0) (list 'CAPTURE-COND (BREAKX "A") 'R2) "AS")]
      (let [r (SEARCH sub pat)]
        (is (some? r))
        ;; The match should start at pos 0 and BREAKX should slide to 'EXCEPTIONS-ARE-'
        ;; before finding 'AS' succeeding
        (is (= "EXCEPTIONS-ARE-AS" (subs sub (first r) (second r))))))))

(deftest break-014-does-not-retry
  (testing "BREAK does NOT retry — fails where BREAKX succeeds — Break_014"
    ;; POS(0) BREAK('A') . R2 'AS' on 'EXCEPTIONS-ARE-AS-TRUE-AS-RULES'
    ;; BREAK gets 'EXCEPTIONS-' (stops at first A), then tries 'AS' against 'ARE...' → fails
    ;; No retry, so overall failure
    (let [sub "EXCEPTIONS-ARE-AS-TRUE-AS-RULES"
          pat (SEQ (POS 0) (BREAK "A") "AS")]
      (is (nil? (SEARCH sub pat))))))

(deftest breakx-002-non-backtracking-match
  (testing "BREAKX match with first occurrence works — BreakX_001"
    (is (= ":= " (m ":= one,,, two,..  three"
                    (BREAKX "abcdefghijklmnopqrstuvwxyz"))))))

(deftest breakx-empty-subject
  (testing "BREAKX on empty subject fails"
    (is (nil? (SEARCH "" (BREAKX "abc"))))))

(deftest breakx-no-break-char-in-subject
  (testing "BREAKX fails when break char never found — BreakX_009"
    (is (nil? (SEARCH "12345" (BREAKX "8"))))))

(deftest breakx-matches-at-start
  (testing "BREAKX can match zero chars when subject starts with break char — BreakX_003"
    (is (= "" (m "one,,, two,..  three"
                  (BREAKX "abcdefghijklmnopqrstuvwxyz"))))))

;; ─── 9.2  NSPAN (0-or-more span) ─────────────────────────────────────────────

(deftest nspan-matches-nonempty
  (testing "NSPAN matches one or more chars in the set"
    (is (= "abc" (m "abcDEF" (NSPAN "abcdefghijklmnopqrstuvwxyz"))))))

(deftest nspan-matches-empty
  (testing "NSPAN succeeds even when no chars match (returns empty string)"
    ;; At position 0 of '123', no lowercase letters → NSPAN matches ε
    (let [r (SEARCH "123" (NSPAN "abcdefghijklmnopqrstuvwxyz"))]
      (is (some? r))
      (is (= "" (subs "123" (first r) (second r)))))))

(deftest nspan-vs-span-on-failure
  (testing "SPAN fails where NSPAN succeeds on no-char-matches"
    (is (nil?   (SEARCH "123" (SPAN  "abcdefghijklmnopqrstuvwxyz"))))
    (is (some?  (SEARCH "123" (NSPAN "abcdefghijklmnopqrstuvwxyz"))))))

;; ─── 9.3  BOL / EOL ──────────────────────────────────────────────────────────

(deftest bol-matches-at-position-zero
  (testing "BOL anchors match to start of subject"
    (is (some? (SEARCH "hello" (SEQ BOL "hello"))))
    (is (nil?  (SEARCH "hello" (SEQ BOL "ello"))))))

(deftest eol-matches-at-end
  (testing "EOL anchors match to end of subject"
    (is (some? (SEARCH "hello" (SEQ "hello" EOL))))
    (is (nil?  (SEARCH "hello" (SEQ "hell"  EOL))))))

(deftest bol-eol-fullmatch-equivalent
  (testing "BOL + EOL together behave like FULLMATCH anchoring"
    (is (some? (SEARCH "hello" (SEQ BOL "hello" EOL))))
    (is (nil?  (SEARCH "hello" (SEQ BOL "hell"  EOL))))))

(deftest bol-only-at-zero
  (testing "BOL refuses to match in the middle of a string (unanchored search)"
    ;; SEARCH tries all positions; BOL fails at every position except 0
    ;; so pattern BOL + 'ello' should fail
    (is (nil? (SEARCH "hello" (SEQ BOL "ello"))))))

;; ─── 9.4  $ (CAPTURE-IMM) vs . (CAPTURE-COND) ───────────────────────────────
;; Build capture nodes directly to avoid Clojure . interop ambiguity.
;; CAPTURE-COND = conditional (. operator), CAPTURE-IMM = immediate ($ operator)

(deftest capture-cond-assigns-on-success
  (testing "CAPTURE-COND assigns variable when match succeeds"
    (snobol-set! 'V ε)
    (let [pat (list 'CAPTURE-COND (SPAN "abc") 'V)]
      (SEARCH "abcXYZ" pat)
      (is (= "abc" ($$ 'V))))))

(deftest capture-cond-basic-capture
  (testing "CAPTURE-COND captures the text matched by P"
    (snobol-set! 'W ε)
    (let [pat (list 'CAPTURE-COND (SPAN "0123456789") 'W)]
      (SEARCH "123abc" pat)
      (is (= "123" ($$ 'W))))))

(deftest capture-imm-assigns-immediately
  (testing "CAPTURE-IMM ($ operator) assigns as soon as inner P matches"
    (snobol-set! 'X ε)
    (let [pat (list 'CAPTURE-IMM (SPAN "abc") 'X)]
      (SEARCH "abcDEF" pat)
      (is (= "abc" ($$ 'X))))))

(deftest capture-imm-captures-first-word
  (testing "CAPTURE-IMM captures first word from subject"
    (snobol-set! 'Y ε)
    (let [letters "abcdefghijklmnopqrstuvwxyz"
          pat     (list 'CAPTURE-IMM (SPAN letters) 'Y)]
      (SEARCH "hello world" pat)
      (is (= "hello" ($$ 'Y))))))

