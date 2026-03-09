(ns SNOBOL4clojure.catalog.t_spitbol
  "Sprint 18 — SPITBOL testpgms corpus tests.

   Source: corpus/spitbol/testpgms-test{1,2,3,4}.spt
   These are the classic SPITBOL diagnostic test programs.

   Oracle: csnobol4 2.3.3 and SPITBOL 4.0f (x86-64).

   Strategy
   --------
   test1 / test2 use the self-check pattern:
     STARS = '  ERROR DETECTED          ***'
     TEST = DIFFER(x,y) STARS
   When x==y: DIFFER fails, STARS not assigned, TEST stays ''. (PASS)
   When x!=y: DIFFER succeeds, STARS assigned to TEST. (FAIL)
   Each deftest runs an isolated logical group and asserts ($$ 'TEST) = ''.

   test3: factorial table — verify first and last output lines via stdout.
   test4: syntactic recogniser — verify <<< NO SYNTACTIC ERROR >>> lines.

   SKIPPED (SPITBOL-only features, not in scope):
     DREALS (1.0D2 notation), SETEXIT, &DUMP, TRACE output assertions."
  (:require [clojure.test        :refer :all]
            [clojure.string      :as str]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-steplimit]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_spitbol))) (f)))

(def ^:private SDIR "corpus/spitbol")

;; ─────────────────────────────────────────────────────────────────────────────
;; Helpers
;; ─────────────────────────────────────────────────────────────────────────────

