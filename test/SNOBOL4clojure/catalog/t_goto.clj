(ns SNOBOL4clojure.catalog.t_goto
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_goto))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_goto_skip
  "Unconditional goto skips code"
  (prog
    "        R = 'before'"
    "        :(SKIP)"
    "        R = 'skipped'"
    "SKIP    "
    "end"
  )
    (is (= "before" ($$ (quote R))))
)

(deftest worm_goto_end
  ":(END) halts program"
  (prog
    "        R = 'set'"
    "        :(END)"
    "        R = 'never'"
    "END"
  )
    (is (= "set" ($$ (quote R))))
)

(deftest worm_goto_forward_ref
  "Forward reference label resolves"
  (prog
    "        :(TARGET)"
    "        R = 'never'"
    "TARGET  R = 'reached'"
    "end"
  )
    (is (= "reached" ($$ (quote R))))
)

(deftest worm_goto_s_taken
  ":S branch taken on success"
  (prog
    "        EQ(1,1) :S(TAKEN)"
    "        R = 'not taken'   :(END)"
    "TAKEN   R = 'taken'"
    "END"
  )
    (is (= "taken" ($$ (quote R))))
)

(deftest worm_goto_s_not_taken
  ":S branch not taken on failure"
  (prog
    "        EQ(1,2) :S(TAKEN)"
    "        R = 'fell through'   :(END)"
    "TAKEN   R = 'taken'"
    "END"
  )
    (is (= "fell through" ($$ (quote R))))
)

(deftest worm_goto_f_taken
  ":F branch taken on failure"
  (prog
    "        EQ(1,2) :F(TAKEN)"
    "        R = 'not taken'   :(END)"
    "TAKEN   R = 'taken'"
    "END"
  )
    (is (= "taken" ($$ (quote R))))
)

(deftest worm_goto_sf_success
  ":S(x)F(y) — success side"
  (prog
    "        EQ(3,3) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "yes" ($$ (quote R))))
)

(deftest worm_goto_sf_failure
  ":S(x)F(y) — failure side"
  (prog
    "        EQ(3,4) :S(YES)F(NO)"
    "YES     R = 'yes'   :(END)"
    "NO      R = 'no'"
    "END"
  )
    (is (= "no" ($$ (quote R))))
)

;; ─────────────────────────────────────────────────────────────────────────────
;; Lowercase goto — regression tests for Issue #6
;; Fixed: grammar now accepts 's'/'f' as well as 'S'/'F' in goto clauses.
;; ─────────────────────────────────────────────────────────────────────────────

(deftest lowercase_s_goto_taken
  "GT(5,0) :s(HIT) — lowercase :s — branch taken"
  (prog
    "        I = 5"
    "        R = 'no'"
    "        GT(I,0) :s(HIT)"
    "        :(DONE)"
    "HIT"
    "        R = 'yes'"
    "DONE    end")
  (is (= "yes" ($$ 'R))))

(deftest lowercase_f_goto_taken
  "LT(5,0) :f(MISS) — lowercase :f — branch taken on failure"
  (prog
    "        I = 5"
    "        R = 'no'"
    "        LT(I,0) :f(MISS)"
    "        :(DONE)"
    "MISS"
    "        R = 'missed'"
    "DONE    end")
  (is (= "missed" ($$ 'R))))

(deftest mixed_lower_upper_goto
  "GT(0,5) :s(HIT)f(MISS) — mixed case s/F"
  (prog
    "        R = 'no'"
    "        GT(0,5) :s(HIT)f(MISS)"
    "        :(DONE)"
    "HIT"
    "        R = 'hit'"
    "        :(DONE)"
    "MISS"
    "        R = 'miss'"
    "DONE    end")
  (is (= "miss" ($$ 'R))))
