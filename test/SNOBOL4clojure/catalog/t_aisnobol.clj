(ns SNOBOL4clojure.catalog.t_aisnobol
  "Sprint 17 — Shafto AI-SNOBOL corpus tests.

   Source material: 'Artificial Intelligence Programming in SNOBOL4'
     by Michael G. Shafto.
   Report text OCR scanned at Arizona State University by Mark Olsen.
   Scanned report extensively edited and corrected by Mark Emmer of Catspaw.
   SNOLISPIST library typed in by Martin Rice of the University of Tennessee.
   Converted to SNOBOL4+ and Macro SPITBOL by Mark Emmer, Catspaw, Inc.
   Distributed with the kind permission of Michael G. Shafto.
   Material may be distributed freely and without restriction.
   Source: corpus/aisnobol/ (verbatim distribution, committed as corpus).

   Programs tested:
     HSORT.SNO  — C.A.R. Hoare Quicksort with APPLY-based predicate
     ENDING.SNO — English word-ending morphology (Winograd 1972 flowchart)
     WANG.SNO   — Wang's algorithm for propositional logic theorem proving"
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-steplimit prog-include]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_aisnobol))) (f)))

(def ^:private AIDIR "corpus/aisnobol")

;; ── HSORT (Shafto version) ────────────────────────────────────────────────────
;;
;; HSORT.SNO from the Shafto corpus implements C.A.R. Hoare's Quicksort with
;; a predicate argument passed as a name (.LLE, .LGE, .LE, .GE), so the sort
;; order is determined by APPLY(P, V1, V2) at runtime.  This is a different
;; implementation from the Gimpel HSORT.INC — it also handles two-element
;; base cases directly, and uses inner HSORT.SWAP/HSORT.OK/HSORT.KO helpers.
;;
;; We test it by embedding just the function definitions (omitting the stdin
;; read loop) and calling HSORT directly.

