(ns SNOBOL4clojure.test-sprint13
  "Sprint 13: RETURN/FRETURN signals, local variable save/restore,
   APPLY, END label.
   Design rule: ALL language keywords UPPERCASE. No case folding.
   :(RETURN) :(FRETURN) :(NRETURN) :(END) :S(x) :F(x) always uppercase."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]))

(defmacro prog [& lines]
  `(RUN (CODE ~(clojure.string/join "\n" (map str lines)))))

;; ── 13.1  RETURN ─────────────────────────────────────────────────────────────

(deftest return-simple
  (prog
    "        DEFINE('DOUBLE(S)') :(DOUBLE_END)"
    "DOUBLE  DOUBLE = 2 * S      :(RETURN)"
    "DOUBLE_END"
    "        R = DOUBLE(5)"
    "end")
  (is (= 10 ($$ 'R))))

(deftest return-from-bump
  (prog
    "        DEFINE('BUMP(V)') :(BUMPEND)"
    "BUMP    BUMP = V + 1      :(RETURN)"
    "BUMPEND"
    "        S = ''"
    "        J = 0"
    "LOOP    S = S BUMP(2 * J)"
    "        J = J + 1"
    "        LT(J,10)          :S(LOOP)"
    "end")
  (is (= "135791113151719" ($$ 'S))))

(deftest return-explicit-entry-label
  (prog
    "        DEFINE('BUMP2(V)',.BUMPIT) :(BUMPEND2)"
    "BUMPIT  BUMP2 = V + 1             :(RETURN)"
    "BUMPEND2"
    "        R = BUMP2(7)"
    "end")
  (is (= 8 ($$ 'R))))

;; ── 13.2  FRETURN ────────────────────────────────────────────────────────────

(deftest freturn-success-path
  (prog
    "        DEFINE('SHIFT(S,N)FRONT,REST')"
    "        SHIFT_PAT = LEN(*N) . FRONT REM . REST :(SHIFT_END)"
    "SHIFT   S ? SHIFT_PAT                          :F(FRETURN)"
    "        SHIFT = REST FRONT                     :(RETURN)"
    "SHIFT_END"
    "        R = SHIFT('COTTON',4)"
    "end")
  (is (= "ONCOTT" ($$ 'R))))

(deftest freturn-triggers-failure-branch
  ;; Pattern too long -> FRETURN -> :S(ENDMARK) not taken -> S2 = 'FAILURE'
  (prog
    "        DEFINE('SHIFT2(S,N)FRONT,REST')"
    "        SHIFT2_PAT = LEN(*N) . FRONT REM . REST :(SHIFT2_END)"
    "SHIFT2  S ? SHIFT2_PAT                          :F(FRETURN)"
    "        SHIFT2 = REST FRONT                     :(RETURN)"
    "SHIFT2_END"
    "        R2 = SHIFT2('OAK',4)                    :S(ENDMARK)"
    "        S2 = 'FAILURE'"
    "ENDMARK end")
  (is (= "FAILURE" ($$ 'S2))))

;; ── 13.3  Local variable save/restore ────────────────────────────────────────

(deftest locals-saved-and-restored
  (prog
    "        DEFINE('ADD(X,Y)FRONT,REST')"
    "        FRONT = 99"
    "        REST  = 88"
    "        X     = 77"
    "        Y     = 66          :(ADDEND)"
    "ADD     ADD = X + Y"
    "        FRONT = 9"
    "        REST  = 8           :(RETURN)"
    "ADDEND  A = ADD(3,4)"
    "end")
  (is (= 7   ($$ 'A)))
  (is (= 99  ($$ 'FRONT)))
  (is (= 88  ($$ 'REST))))

(deftest params-restored-after-call
  (prog
    "        DEFINE('SQ(N)')"
    "        N = 100             :(SQEND)"
    "SQ      SQ = N * N          :(RETURN)"
    "SQEND   R = SQ(5)"
    "end")
  (is (= 25  ($$ 'R)))
  (is (= 100 ($$ 'N))))

;; ── 13.4  Recursion ──────────────────────────────────────────────────────────

(deftest fibonacci-recursive
  (prog
    "            DEFINE('FIBONACCI(N)')  :(FIBONACCI_END)"
    "FIBONACCI   LE(N,1)                 :S(FIB_BASE)"
    "            FIBONACCI = FIBONACCI(N - 1)"
    "            FIBONACCI = FIBONACCI + FIBONACCI(N - 2)  :(RETURN)"
    "FIB_BASE    FIBONACCI = N           :(RETURN)"
    "FIBONACCI_END"
    "            F = FIBONACCI(10)"
    "end")
  (is (= 55 ($$ 'F))))

;; ── 13.5  APPLY ──────────────────────────────────────────────────────────────

(deftest apply-two-args
  (prog
    "        A = 'GT'"
    "        B = 3"
    "        C = 1"
    "        R1 = GT(B,C)       'success'"
    "        R2 = APPLY(A,B,C)  'success'"
    "end")
  (is (= "success" ($$ 'R1)))
  (is (= "success" ($$ 'R2))))

(deftest apply-single-arg
  ;; GT requires 2 args — single arg causes statement failure, R1/R2 stay ""
  (prog
    "        A = 'GT'"
    "        B = 3"
    "        R1 = GT(B)       'success'"
    "        R2 = APPLY(A,B)  'success'"
    "end")
  (is (= "" ($$ 'R1)))
  (is (= "" ($$ 'R2))))

(deftest apply-user-defined-fn
  (prog
    "        DEFINE('TRIPLE(V)') :(TRIPLE_END)"
    "TRIPLE  TRIPLE = 3 * V      :(RETURN)"
    "TRIPLE_END"
    "        FNAME = 'TRIPLE'"
    "        R = APPLY(FNAME, 7)"
    "end")
  (is (= 21 ($$ 'R))))

(deftest apply-failure-path
  ;; GT(1,3) fails — R2 stays ""
  (prog
    "        A  = 'GT'"
    "        B  = 1"
    "        C  = 3"
    "        R2 = APPLY(A,B,C)  'success'"
    "end")
  (is (= "" ($$ 'R2))))

;; ── 13.6  END label ──────────────────────────────────────────────────────────

(deftest end-halts-program
  (prog
    "        R = 'before'"
    "end"
    "        R = 'after'")
  (is (= "before" ($$ 'R))))

(deftest goto-end-halts
  (prog
    "        R = 'set'"
    "        :(END)"
    "        R = 'clobbered'"
    "end")
  (is (= "set" ($$ 'R))))
