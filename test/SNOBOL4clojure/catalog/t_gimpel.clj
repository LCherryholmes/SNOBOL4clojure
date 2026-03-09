(ns SNOBOL4clojure.catalog.t_gimpel
  "Sprint 15 — Gimpel corpus tests.

   Source material: *Algorithms in SNOBOL4* by James F. Gimpel.
     (c) 1976 Bell Telephone Laboratories, Inc.
     (c) 1986, 1998 Catspaw, Inc.
   Program material provided courtesy of Dr. James F. Gimpel.
   Distributed by Catspaw, Inc., Salida CO — http://www.SNOBOL4.com

   Every test -INCLUDEs the relevant .INC file(s) from corpus/gimpel/SNOBOL4
   (the verbatim Gimpel distribution, committed as a corpus in this repo).
   Tests verify the output of each algorithm against known-correct values,
   providing confidence that SNOBOL4clojure faithfully executes the
   original Gimpel code."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-include prog-steplimit]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_gimpel))) (f)))

(def ^:private GDIR "corpus/gimpel/SNOBOL4")

;; ── Chapter 2: Conversions ────────────────────────────────────────────────────

(deftest gimpel_uplo_swap_case
  "UPLO (ch.2.1) swaps upper↔lower case via REPLACE."
  (prog-include [GDIR]
    "-INCLUDE 'UPLO.INC'
        R = UPLO('Hello World')
     END")
  (is (= "hELLO wORLD" ($$ 'R))))

(deftest gimpel_roman_single_digits
  "ROMAN (ch.2.3) Arabic to Roman — single-digit values."
  (prog-include [GDIR]
    "-INCLUDE 'ROMAN.INC'
        R1 = ROMAN(1)
        R4 = ROMAN(4)
        R9 = ROMAN(9)
     END")
  (is (= "I"  ($$ 'R1)))
  (is (= "IV" ($$ 'R4)))
  (is (= "IX" ($$ 'R9))))

(deftest gimpel_roman_larger_values
  "ROMAN (ch.2.3) — larger values including MCMXCIX."
  (prog-include [GDIR]
    "-INCLUDE 'ROMAN.INC'
        R14   = ROMAN(14)
        R40   = ROMAN(40)
        R1999 = ROMAN(1999)
     END")
  (is (= "XIV"     ($$ 'R14)))
  (is (= "XL"      ($$ 'R40)))
  (is (= "MCMXCIX" ($$ 'R1999))))

(deftest gimpel_baseb_base2
  "BASEB (ch.2.4) decimal→binary."
  (prog-include [GDIR]
    "-INCLUDE 'BASEB.INC'
        B2  = BASEB(10,2)
        B16 = BASEB(255,16)
     END")
  (is (= "1010" ($$ 'B2)))
  (is (= "FF"   ($$ 'B16))))

(deftest gimpel_base10_from_binary
  "BASE10 (ch.2.5) binary→decimal."
  (prog-include [GDIR]
    "-INCLUDE 'BASE10.INC'
        D1 = BASE10('1010',2)
        D2 = BASE10('FF',16)
     END")
  (is (= 10  ($$ 'D1)))
  (is (= 255 ($$ 'D2))))

(deftest gimpel_spell_numbers
  "SPELL (ch.2.10) spells out integers in English."
  (prog-include [GDIR]
    "-INCLUDE 'SPELL.INC'
        S1  = SPELL(1)
        S13 = SPELL(13)
        S42 = SPELL(42)
     END")
  (is (= "ONE"        ($$ 'S1)))
  (is (= "THIRTEEN"   ($$ 'S13)))
  (is (= "FORTY-TWO"  ($$ 'S42))))

;; ── Chapter 3: Basic String Functions ────────────────────────────────────────

(deftest gimpel_order_sort_chars
  "ORDER (ch.3.1) puts characters of a string into alphabetic order."
  (prog-include [GDIR]
    "-INCLUDE 'ORDER.INC'
        R = ORDER('DCBA')
     END")
  (is (= "ABCD" ($$ 'R))))

(deftest gimpel_rotater_right
  "ROTATER (ch.3.5) rotates string right by N."
  (prog-include [GDIR]
    "-INCLUDE 'ROTATER.INC'
        R1 = ROTATER('ABCDE',2)
        R2 = ROTATER('ABCDE',0)
     END")
  (is (= "DEABC" ($$ 'R1)))
  (is (= "ABCDE" ($$ 'R2))))

(deftest gimpel_count_substring
  "COUNT (ch.3.4) counts occurrences of a substring."
  (prog-include [GDIR]
    "-INCLUDE 'COUNT.INC'
        C1 = COUNT('MISSISSIPPI','SS')
        C2 = COUNT('ABCABC','ABC')
        C3 = COUNT('ABCABC','X')
     END")
  (is (= 2 ($$ 'C1)))
  (is (= 2 ($$ 'C2)))
  (is (= 0 ($$ 'C3))))

(deftest gimpel_diff_set_difference
  "DIFF (ch.3.10) set difference of character sets."
  (prog-include [GDIR]
    "-INCLUDE 'DIFF.INC'
        R = DIFF('ABCDE','BD')
     END")
  (is (= "ACE" ($$ 'R))))

(deftest gimpel_skim_unique_chars
  "SKIM (ch.3.11) skims first occurrence of each distinct character."
  (prog-include [GDIR]
    "-INCLUDE 'SKIM.INC'
        R = SKIM('MISSISSIPPI')
     END")
  (is (= "MISP" ($$ 'R))))

(deftest gimpel_agt_alphabetic_compare
  "AGT (ch.3.13) alphabetic comparison, case-insensitive."
  (prog-include [GDIR]
    "-INCLUDE 'AGT.INC'
        AGT('Z','a')     :S(YES1)F(NO1)
 YES1   R1 = 'yes'       :(DONE1)
 NO1    R1 = 'no'
 DONE1  AGT('apple','BANANA')  :S(YES2)F(NO2)
 YES2   R2 = 'yes'       :(DONE2)
 NO2    R2 = 'no'
 DONE2
     END")
  (is (= "yes" ($$ 'R1)))   ;; Z > a alphabetically
  (is (= "no"  ($$ 'R2))))  ;; apple < BANANA alphabetically

(deftest gimpel_swap_values
  "SWAP (ch.3.14) swaps named variables."
  (prog-include [GDIR]
    "-INCLUDE 'SWAP.INC'
        A = 'hello'
        B = 'world'
        SWAP(.A,.B)
     END")
  (is (= "world" ($$ 'A)))
  (is (= "hello" ($$ 'B))))

(deftest gimpel_repl_string_replace
  "REPL (ch.3.15) string-by-string replacement (vs character REPLACE)."
  (prog-include [GDIR]
    "-INCLUDE 'REPL.INC'
        R = REPL('abcabc','bc','XY')
     END")
  (is (= "aXYaXY" ($$ 'R))))

(deftest gimpel_quote_snobol4_quoting
  "QUOTE (ch.3.16) wraps a string in SNOBOL4 quote syntax."
  (prog-include [GDIR]
    "-INCLUDE 'QUOTE.INC'
        R = QUOTE('hello')
     END")
  (is (= "'hello'" ($$ 'R))))

;; ── Chapter 4: Basic Array Functions ─────────────────────────────────────────

(deftest gimpel_crack_string_to_array
  "CRACK (ch.4.1) converts a delimited string to an array."
  (prog-include [GDIR]
    "-INCLUDE 'CRACK.INC'
        A = CRACK('A,B,C',',')
        E1 = A<1>
        E2 = A<2>
        E3 = A<3>
     END")
  (is (= "A" ($$ 'E1)))
  (is (= "B" ($$ 'E2)))
  (is (= "C" ($$ 'E3))))

;; ── Chapter 12: Permutations ──────────────────────────────────────────────────

(deftest gimpel_comb_combinations
  "COMB (ch.15.1) combinations: C(n,m)."
  (prog-include [GDIR]
    "-INCLUDE 'COMB.INC'
        C1 = COMB(5,0)
        C2 = COMB(5,1)
        C3 = COMB(5,2)
        C4 = COMB(10,3)
     END")
  (is (= 1   ($$ 'C1)))
  (is (= 5   ($$ 'C2)))
  (is (= 10  ($$ 'C3)))
  (is (= 120 ($$ 'C4))))

(deftest gimpel_permutation_ith
  "PERMUTATION (ch.12.1) returns the Ith permutation of a string."
  (prog-include [GDIR]
    "-INCLUDE 'PERMUTAT.INC'
        P0 = PERMUTATION('ABC',0)
        P1 = PERMUTATION('ABC',1)
        P2 = PERMUTATION('ABC',2)
     END")
  (is (= "ABC" ($$ 'P0)))
  (is (= "BAC" ($$ 'P1)))
  (is (= "CAB" ($$ 'P2))))

;; ── Chapter 13: Sorting ───────────────────────────────────────────────────────

(deftest gimpel_bsort_bubble_sort
  "BSORT (ch.13.1) bubble-sorts an array in ascending lexical order."
  (prog-steplimit 5000 2000
    (str "-INCLUDE '" GDIR "/BSORT.INC'\n"
         "        A = ARRAY(5)\n"
         "        A<1> = 'dog'\n"
         "        A<2> = 'cat'\n"
         "        A<3> = 'ant'\n"
         "        A<4> = 'bee'\n"
         "        A<5> = 'emu'\n"
         "        BSORT(A,1,5)\n"
         "     END"))
  (is (= "ant" (SNOBOL4clojure.env/array-get ($$ 'A) [1])))
  (is (= "dog" (SNOBOL4clojure.env/array-get ($$ 'A) [5]))))

(deftest gimpel_hsort_quicksort
  "HSORT (ch.13.2) Hoare quicksort — depends on SWAP."
  (prog-steplimit 5000 2000
    (str "-INCLUDE '" GDIR "/HSORT.INC'\n"
         "        A = ARRAY(6)\n"
         "        A<1> = 'fox'\n"
         "        A<2> = 'ant'\n"
         "        A<3> = 'zoo'\n"
         "        A<4> = 'bee'\n"
         "        A<5> = 'cat'\n"
         "        A<6> = 'dog'\n"
         "        HSORT(A,1,6)\n"
         "     END"))
  (is (= "ant" (SNOBOL4clojure.env/array-get ($$ 'A) [1])))
  (is (= "zoo" (SNOBOL4clojure.env/array-get ($$ 'A) [6]))))

;; ── Chapter 15: Numbers ───────────────────────────────────────────────────────

(deftest gimpel_floor_function
  "FLOOR (ch.15.4) largest integer not greater than X."
  (prog-include [GDIR]
    "-INCLUDE 'FLOOR.INC'
        F1 = FLOOR(3.7)
        F2 = FLOOR(-2.3)
        F3 = FLOOR(5.0)
     END")
  (is (= 3  ($$ 'F1)))
  (is (= -3 ($$ 'F2)))
  (is (= 5  ($$ 'F3))))

(deftest gimpel_sqrt_function
  "SQRT (ch.15.6) square root via Newton's method.
   NOTE: SQRT uses internal locals T, ERR, SLOPE, Y — avoid naming our
   result vars to clash.  Use RSQRT4, RSQRT9, RSQRT2."
  (prog-include [GDIR]
    "-INCLUDE 'SQRT.INC'
        RSQRT4 = SQRT(4.0)
        RSQRT9 = SQRT(9.0)
        RSQRT2 = SQRT(2.0)
     END")
  (is (= 2.0 (double ($$ 'RSQRT4))))
  (is (= 3.0 (double ($$ 'RSQRT9))))
  ;; sqrt(2) ≈ 1.41421…
  (is (< 1.414 (double ($$ 'RSQRT2)) 1.415)))

(deftest gimpel_decomb_combinatorial_number
  "DECOMB (ch.15.2) combinatorial number system decoding."
  (prog-include [GDIR]
    "-INCLUDE 'DECOMB.INC'
        D0 = DECOMB('0')
        D1 = DECOMB('1')
        DA = DECOMB('A')
     END")
  ;; DECOMB('0') = COMB(0,1) = 1? Let's verify 0-indexed positions
  ;; COMB_ALPHA = '0123456789ABC...' so '0'->K=0 COMB(0,1)=0; '1'->K=1 COMB(1,1)=1
  (is (= 0 ($$ 'D0)))
  (is (= 1 ($$ 'D1))))

;; ── Chapter 16: Stochastic Strings ───────────────────────────────────────────

(deftest gimpel_random_in_range
  "RANDOM (ch.16.1) returns integer in [1..N].
   Uses RANDOM(N) from Gimpel — returns a SNOBOL4 number in [1..N].
   We verify the result is numeric and in range."
  (prog-include [GDIR]
    "-INCLUDE 'RANDOM.INC'
        R1 = RANDOM(10)
        R2 = RANDOM(100)
     END")
  (let [r1-raw ($$ 'R1)
        r2-raw ($$ 'R2)
        r1 (if (number? r1-raw) r1-raw (Long/parseLong (str r1-raw)))
        r2 (if (number? r2-raw) r2-raw (Long/parseLong (str r2-raw)))]
    (is (>= r1 1))
    (is (<= r1 10))
    (is (>= r2 1))
    (is (<= r2 100))))