(deftest shafto_hsort_lexical_ascending
  "Shafto HSORT (from HSORT.SNO) — sort strings lexically ascending (.LLE)."
  (prog-steplimit 8000 3000
    (str
      " DEFINE('HSORT(A,I,N,P)J,K,C')\n"
      " DEFINE('HSORT.KO(V1,V2)')\n"
      " DEFINE('HSORT.OK(V1,V2)')\n"
      " DEFINE('HSORT.SWAP(I1,I2)T')              :(HSORT.END)\n"
      "HSORT\n"
      "      GT(N - I,  1)                        :S(HSORT1)\n"
      "      GE(I, N)                             :S(RETURN)\n"
      "      (HSORT.KO(A<I>,A<N>)   HSORT.SWAP(I,N))\n"
      "         :(RETURN)\n"
      "HSORT1\n"
      "      C = A<(I + N) / 2>\n"
      "      J = I - 1\n"
      "      K = N + 1\n"
      "HSORT2   J = J + 1\n"
      "      HSORT.OK(C,A<J>)                     :F(HSORT2)\n"
      "HSORT3   K = K - 1\n"
      "      HSORT.OK(A<K>,C)                     :F(HSORT3)\n"
      "      (LT(J,K)    HSORT.SWAP(J,K))         :S(HSORT2)\n"
      "      HSORT(A, I, K, P)\n"
      "      HSORT(A, K + 1, N, P)                :(RETURN)\n"
      "HSORT.SWAP  T = A<I1> ; A<I1> = A<I2> ; A<I2> = T     :(RETURN)\n"
      "HSORT.OK      APPLY(P,V1,V2)               :S(RETURN)F(FRETURN)\n"
      "HSORT.KO      APPLY(P,V1,V2)               :S(FRETURN)F(RETURN)\n"
      "HSORT.END\n"
      "      &TRIM = 1\n"
      "      A = ARRAY(5)\n"
      "      A<1> = 'ZEBRA'\n"
      "      A<2> = 'ANT'\n"
      "      A<3> = 'CAT'\n"
      "      A<4> = 'BEE'\n"
      "      A<5> = 'DOG'\n"
      "      HSORT(A, 1, 5, .LLE)\n"
      "END"))
  (is (= "ANT"   (SNOBOL4clojure.env/array-get ($$ 'A) [1])))
  (is (= "BEE"   (SNOBOL4clojure.env/array-get ($$ 'A) [2])))
  (is (= "CAT"   (SNOBOL4clojure.env/array-get ($$ 'A) [3])))
  (is (= "DOG"   (SNOBOL4clojure.env/array-get ($$ 'A) [4])))
  (is (= "ZEBRA" (SNOBOL4clojure.env/array-get ($$ 'A) [5]))))

(deftest shafto_hsort_numeric_ascending
  "Shafto HSORT — sort integers numerically ascending (.LE)."
  (prog-steplimit 8000 3000
    (str
      " DEFINE('HSORT(A,I,N,P)J,K,C')\n"
      " DEFINE('HSORT.KO(V1,V2)')\n"
      " DEFINE('HSORT.OK(V1,V2)')\n"
      " DEFINE('HSORT.SWAP(I1,I2)T')              :(HSORT.END)\n"
      "HSORT\n"
      "      GT(N - I,  1)                        :S(HSORT1)\n"
      "      GE(I, N)                             :S(RETURN)\n"
      "      (HSORT.KO(A<I>,A<N>)   HSORT.SWAP(I,N))\n"
      "         :(RETURN)\n"
      "HSORT1\n"
      "      C = A<(I + N) / 2>\n"
      "      J = I - 1\n"
      "      K = N + 1\n"
      "HSORT2   J = J + 1\n"
      "      HSORT.OK(C,A<J>)                     :F(HSORT2)\n"
      "HSORT3   K = K - 1\n"
      "      HSORT.OK(A<K>,C)                     :F(HSORT3)\n"
      "      (LT(J,K)    HSORT.SWAP(J,K))         :S(HSORT2)\n"
      "      HSORT(A, I, K, P)\n"
      "      HSORT(A, K + 1, N, P)                :(RETURN)\n"
      "HSORT.SWAP  T = A<I1> ; A<I1> = A<I2> ; A<I2> = T     :(RETURN)\n"
      "HSORT.OK      APPLY(P,V1,V2)               :S(RETURN)F(FRETURN)\n"
      "HSORT.KO      APPLY(P,V1,V2)               :S(FRETURN)F(RETURN)\n"
      "HSORT.END\n"
      "      A = ARRAY(4)\n"
      "      A<1> = 50\n"
      "      A<2> = 10\n"
      "      A<3> = 30\n"
      "      A<4> = 20\n"
      "      HSORT(A, 1, 4, .LE)\n"
      "END"))
  (is (= 10 (SNOBOL4clojure.env/array-get ($$ 'A) [1])))
  (is (= 20 (SNOBOL4clojure.env/array-get ($$ 'A) [2])))
  (is (= 30 (SNOBOL4clojure.env/array-get ($$ 'A) [3])))
  (is (= 50 (SNOBOL4clojure.env/array-get ($$ 'A) [4]))))

(deftest shafto_hsort_two_elements
  "Shafto HSORT — two-element base case (special path in algorithm)."
  (prog-steplimit 5000 1000
    (str
      " DEFINE('HSORT(A,I,N,P)J,K,C')\n"
      " DEFINE('HSORT.KO(V1,V2)')\n"
      " DEFINE('HSORT.OK(V1,V2)')\n"
      " DEFINE('HSORT.SWAP(I1,I2)T')              :(HSORT.END)\n"
      "HSORT\n"
      "      GT(N - I,  1)                        :S(HSORT1)\n"
      "      GE(I, N)                             :S(RETURN)\n"
      "      (HSORT.KO(A<I>,A<N>)   HSORT.SWAP(I,N))\n"
      "         :(RETURN)\n"
      "HSORT1\n"
      "      C = A<(I + N) / 2>\n"
      "      J = I - 1\n"
      "      K = N + 1\n"
      "HSORT2   J = J + 1\n"
      "      HSORT.OK(C,A<J>)                     :F(HSORT2)\n"
      "HSORT3   K = K - 1\n"
      "      HSORT.OK(A<K>,C)                     :F(HSORT3)\n"
      "      (LT(J,K)    HSORT.SWAP(J,K))         :S(HSORT2)\n"
      "      HSORT(A, I, K, P)\n"
      "      HSORT(A, K + 1, N, P)                :(RETURN)\n"
      "HSORT.SWAP  T = A<I1> ; A<I1> = A<I2> ; A<I2> = T     :(RETURN)\n"
      "HSORT.OK      APPLY(P,V1,V2)               :S(RETURN)F(FRETURN)\n"
      "HSORT.KO      APPLY(P,V1,V2)               :S(FRETURN)F(RETURN)\n"
      "HSORT.END\n"
      "      A = ARRAY(2)\n"
      "      A<1> = 'Z'\n"
      "      A<2> = 'A'\n"
      "      HSORT(A, 1, 2, .LLE)\n"
      "END"))
  (is (= "A" (SNOBOL4clojure.env/array-get ($$ 'A) [1])))
  (is (= "Z" (SNOBOL4clojure.env/array-get ($$ 'A) [2]))))

;; ── WANG (theorem prover) ─────────────────────────────────────────────────────
;;
;; WANG.SNO implements Wang's algorithm for propositional logic.
;; Given a formula in prefix notation (NOT, AND, OR, IMP, EQU),
;; WANG(antecedent, consequent) succeeds iff the sequent is valid,
;; fails otherwise.
;;
;; From the Shafto corpus, adapted from Griswold, Poage & Polonsky
;; *The SNOBOL4 Programming Language*, pp. 183-185.
;;
;; We embed the function definitions and test specific sequents.

(def ^:private wang-defs
  (str
    " DEFINE('WANG(ANTECEDENT,CONSEQUENT)PHI,PSI')\n"
    " UNOP = 'NOT'\n"
    " BINOP  = ('AND' | 'IMP' | 'OR' | 'EQU')\n"
    " UNOP.FORMULA = ' ' (UNOP . OP) '(' (BAL . PHI) ')'\n"
    " BINOP.FORMULA = ' ' (BINOP . OP) '(' (BAL . PHI) ','\n"
    "                     (BAL . PSI) ')'\n"
    " FORMULA = UNOP.FORMULA | BINOP.FORMULA\n"
    " ATOM = (NOTANY(' ') (BREAK(' ') | REM)) . A\n"
    "                                               :(WANG.END)\n"
    "WANG\n"
    "       ANTECEDENT FORMULA = NULL\n"
    "+             :F(WANG1)S( $('WANG.A.' OP) )\n"
    "WANG.A.NOT\n"
    "       WANG(ANTECEDENT, CONSEQUENT ' ' PHI)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG.A.AND\n"
    "       WANG(ANTECEDENT ' ' PHI ' ' PSI, CONSEQUENT)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG.A.OR\n"
    "       WANG(ANTECEDENT ' ' PHI, CONSEQUENT)     :F(FRETURN)\n"
    "       WANG(ANTECEDENT ' ' PSI, CONSEQUENT)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG.A.IMP\n"
    "       WANG(ANTECEDENT ' ' PSI, CONSEQUENT)     :F(FRETURN)\n"
    "       WANG(ANTECEDENT, CONSEQUENT ' ' PHI)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG.A.EQU\n"
    "       WANG(ANTECEDENT ' ' PHI ' ' PSI, CONSEQUENT)\n"
    "+        :F(FRETURN)\n"
    "       WANG(ANTECEDENT, CONSEQUENT ' ' PHI ' ' PSI)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG1\n"
    "       CONSEQUENT FORMULA =\n"
    "+          :F(WANG2)S( $('WANG.C.' OP) )\n"
    "WANG.C.NOT\n"
    "       WANG(ANTECEDENT ' ' PHI, CONSEQUENT)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG.C.AND\n"
    "       WANG(ANTECEDENT, CONSEQUENT ' ' PHI)       :F(FRETURN)\n"
    "       WANG(ANTECEDENT, CONSEQUENT ' ' PSI)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG.C.OR\n"
    "       WANG(ANTECEDENT, CONSEQUENT ' ' PHI ' ' PSI)\n"
    "+          :S(RETURN)F(FRETURN)\n"
    "WANG.C.IMP\n"
    "       WANG(ANTECEDENT ' ' PHI, CONSEQUENT ' ' PSI)\n"
    "+         :S(RETURN)F(FRETURN)\n"
    "WANG.C.EQU\n"
    "       WANG(ANTECEDENT ' ' PHI, CONSEQUENT ' ' PSI)\n"
    "+        :F(FRETURN)\n"
    "       WANG(ANTECEDENT ' ' PSI, CONSEQUENT ' ' PHI)\n"
    "+        :S(RETURN)F(FRETURN)\n"
    "WANG2\n"
    "       ANTECEDENT ATOM =                          :F(FRETURN)\n"
    "       CONSEQUENT A                               :S(RETURN)F(WANG2)\n"
    "WANG.END\n"))

(deftest wang_valid_formula_imp_not_or
  "Wang: IMP(NOT(OR(P,Q)),NOT(P)) is valid (tautology)."
  ;; Proof: NOT(OR(P,Q)) => NOT(P) because OR(P,Q) => P
  (prog-steplimit 15000 5000
    (str wang-defs
         " &ANCHOR = 0\n"
         " &FULLSCAN = 1\n"
         " &TRIM = 1\n"
         "       VALID = WANG(NULL, ' IMP(NOT(OR(P,Q)),NOT(P))')  'VALID'\n"
         "                                                          :S(END)\n"
         "       VALID = 'INVALID'\n"
         "END"))
  (is (= "VALID" ($$ 'VALID))))

(deftest wang_invalid_formula
  "Wang: NOT(IMP(NOT(OR(P,Q)),NOT(P))) is not valid."
  (prog-steplimit 15000 5000
    (str wang-defs
         " &ANCHOR = 0\n"
         " &FULLSCAN = 1\n"
         " &TRIM = 1\n"
         "       RESULT = WANG(NULL, ' NOT(IMP(NOT(OR(P,Q)),NOT(P)))')  'VALID'\n"
         "                                                          :S(END)\n"
         "       RESULT = 'INVALID'\n"
         "END"))
  (is (= "INVALID" ($$ 'RESULT))))

(deftest wang_simple_atom_identity
  "Wang: P => P  (trivial axiom — atom appears in both antecedent and consequent)."
  (prog-steplimit 5000 1000
    (str wang-defs
         " &ANCHOR = 0\n"
         " &FULLSCAN = 1\n"
         " &TRIM = 1\n"
         "       RESULT = WANG(' P', ' P') 'VALID'  :S(END)\n"
         "       RESULT = 'INVALID'\n"
         "END"))
  (is (= "VALID" ($$ 'RESULT))))

(deftest wang_and_elimination
  "Wang: AND(P,Q) => P  (AND elimination is valid)."
  (prog-steplimit 8000 2000
    (str wang-defs
         " &ANCHOR = 0\n"
         " &FULLSCAN = 1\n"
         " &TRIM = 1\n"
         "       RESULT = WANG(' AND(P,Q)', ' P') 'VALID'  :S(END)\n"
         "       RESULT = 'INVALID'\n"
         "END"))
  (is (= "VALID" ($$ 'RESULT))))

;; ── ENDING (word morphology) ───────────────────────────────────────────────────
;;
;; ENDING.SNO implements the English word-ending analysis flowchart from
;;   Winograd, T. *Understanding Natural Language*. Academic Press, 1972, p.74.
;; Given an inflected word form, WORDEND(WORD) strips the inflectional suffix
;; and stores the base form in WRD.
;;
;; The original program reads from stdin.  We test by calling WORDEND directly
;; with a small inline dictionary (the same one used in the corpus .SNO file)
;; and checking that each inflected form reduces to the expected base.

(def ^:private ending-defs
  ;; Full WORDEND function + helper definitions from ENDING.SNO
  (str
    " &FULLSCAN = 1\n"
    " &TRIM = 1\n"
    " DEFINE('WORDEND(WORD)L,VOWEL,DOUBLE,LIQUID,NOEND')\n"
    " DEFINE('MATCH(L,PAT)')\n"
    " DEFINE('CUT(N)')\n"
    " DEFINE('ADDON(X)')\n"
    " DEFINE('TRY()')\n"
    "            :(WORDEND.END)\n"
    "WORDEND\n"
    "        WRD = WORD\n"
    "        DOUBLE = (LEN(1) $ L) *L RPOS(0)\n"
    "        LIQUID = ANY(\"LRSVZ\")\n"
    "        NOEND = ANY(\"CGSVZ\")\n"
    "        VOWEL = ANY(\"AEIOUY\")\n"
    "        WRD\n"
    "+       (\"N'T\" | \"'S\" | \"S'\" | \"S\" | \"LY\" |\n"
    "+        \"ING\" | \"ED\" | \"EN\" | \"ER\" | \"EST\")\n"
    "+        $ WEND RPOS(0) =                          :F(WTRY)\n"
    "        WEND POS(0) (\"S\" | \"'S\" | \"S'\" | \"N'T\") RPOS(0)\n"
    "+                                                 :F(WORDEND.1)\n"
    "        MATCH(1,\"E\")                              :F(WTRY)\n"
    "        MATCH(2,\"I\") CUT(2) ADDON(\"Y\")            :S(WTRY)\n"
    "        MATCH(2,\"TH\")                             :S(WTRY)\n"
    "        MATCH(2,ANY(\"HX\")) CUT(1)                 :S(WTRY)\n"
    "        MATCH(2,ANY(\"SZ\") ANY(\"SZ\")) CUT(1)       :S(WTRY)\n"
    "        MATCH(2,ANY(\"SZ\"))                        :S(WTRY)\n"
    "        MATCH(2,\"V\")                              :F(WTRY)\n"
    "        ~TRY() CUT(2) ADDON(\"FE\")                 :S(WTRY)F(RETURN)\n"
    "WORDEND.1\n"
    "        IDENT(WEND,\"LY\")                          :F(WORDEND.2)\n"
    "        MATCH(1,\"I\") CUT(1) ADDON(\"Y\")            :S(WTRY)\n"
    "        ~TRY() ADDON(\"LE\")                        :S(WTRY)F(RETURN)\n"
    "WORDEND.2\n"
    "        MATCH(1,VOWEL)                            :F(WORDEND.3)\n"
    "        MATCH(1,\"I\") CUT(1) ADDON(\"Y\")            :S(WTRY)\n"
    "        MATCH(1,\"Y\")                              :S(WTRY)\n"
    "        ~MATCH(1,\"E\") ADDON(\"E\")                  :S(WTRY)\n"
    "        MATCH(2,\"E\")                              :S(WTRY)\n"
    "        ~TRY() ADDON(\"E\")                         :S(WTRY)F(RETURN)\n"
    "WORDEND.3\n"
    "        MATCH(1,\"H\")                              :F(WORDEND.4)\n"
    "        MATCH(2,\"T\")                              :F(WTRY)\n"
    "        ~TRY() ADDON(\"E\")                         :S(WTRY)F(RETURN)\n"
    "WORDEND.4\n"
    "        WRD DOUBLE                                :F(WORDEND.5)\n"
    "        ~MATCH(1,LIQUID) CUT(1)                   :S(WTRY)\n"
    "        ~TRY() CUT(1)                             :S(WTRY)F(RETURN)\n"
    "WORDEND.5\n"
    "        MATCH(2,VOWEL)                            :S(WORDEND.6)\n"
    "        MATCH(1,\"RL\")                             :S(WTRY)\n"
    "        MATCH(1,LIQUID | NOEND) ADDON(\"E\")        :(WTRY)\n"
    "WORDEND.6\n"
    "        ~MATCH(3,VOWEL) ADDON(\"E\")                :S(WTRY)\n"
    "        MATCH(1,NOEND) ADDON(\"E\")                 :(WTRY)\n"
    "WTRY    TRY()                                     :S(RETURN)F(FRETURN)\n"
    "MATCH\n"
    "        WRD PAT RPOS(L - 1)                       :S(RETURN)F(FRETURN)\n"
    "CUT\n"
    "        WRD RPOS(N) REM =                         :(RETURN)\n"
    "ADDON\n"
    "        WRD = WRD X                               :(RETURN)\n"
    "TRY\n"
    "        DIFFER(DICTIONARY<WRD>)                   :S(RETURN)F(FRETURN)\n"
    "WORDEND.END\n"
    ;; Build dictionary (from ENDING.SNO)
    " DICTIONARY = TABLE(101)\n"
    " WORDS =\n"
    "+     'BASH,BATHE,LEAN,LEAVE,DENT,DANCE,DOG,KISS,CURVE,'\n"
    "+     'CURL,ROT,ROLL,PLAY,PLY,REAL,PALE,KNIFE,PRETTY,'\n"
    "+     'NOBLE,PATROL,'\n"
    "DICT.LOOP\n"
    "       WORDS BREAK(',') . W LEN(1) =              :F(DICT.DONE)\n"
    "       DICTIONARY<W> = 1                          :(DICT.LOOP)\n"
    "DICT.DONE\n"))

(deftest ending_inflected_s_forms
  "ENDING: plural -S forms reduce to base."
  (prog-steplimit 15000 5000
    (str ending-defs
         "       WORDEND('DOGS')    :F(END)\n"
         "       R_DOGS = WRD\n"
         "       WORDEND('ROLLS')   :F(END)\n"
         "       R_ROLLS = WRD\n"
         "       WORDEND('KISSES')  :F(END)\n"
         "       R_KISSES = WRD\n"
         "END"))
  (is (= "DOG"  ($$ 'R_DOGS)))
  (is (= "ROLL" ($$ 'R_ROLLS)))
  (is (= "KISS" ($$ 'R_KISSES))))

(deftest ending_ing_forms
  "ENDING: -ING forms reduce to base."
  (prog-steplimit 15000 5000
    (str ending-defs
         "       WORDEND('DANCING')  :F(END)\n"
         "       R_DANCING = WRD\n"
         "       WORDEND('LEAVING')  :F(END)\n"
         "       R_LEAVING = WRD\n"
         "       WORDEND('CURLING')  :F(END)\n"
         "       R_CURLING = WRD\n"
         "       WORDEND('ROTTING')  :F(END)\n"
         "       R_ROTTING = WRD\n"
         "END"))
  (is (= "DANCE" ($$ 'R_DANCING)))
  (is (= "LEAVE" ($$ 'R_LEAVING)))
  (is (= "CURL"  ($$ 'R_CURLING)))
  (is (= "ROT"   ($$ 'R_ROTTING))))

(deftest ending_ed_forms
  "ENDING: -ED forms reduce to base."
  (prog-steplimit 15000 5000
    (str ending-defs
         "       WORDEND('DENTED')   :F(END)\n"
         "       R_DENTED = WRD\n"
         "       WORDEND('CURVED')   :F(END)\n"
         "       R_CURVED = WRD\n"
         "       WORDEND('PALED')    :F(END)\n"
         "       R_PALED = WRD\n"
         "END"))
  (is (= "DENT"  ($$ 'R_DENTED)))
  (is (= "CURVE" ($$ 'R_CURVED)))
  (is (= "PALE"  ($$ 'R_PALED))))

(deftest ending_est_er_forms
  "ENDING: -EST and -ER superlative/comparative forms."
  (prog-steplimit 15000 5000
    (str ending-defs
         "       WORDEND('PRETTIEST') :F(END)\n"
         "       R_PRETTIEST = WRD\n"
         "       WORDEND('NOBLEST')   :F(END)\n"
         "       R_NOBLEST = WRD\n"
         "END"))
  (is (= "PRETTY" ($$ 'R_PRETTIEST)))
  (is (= "NOBLE"  ($$ 'R_NOBLEST))))

(deftest ending_identity_no_change
  "ENDING: base forms that need no change return themselves."
  (prog-steplimit 10000 2000
    (str ending-defs
         "       WORDEND('LEAN')    :F(END)\n"
         "       R_LEAN = WRD\n"
         "       WORDEND('BASH')    :F(END)\n"
         "       R_BASH = WRD\n"
         "       WORDEND('PATROL')  :F(END)\n"
         "       R_PATROL = WRD\n"
         "END"))
  (is (= "LEAN"   ($$ 'R_LEAN)))
  (is (= "BASH"   ($$ 'R_BASH)))
  (is (= "PATROL" ($$ 'R_PATROL))))