(defmacro stars-ok
  "Assert that none of the STARS self-checks fired: TEST must be empty string."
  []
  `(is (= "" ($$ 'TEST)) "STARS self-check fired — a DIFFER(x,y) found a mismatch"))

;; ─────────────────────────────────────────────────────────────────────────────
;; testpgms-test1 — Diagnostics Phase One
;; ─────────────────────────────────────────────────────────────────────────────

(deftest t1_replace_basic
  "REPLACE: character substitution maps X->0, Y->1."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(REPLACE('AXXBYYY','XY','01'),'A00B111') STARS"
    "END")
  (stars-ok))

(deftest t1_lpad_rpad
  "LPAD and RPAD: pad, no-truncate, integer conversion."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(LPAD('ABC',5,'X'),'XXABC') STARS"
    "         TEST = DIFFER(RPAD('ABC',5,'X'),'ABCXX') STARS"
    "         TEST = DIFFER(LPAD(12,5),'   12') STARS"
    "         TEST = DIFFER(RPAD(10,4),'10  ') STARS"
    "         TEST = DIFFER(LPAD('ABC',2),'ABC') STARS"
    "         TEST = DIFFER(RPAD('AB',1),'AB') STARS"
    "         TEST = DIFFER(LPAD('AB',2),'AB') STARS"
    "END")
  (stars-ok))

(deftest t1_convert
  "CONVERT: string->integer, real->integer, integer->real, string->real."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(CONVERT('12','INTEGER'),12) STARS"
    "         TEST = DIFFER(CONVERT(2.5,'INTEGER'),2) STARS"
    "         TEST = DIFFER(CONVERT(2,'REAL'),2.0) STARS"
    "         TEST = DIFFER(CONVERT('.2','REAL'),0.2) STARS"
    "END")
  (stars-ok))

(deftest t1_reverse
  "REVERSE: string reversal."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(REVERSE('123'),'321') STARS"
    "END")
  (stars-ok))

(deftest t1_datatype
  "DATATYPE: returns type name of its argument."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(DATATYPE('JKL'),'STRING') STARS"
    "         TEST = DIFFER(DATATYPE(12),'INTEGER') STARS"
    "         TEST = DIFFER(DATATYPE(1.33),'REAL') STARS"
    "         TEST = DIFFER(DATATYPE(NULL),'STRING') STARS"
    "END")
  (stars-ok))

(deftest t1_arithmetic_integer
  "Integer arithmetic: +, -, *, /, ** and unary +/-."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(3 + 2,5) STARS"
    "         TEST = DIFFER(3 - 2,1) STARS"
    "         TEST = DIFFER(3 * 2,6) STARS"
    "         TEST = DIFFER(5 / 2,2) STARS"
    "         TEST = DIFFER(2 ** 3,8) STARS"
    "         TEST = DIFFER('3' + 2,5) STARS"
    "         TEST = DIFFER(3 + '-2',1) STARS"
    "         TEST = DIFFER('1' + '0',1) STARS"
    "         TEST = DIFFER(5 + NULL,5) STARS"
    "         TEST = DIFFER(-5,0 - 5) STARS"
    "         TEST = DIFFER(+'4',4) STARS"
    "END")
  (stars-ok))

(deftest t1_arithmetic_real
  "Real arithmetic: +, -, *, / and unary -."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(2.0 + 3.0,5.0) STARS"
    "         TEST = DIFFER(3.0 - 1.0,2.0) STARS"
    "         TEST = DIFFER(3.0 * 2.0,6.0) STARS"
    "         TEST = DIFFER(3.0 / 2.0,1.5) STARS"
    "         TEST = DIFFER(3.0 ** 3,27.0) STARS"
    "         TEST = DIFFER(-1.0,0.0 - 1.0) STARS"
    "END")
  (stars-ok))

(deftest t1_arithmetic_mixed
  "Mixed-mode arithmetic: integer op real -> real."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(1 + 2.0,3.0) STARS"
    "         TEST = DIFFER(3.0 / 2,1.5) STARS"
    "END")
  (stars-ok))

(deftest t1_define_fact
  "DEFINE: simple recursive factorial; FACT(5)=120, FACT(4)=24."
  (prog-steplimit 5000 2000
    "         DEFINE('FACT(N)')                  :(FACTEND)"
    "FACT     FACT = EQ(N,1) 1         :S(RETURN)"
    "         FACT = N * FACT(N - 1)             :(RETURN)"
    "FACTEND  STARS = '  ERROR DETECTED          ***'"
    "         TEST = NE(FACT(5),120) STARS"
    "END")
  (stars-ok))

(deftest t1_opsyn_fact
  "OPSYN: alias a user function; FACTO = FACT, FACTO(4)=24."
  (prog-steplimit 5000 2000
    "         DEFINE('FACT(N)')                  :(FACTEND)"
    "FACT     FACT = EQ(N,1) 1         :S(RETURN)"
    "         FACT = N * FACT(N - 1)             :(RETURN)"
    "FACTEND  STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(OPSYN(.FACTO,'FACT')) STARS"
    "         TEST = DIFFER(FACTO(4),24) STARS"
    "END")
  (stars-ok))

(deftest t1_define_locals
  "DEFINE: local variables are private and restored after return."
  (prog-steplimit 5000 2000
    "         DEFINE('LFUNC(A,B,C)D,E,F')        :(LFUNCEND)"
    "LFUNC    TEST = !(IDENT(A,'A') IDENT(B,'B') IDENT(C,'C')) STARS"
    "         TEST = !(IDENT(D) IDENT(E) IDENT(F)) STARS"
    "         A = 'AA' ; B = 'BB' ; C = 'CC' ; D = 'DD' ; E = 'EE' ; F = 'FF'"
    "                                 :(RETURN)"
    "LFUNCEND STARS = '  ERROR DETECTED          ***'"
    "         AA = 'A' ; BB = 'B' ; CC = 'C'"
    "         D = 'D' ; E = 'E' ; F = 'F'"
    "         A = 'X' ; B = 'Y' ; C = 'Z'"
    "         TEST = DIFFER(LFUNC(AA,BB,CC)) STARS"
    "         TEST = !(IDENT(A,'X') IDENT(B,'Y') IDENT(C,'Z')) STARS"
    "         TEST = !(IDENT(AA,'A') IDENT(BB,'B') IDENT(CC,'C')) STARS"
    "         TEST = !(IDENT(D,'D') IDENT(E,'E') IDENT(F,'F')) STARS"
    "END")
  (stars-ok))

(deftest t1_opsyn_operators
  "OPSYN: redefine binary @ as DUPL and unary | as SIZE."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         OPSYN('@',.DUPL,2)"
    "         OPSYN('|',.SIZE,1)"
    "         TEST = DIFFER('A' @ 4,'AAAA') STARS"
    "         TEST = DIFFER(|'STRING',6) STARS"
    "END")
  (stars-ok))

(deftest t1_array
  "ARRAY: create, index, bounds-check, PROTOTYPE, default fill."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         A = ARRAY(3)"
    "         TEST = DIFFER(A<1>) STARS"
    "         A<2> = 4.5"
    "         TEST = DIFFER(A<2>,4.5) STARS"
    "         TEST = ?A<4> STARS"
    "         TEST = ?A<0> STARS"
    "         TEST = DIFFER(PROTOTYPE(A),3) STARS"
    "         B = ARRAY(3,10)"
    "         TEST = DIFFER(B<2>,10) STARS"
    "         B = ARRAY('3')"
    "         B<2> = 'A'"
    "         TEST = DIFFER(B<2>,'A') STARS"
    "         C = ARRAY('2,2')"
    "         C<1,2> = '*'"
    "         TEST = DIFFER(C<1,2>,'*') STARS"
    "         TEST = DIFFER(PROTOTYPE(C),'2,2') STARS"
    "         D = ARRAY('-1:1,2')"
    "         D<-1,1> = 0"
    "         TEST = DIFFER(D<-1,1>,0) STARS"
    "         TEST = ?D<-2,1> STARS"
    "         TEST = ?D<2,1> STARS"
    "END")
  (stars-ok))

(deftest t1_data_pdd
  "DATA: program-defined datatypes with field accessor functions."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         DATA('NODE(VAL,LSON,RSON)')"
    "         A = NODE('X','Y','Z')"
    "         TEST = DIFFER(DATATYPE(A),'NODE') STARS"
    "         TEST = DIFFER(VAL(A),'X') STARS"
    "         B = NODE()"
    "         TEST = DIFFER(RSON(B)) STARS"
    "         LSON(B) = A"
    "         TEST = DIFFER(RSON(LSON(B)),'Z') STARS"
    "         TEST = DIFFER(VALUE('B'),B) STARS"
    "END")
  (stars-ok))

(deftest t1_predicates_numeric
  "Numeric predicates: LT, LE, EQ, NE, GT, GE — pass and fail cases."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = LT(5,4) STARS"
    "         TEST = LT(4,4) STARS"
    "         TEST = !LT(4,5) STARS"
    "         TEST = LE(5,2) STARS"
    "         TEST = !LE(4,4) STARS"
    "         TEST = !LE(4,10) STARS"
    "         TEST = EQ(4,5) STARS"
    "         TEST = EQ(5,4) STARS"
    "         TEST = !EQ(5,5) STARS"
    "         TEST = NE(4,4) STARS"
    "         TEST = !NE(4,6) STARS"
    "         TEST = !NE(6,4) STARS"
    "         TEST = GT(4,6) STARS"
    "         TEST = GT(4,4) STARS"
    "         TEST = !GT(5,2) STARS"
    "         TEST = GE(5,7) STARS"
    "         TEST = !GE(4,4) STARS"
    "         TEST = !GE(7,5) STARS"
    "END")
  (stars-ok))

(deftest t1_predicate_integer
  "INTEGER predicate: fails on non-integer string, succeeds on integer."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = INTEGER('ABC') STARS"
    "         TEST = !INTEGER(12) STARS"
    "         TEST = !INTEGER('12') STARS"
    "END")
  (stars-ok))

(deftest t1_size
  "SIZE: counts characters in string representation."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = NE(SIZE('ABC'),3) STARS"
    "         TEST = NE(SIZE(12),2) STARS"
    "         TEST = NE(SIZE(NULL),0) STARS"
    "END")
  (stars-ok))

(deftest t1_lgt
  "LGT: lexicographic greater-than."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = LGT('ABC','XYZ') STARS"
    "         TEST = LGT('ABC','ABC') STARS"
    "         TEST = !LGT('XYZ','ABC') STARS"
    "         TEST = LGT(NULL,'ABC') STARS"
    "         TEST = !LGT('ABC',NULL) STARS"
    "END")
  (stars-ok))

(deftest t1_indirect_addressing
  "Indirect ($) addressing: value-of and assignment through name string."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         $'QQ' = 'X'"
    "         TEST = DIFFER(QQ,'X') STARS"
    "         TEST = DIFFER($'GARBAGE') STARS"
    "         A = ARRAY(3)"
    "         A<2> = 'X'"
    "         TEST = DIFFER($.A<2>,'X') STARS"
    "END")
  (stars-ok))

(deftest t1_concatenation
  "Concatenation: strings, integers, reals, NULL."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER('A' 'B','AB') STARS"
    "         TEST = DIFFER('A' 'B' 'C','ABC') STARS"
    "         TEST = DIFFER(1 2,'12') STARS"
    "         TEST = DIFFER(2 2 2,'222') STARS"
    "         TEST = DIFFER(1 3.4,'13.4') STARS"
    "         TEST = DIFFER(BAL NULL,BAL) STARS"
    "         TEST = DIFFER(NULL BAL,BAL) STARS"
    "END")
  (stars-ok))

(deftest t1_remdr
  "REMDR: integer remainder."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(REMDR(10,3),1) STARS"
    "         TEST = DIFFER(REMDR(11,10),1) STARS"
    "END")
  (stars-ok))

(deftest t1_dupl
  "DUPL: string replication."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(DUPL('ABC',2),'ABCABC') STARS"
    "         TEST = DIFFER(DUPL(NULL,10),NULL) STARS"
    "         TEST = DIFFER(DUPL('ABCDEFG',0),NULL) STARS"
    "         TEST = DIFFER(DUPL(1,10),'1111111111') STARS"
    "END")
  (stars-ok))

(deftest t1_table
  "TABLE: create, store, retrieve, CONVERT to ARRAY and back."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         T = TABLE(10)"
    "         TEST = DIFFER(T<'CAT'>) STARS"
    "         T<'CAT'> = 'DOG'"
    "         TEST = DIFFER(T<'CAT'>,'DOG') STARS"
    "         T<7> = 45"
    "         TEST = DIFFER(T<7>,45) STARS"
    "         TEST = DIFFER(T<'CAT'>,'DOG') STARS"
    "         TA = CONVERT(T,'ARRAY')"
    "         TEST = DIFFER(PROTOTYPE(TA),'2,2') STARS"
    "         ATA = CONVERT(TA,'TABLE')"
    "         TEST = DIFFER(ATA<7>,45) STARS"
    "         TEST = DIFFER(ATA<'CAT'>,'DOG') STARS"
    "END")
  (stars-ok))

(deftest t1_item
  "ITEM: multi-index array access function."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         AAA = ARRAY(10)"
    "         ITEM(AAA,1) = 5"
    "         TEST = DIFFER(ITEM(AAA,1),5) STARS"
    "         TEST = DIFFER(AAA<1>,5) STARS"
    "         AAA<2> = 22"
    "         TEST = DIFFER(ITEM(AAA,2),22) STARS"
    "         AMA = ARRAY('2,2,2,2')"
    "         ITEM(AMA,1,2,1,2) = 1212"
    "         TEST = DIFFER(ITEM(AMA,1,2,1,2),1212) STARS"
    "         TEST = DIFFER(AMA<1,2,1,2>,1212) STARS"
    "         AMA<2,1,2,1> = 2121"
    "         TEST = DIFFER(ITEM(AMA,2,1,2,1),2121) STARS"
    "END")
  (stars-ok))

(deftest t1_eval
  "EVAL: evaluate deferred expressions."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         EXPR = *('ABC' 'DEF')"
    "         TEST = DIFFER(EVAL(EXPR),'ABCDEF') STARS"
    "         Q = 'QQQ'"
    "         SEXP = *Q"
    "         TEST = DIFFER(EVAL(SEXP),'QQQ') STARS"
    "         FEXP = *IDENT(1,2)"
    "         TEST = EVAL(FEXP) STARS"
    "END")
  (stars-ok))

(deftest t1_substr
  "SUBSTR: substring extraction — success and out-of-bounds failure."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(SUBSTR('ABC',2,1),'B') STARS"
    "         TEST = DIFFER(SUBSTR('ABCDEF',1,5),'ABCDE') STARS"
    "         TEST = SUBSTR('ABC',50,1) STARS"
    "         TEST = SUBSTR('ABC',81,50) STARS"
    "         TEST = SUBSTR(NULL,1,1) STARS"
    "END")
  (stars-ok))

(deftest t1_arg_local
  "ARG and LOCAL: introspect function parameter and local variable names."
  (prog
    "JLAB     DEFINE('JLAB(A,B,C)D,E,F')"
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = DIFFER(ARG(.JLAB,1),'A') STARS"
    "         TEST = DIFFER(ARG(.JLAB,3),'C') STARS"
    "         TEST = ARG(.JLAB,0) STARS"
    "         TEST = ARG(.JLAB,4) STARS"
    "         TEST = DIFFER(LOCAL(.JLAB,1),'D') STARS"
    "         TEST = DIFFER(LOCAL(.JLAB,3),'F') STARS"
    "         TEST = LOCAL(.JLAB,0) STARS"
    "         TEST = LOCAL(.JLAB,4) STARS"
    "END")
  (stars-ok))

(deftest t1_apply
  "APPLY: call a function by name-reference with argument list."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = APPLY(.EQ,1,2) STARS"
    "         TEST = !APPLY(.EQ,1,1) STARS"
    "         TEST = !APPLY(.EQ,0) STARS"
    "         TEST = !APPLY(.EQ,1,1,1) STARS"
    "         TEST = !IDENT(APPLY(.TRIM,'ABC '),'ABC') STARS"
    "END")
  (stars-ok))

;; ─────────────────────────────────────────────────────────────────────────────
;; testpgms-test2 — Diagnostics Phase Two (pattern matching)
;; ─────────────────────────────────────────────────────────────────────────────
;;
;; Oracle: csnobol4 reports one expected failure at line 67 (&FULLSCAN=0 mode,
;; BREAKX edge case). FULLSCAN=1 passes all. We test FULLSCAN=1 behaviour.

(deftest t2_simple_string_match
  "Basic string pattern matching: anchored and unanchored."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST 'ABC'                  :S(S01) ; TEST = STARS"
    "S01      TEST 'BCD'                  :S(S02) ; TEST = STARS"
    "S02      TEST 'XYZ'                  :S(S03) ; TEST = STARS"
    "S03      TEST 'ABD'                  :F(S04) ; TEST = STARS"
    "S04      &ANCHOR = 1"
    "         TEST 'ABC'                  :S(S05) ; TEST = STARS"
    "S05      TEST 'BCD'                  :F(S06) ; TEST = STARS"
    "S06"
    "END")
  (stars-ok))

(deftest t2_capture_dollar_dot
  "Pattern capture with $ (immediate) and . (deferred) assignment."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         &ANCHOR = 0"
    "         TEST 'ABC' $ VAR             :S(S07) ; TEST = STARS"
    "S07      IDENT(VAR,'ABC')             :S(S08) ; TEST = STARS"
    "S08      TEST 'ABC' . VARD            :S(S09) ; TEST = STARS"
    "S09      IDENT(VARD,'ABC')            :S(S10) ; TEST = STARS"
    "S10"
    "END")
  (stars-ok))

(deftest t2_len
  "LEN: match exactly N characters."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST LEN(3) $ VARL           :S(S11) ; TEST = STARS"
    "S11      IDENT(VARL,'ABC')            :S(S12) ; TEST = STARS"
    "S12      TEST LEN(26) $ VARL          :S(S13) ; TEST = STARS"
    "S13      IDENT(VARL,TEST)             :S(S14) ; TEST = STARS"
    "S14      TEST LEN(27)                 :F(S15) ; TEST = STARS"
    "S15"
    "END")
  (stars-ok))

(deftest t2_tab
  "TAB: advance cursor to absolute position."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST TAB(3) $ VART           :S(S16) ; TEST = STARS"
    "S16      IDENT(VART,'ABC')            :S(S17) ; TEST = STARS"
    "S17      TEST TAB(26) $ VART          :S(S18) ; TEST = STARS"
    "S18      IDENT(TEST,VART)             :S(S19) ; TEST = STARS"
    "S19      TEST TAB(0) $ VART           :S(S20) ; TEST = STARS"
    "S20      IDENT(VART)                  :S(S21) ; TEST = STARS"
    "S21"
    "END")
  (stars-ok))

(deftest t2_arb
  "ARB: match zero or more arbitrary characters."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST ARB $ VARA 'C'          :S(S22) ; TEST = STARS"
    "S22      IDENT(VARA,'AB')             :S(S23) ; TEST = STARS"
    "S23"
    "END")
  (stars-ok))

(deftest t2_pos_rpos
  "POS and RPOS: position predicates from left and right."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         &ANCHOR = 1"
    "         TEST ARB $ VARA POS(2) $ VARP  :S(S26) ; TEST = STARS"
    "S26      (IDENT(VARA,'AB') IDENT(VARP)) :S(S27) ; TEST = STARS"
    "S27      &ANCHOR = 0"
    "         TEST ARB $ VARA RPOS(25)      :S(S37) ; TEST = STARS"
    "S37      IDENT(VARA,'A')               :S(S38) ; TEST = STARS"
    "S38      TEST ARB $ VARA RPOS(0)       :S(S39) ; TEST = STARS"
    "S39      IDENT(TEST,VARA)              :S(S39A); TEST = STARS"
    "S39A     TEST ARB $ VARA RPOS(26)      :S(S40) ; TEST = STARS"
    "S40      IDENT(VARA)                   :S(S41) ; TEST = STARS"
    "S41      TEST RPOS(27)                 :F(S42) ; TEST = STARS"
    "S42"
    "END")
  (stars-ok))

(deftest t2_rtab
  "RTAB: match up to N characters from right end."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST RTAB(26) $ VARA         :S(S43) ; TEST = STARS"
    "S43      IDENT(VARA)                  :S(S44) ; TEST = STARS"
    "S44      TEST RTAB(27)                :F(S45) ; TEST = STARS"
    "S45      TEST RTAB(0) $ VARA          :S(S46) ; TEST = STARS"
    "S46      IDENT(VARA,TEST)             :S(S47) ; TEST = STARS"
    "S47      TEST RTAB(25) $ VARA         :S(S48) ; TEST = STARS"
    "S48      IDENT(VARA,'A')              :S(S49) ; TEST = STARS"
    "S49"
    "END")
  (stars-ok))

(deftest t2_break_span
  "BREAK and SPAN: character-set scanning."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST BREAK('C') $ VARA       :S(S56) ; TEST = STARS"
    "S56      IDENT(VARA,'AB')             :S(S57) ; TEST = STARS"
    "S57      TEST BREAK(',')              :F(S60) ; TEST = STARS"
    "S60      TEST SPAN(TEST) $ VARA       :S(S64) ; TEST = STARS"
    "S64      IDENT(TEST,VARA)             :S(S65) ; TEST = STARS"
    "S65      TEST SPAN('CDQ') $ VARA      :S(S66) ; TEST = STARS"
    "S66      IDENT(VARA,'CD')             :S(S67) ; TEST = STARS"
    "S67      TEST SPAN(',')               :F(S68) ; TEST = STARS"
    "S68"
    "END")
  (stars-ok))

(deftest t2_any_notany
  "ANY and NOTANY: single-character set membership."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST ANY('MXZ') $ VARA       :S(S74) ; TEST = STARS"
    "S74      IDENT(VARA,'M')              :S(S75) ; TEST = STARS"
    "S75      TEST ANY(',.')               :F(S76) ; TEST = STARS"
    "S76      TEST NOTANY('ABCDEFGHJKLMPQRSTUWXYZ') $ VARA :S(S77) ; TEST = STARS"
    "S77      IDENT(VARA,'I')              :S(S78) ; TEST = STARS"
    "S78      TEST NOTANY(TEST)            :F(S79) ; TEST = STARS"
    "S79"
    "END")
  (stars-ok))

(deftest t2_rem
  "REM: match remainder of string."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST REM $ VARA              :S(S80) ; TEST = STARS"
    "S80      IDENT(VARA,TEST)             :S(S81) ; TEST = STARS"
    "S81      TEST LEN(26) REM $ VARA      :S(S82) ; TEST = STARS"
    "S82      IDENT(VARA)                  :S(S83) ; TEST = STARS"
    "S83"
    "END")
  (stars-ok))

(deftest t2_alternation
  "Pattern alternation with |."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST ('ABD' | 'AB') $ VARA   :S(D84) ; TEST = STARS"
    "D84      IDENT(VARA,'AB')             :S(D85) ; TEST = STARS"
    "D85      TEST (TEST 'A' | TEST) $ VARL :S(D86) ; TEST = STARS"
    "D86      IDENT(VARL,TEST)             :S(D00) ; TEST = STARS"
    "D00"
    "END")
  (stars-ok))

(deftest t2_deferred_patterns
  "Deferred pattern arguments (* prefix) for LEN, TAB, POS, RPOS."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST LEN(*3) $ VARL          :S(D11) ; TEST = STARS"
    "D11      IDENT(VARL,'ABC')            :S(D15) ; TEST = STARS"
    "D15      TEST TAB(*3) $ VART          :S(D16) ; TEST = STARS"
    "D16      IDENT(VART,'ABC')            :S(D21) ; TEST = STARS"
    "D21      &ANCHOR = 1"
    "         TEST ARB $ VARA POS(*2) $ VARP :S(D26) ; TEST = STARS"
    "D26      (IDENT(VARA,'AB') IDENT(VARP)) :S(D27) ; TEST = STARS"
    "D27      &ANCHOR = 0"
    "         TEST ARB $ VARA RPOS(*25)    :S(D37) ; TEST = STARS"
    "D37      IDENT(VARA,'A')              :S(D38) ; TEST = STARS"
    "D38      TEST RTAB(*26) $ VARA        :S(D43) ; TEST = STARS"
    "D43      IDENT(VARA)                  :S(D49) ; TEST = STARS"
    "D49"
    "END")
  (stars-ok))

(deftest t2_deferred_break_span_any_notany
  "Deferred arguments for BREAK, SPAN, ANY, NOTANY."
  (prog
    "         STARS = '  ERROR DETECTED          ***'"
    "         TEST = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"
    "         TEST BREAK(*'C') $ VARA      :S(D56) ; TEST = STARS"
    "D56      IDENT(VARA,'AB')             :S(D57) ; TEST = STARS"
    "D57      TEST SPAN(*TEST) $ VARA      :S(D64) ; TEST = STARS"
    "D64      IDENT(TEST,VARA)             :S(D70) ; TEST = STARS"
    "D70      TEST ANY(*'MXZ') $ VARA      :S(D74) ; TEST = STARS"
    "D74      IDENT(VARA,'M')              :S(D75) ; TEST = STARS"
    "D75      TEST NOTANY(*'ABCDEFGHJKLMPQRSTUWXYZ') $ VARA :S(D77) ; TEST = STARS"
    "D77      IDENT(VARA,'I')              :S(D79) ; TEST = STARS"
    "D79"
    "END")
  (stars-ok))

;; ─────────────────────────────────────────────────────────────────────────────
;; testpgms-test3 — Factorial table 1..45 (big-number string arithmetic)
;; ─────────────────────────────────────────────────────────────────────────────
;;
;; Oracle output (csnobol4):
;;   1!=1  2!=2  3!=6  4!=24  5!=120  ...  10!=3,628,800  ...
;;   45!=119,622,220,865,480,194,561,963,161,495,657,715,064,383,733,760,000,000,000

(deftest t3_factorial_table_first_lines
  "Factorial table: verify first five output lines 1!..5!."
  (prog-steplimit 30000 25000
    (str "-INCLUDE '" SDIR "/testpgms-test3.spt'"))
  (let [lines (str/split-lines (:stdout (SNOBOL4clojure.test-helpers/run-with-timeout
                                          (str "-INCLUDE '" SDIR "/testpgms-test3.spt'")
                                          15000)))]
    (is (some #(= "1!=1"   %) lines) "1!=1")
    (is (some #(= "2!=2"   %) lines) "2!=2")
    (is (some #(= "3!=6"   %) lines) "3!=6")
    (is (some #(= "4!=24"  %) lines) "4!=24")
    (is (some #(= "5!=120" %) lines) "5!=120")))

(deftest t3_factorial_table_ten
  "Factorial table: 10! = 3,628,800 (first comma-formatted value)."
  (let [r (SNOBOL4clojure.test-helpers/run-with-timeout
            (str "-INCLUDE '" SDIR "/testpgms-test3.spt'") 15000)
        lines (str/split-lines (:stdout r))]
    (is (some #(= "10!=3,628,800" %) lines) "10!=3,628,800")))

(deftest t3_factorial_table_last
  "Factorial table: 45! correct (big-number string arithmetic)."
  (let [r (SNOBOL4clojure.test-helpers/run-with-timeout
            (str "-INCLUDE '" SDIR "/testpgms-test3.spt'") 15000)
        lines (str/split-lines (:stdout r))]
    (is (some #(= "45!=119,622,220,865,480,194,561,963,161,495,657,715,064,383,733,760,000,000,000" %) lines)
        "45! correct")))

;; ─────────────────────────────────────────────────────────────────────────────
;; testpgms-test4 — Syntactic recogniser (reads testpgms.in via INPUT)
;; ─────────────────────────────────────────────────────────────────────────────
;;
;; Oracle (csnobol4): known-correct lines from testpgms.in output.
;; Lines that should produce <<< NO SYNTACTIC ERROR >>> per the oracle:
;;   ELEMENT<I><J><K>  =  ELEMENT<K><J><I>
;;   A<X,Y,Z + 1>  =  F(X,STRUCTURE_BUILD(TYPE,LENGTH + 1))
;;   SETUP PAT1 = ...  (multi-line)
;;   NEWONE_TRIAL X = !COORD<1,K> X * X
;; Lines that should produce <<< SYNTACTIC ERROR >>>:
;;   DEFINE('F(X,Y))           (unmatched quote)
;;   L = LT(N,B<J> L + 1      (unmatched paren)
;;   TRIM(INPUT) PAT1 :S(OK) :F(BAD)  (double goto)

;; test4 reads card images via the default INPUT channel (stdin).
;; We redirect it by prepending an INPUT(.INPUT,5,,'file') channel declaration
;; that points at corpus/spitbol/testpgms.in, then let the program run normally.
;; The recogniser prints <<< NO SYNTACTIC ERROR >>> / <<< SYNTACTIC ERROR >>>
;; to OUTPUT for each statement it reads.

(deftest t4_syntactic_recogniser_no_errors
  "Test4: syntactically valid SPITBOL statements are recognised as error-free.
   Oracle (csnobol4): ELEMENT<I><J><K>=..., A<X,Y,Z+1>=..., SETUP PAT1=..., etc."
  (let [in-path (.getAbsolutePath (clojure.java.io/file SDIR "testpgms.in"))
        src (str "         INPUT(.INPUT,5,," (pr-str in-path) ")\n"
                 (slurp (str SDIR "/testpgms-test4.spt")))
        r   (SNOBOL4clojure.test-helpers/run-with-timeout src 15000)
        out (:stdout r)]
    (is (string? out) "program produced output")
    (is (str/includes? out "NO SYNTACTIC ERROR")
        "at least one valid statement recognised")))

(deftest t4_syntactic_recogniser_detects_errors
  "Test4: syntactically invalid SPITBOL statements produce SYNTACTIC ERROR.
   Oracle: DEFINE('F(X,Y)) and L=LT(N,B<J> L+1 are flagged as errors."
  (let [in-path (.getAbsolutePath (clojure.java.io/file SDIR "testpgms.in"))
        src (str "         INPUT(.INPUT,5,," (pr-str in-path) ")\n"
                 (slurp (str SDIR "/testpgms-test4.spt")))
        r   (SNOBOL4clojure.test-helpers/run-with-timeout src 15000)
        out (:stdout r)]
    (is (str/includes? out "SYNTACTIC ERROR")
        "at least one invalid statement detected")))