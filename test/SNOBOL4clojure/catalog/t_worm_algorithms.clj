(ns SNOBOL4clojure.catalog.t_worm_algorithms
  "Worm catalog TN — full algorithm programs.
   Bubble sort, GCD, string reversal, palindrome, binary search,
   Sieve of Eratosthenes, CSV tokenizer, Collatz sequence."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.env  :refer [array-get]]
            [SNOBOL4clojure.test-helpers :refer [prog prog-steplimit]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_worm_algorithms))) (f)))

(deftest t_bubble_sort_5
  "Bubble sort ARRAY(5) {5,3,1,4,2} -> {1,2,3,4,5}"
  (prog-steplimit 4000 1000
    "        A = ARRAY(5)"
    "        A<1> = 5"
    "        A<2> = 3"
    "        A<3> = 1"
    "        A<4> = 4"
    "        A<5> = 2"
    "        N = 5"
    "        I = 1"
    "OUTER   J = 1"
    "INNER   K = J + 1"
    "        GT(K,N) :S(NEXT_I)"
    "        GT(A<J>,A<K>) :F(SKIP)"
    "        T = A<J>"
    "        A<J> = A<K>"
    "        A<K> = T"
    "SKIP    J = J + 1"
    "        LT(J,N) :S(INNER)"
    "NEXT_I  I = I + 1"
    "        LT(I,N) :S(OUTER)"
    "end")
  (is (clojure.core/and (= 1 (SNOBOL4clojure.env/array-get ($$ 'A) [1])) (= 5 (SNOBOL4clojure.env/array-get ($$ 'A) [5])))))

(deftest t_gcd_euclid_48_18
  "GCD(48,18)=6 via Euclidean algorithm"
  (prog-steplimit 4000 500
    "        DEFINE('GCD(A,B)') :(GCDEND)"
    "GCD     EQ(B,0) :S(GCDB)"
    "        GCD = GCD(B, REMDR(A,B))  :(RETURN)"
    "GCDB    GCD = A  :(RETURN)"
    "GCDEND  R = GCD(48,18)"
    "end")
  (is (= 6 ($$ 'R))))

(deftest t_gcd_coprime
  "GCD(17,13)=1 (coprime)"
  (prog-steplimit 4000 500
    "        DEFINE('GCD(A,B)') :(GCDEND)"
    "GCD     EQ(B,0) :S(GCDB)"
    "        GCD = GCD(B, REMDR(A,B))  :(RETURN)"
    "GCDB    GCD = A  :(RETURN)"
    "GCDEND  R = GCD(17,13)"
    "end")
  (is (= 1 ($$ 'R))))

(deftest t_reverse_string_recursive
  "RREV('hello')='olleh' recursively"
  (prog-steplimit 4000 2000
    "        DEFINE('RREV(S)') :(REND)"
    "RREV    EQ(SIZE(S),0) :S(REMPTY)"
    "        RREV = RREV(SUBSTR(S,2,SIZE(S)-1)) SUBSTR(S,1,1)  :(RETURN)"
    "REMPTY  RREV = ''  :(RETURN)"
    "REND    R = RREV('hello')"
    "end")
  (is (= "olleh" ($$ 'R))))

(deftest t_reverse_string_loop
  "Reverse 'abcde' via loop -> 'edcba'"
  (prog-steplimit 2000 100
    "        S = 'abcde'"
    "        R = ''"
    "        I = SIZE(S)"
    "LOOP    R = R SUBSTR(S,I,1)"
    "        I = I - 1"
    "        GT(I,0) :S(LOOP)"
    "end")
  (is (= "edcba" ($$ 'R))))

(deftest t_palindrome_check_true
  "'racecar' is a palindrome -> R='yes'"
  (prog-steplimit 4000 2000
    "        DEFINE('RREV(S)') :(REND)"
    "RREV    EQ(SIZE(S),0) :S(REMPTY)"
    "        RREV = RREV(SUBSTR(S,2,SIZE(S)-1)) SUBSTR(S,1,1)  :(RETURN)"
    "REMPTY  RREV = ''  :(RETURN)"
    "REND"
    "        S = 'racecar'"
    "        R = 'no'"
    "        IDENT(S,RREV(S)) :S(PAL)"
    "        :(DONE)"
    "PAL     R = 'yes'"
    "DONE    end")
  (is (= "yes" ($$ 'R))))

(deftest t_palindrome_check_false
  "'hello' is not a palindrome -> R='no'"
  (prog-steplimit 4000 2000
    "        DEFINE('RREV(S)') :(REND)"
    "RREV    EQ(SIZE(S),0) :S(REMPTY)"
    "        RREV = RREV(SUBSTR(S,2,SIZE(S)-1)) SUBSTR(S,1,1)  :(RETURN)"
    "REMPTY  RREV = ''  :(RETURN)"
    "REND"
    "        S = 'hello'"
    "        R = 'no'"
    "        IDENT(S,RREV(S)) :S(PAL)"
    "        :(DONE)"
    "PAL     R = 'yes'"
    "DONE    end")
  (is (= "no" ($$ 'R))))

(deftest t_binary_search_found
  "Binary search sorted array {1,3,5,7,9} for 7 -> pos=4"
  (prog-steplimit 2000 200
    "        A = ARRAY(5)"
    "        A<1> = 1"
    "        A<2> = 3"
    "        A<3> = 5"
    "        A<4> = 7"
    "        A<5> = 9"
    "        TARGET = 7"
    "        LO = 1"
    "        HI = 5"
    "        POS = -1"
    "BLOOP   GT(LO,HI) :S(BDONE)"
    "        MID = (LO + HI) / 2"
    "        EQ(A<MID>,TARGET) :S(BFOUND)"
    "        LT(A<MID>,TARGET) :S(BHIGH)"
    "        HI = MID - 1"
    "        :(BLOOP)"
    "BHIGH   LO = MID + 1"
    "        :(BLOOP)"
    "BFOUND  POS = MID"
    "BDONE   end")
  (is (= 4 ($$ 'POS))))

(deftest t_binary_search_not_found
  "Binary search {1,3,5,7,9} for 4 -> pos=-1"
  (prog-steplimit 2000 200
    "        A = ARRAY(5)"
    "        A<1> = 1"
    "        A<2> = 3"
    "        A<3> = 5"
    "        A<4> = 7"
    "        A<5> = 9"
    "        TARGET = 4"
    "        LO = 1"
    "        HI = 5"
    "        POS = -1"
    "BLOOP   GT(LO,HI) :S(BDONE)"
    "        MID = (LO + HI) / 2"
    "        EQ(A<MID>,TARGET) :S(BFOUND)"
    "        LT(A<MID>,TARGET) :S(BHIGH)"
    "        HI = MID - 1"
    "        :(BLOOP)"
    "BHIGH   LO = MID + 1"
    "        :(BLOOP)"
    "BFOUND  POS = MID"
    "BDONE   end")
  (is (= -1 ($$ 'POS))))

(deftest t_sieve_primes_to_20
  "Sieve to 20: primes = 2,3,5,7,11,13,17,19 -> count=8"
  (prog-steplimit 4000 2000
    "        N = 20"
    "        A = ARRAY(20)"
    "        I = 2"
    "INIT    A<I> = 1"
    "        I = I + 1"
    "        LE(I,N) :S(INIT)"
    "        I = 2"
    "OUTER   EQ(A<I>,0) :S(SKIP_I)"
    "        J = I * 2"
    "INNER   GT(J,N) :S(SKIP_I)"
    "        A<J> = 0"
    "        J = J + I"
    "        :(INNER)"
    "SKIP_I  I = I + 1"
    "        LE(I,N) :S(OUTER)"
    "        CNT = 0"
    "        I = 2"
    "COUNT   EQ(A<I>,1) :F(CNT_SKIP)"
    "        CNT = CNT + 1"
    "CNT_SKIP I = I + 1"
    "        LE(I,N) :S(COUNT)"
    "end")
  (is (= 8 ($$ 'CNT))))

(deftest t_tokenize_csv
  "Split 'a,b,c' on comma -> T1=a T2=b T3=c"
  (prog-steplimit 2000 500
    "        S = 'a,b,c,'"
    "        I = 1"
    "        N = 0"
    "LOOP    S BREAK(',') . TOK LEN(1)  :F(DONE)"
    "        N = N + 1"
    "        EQ(N,1) :S(SET1)"
    "        EQ(N,2) :S(SET2)"
    "        T3 = TOK"
    "        :(SKIP)"
    "SET1    T1 = TOK"
    "        :(SKIP)"
    "SET2    T2 = TOK"
    "SKIP    S = SUBSTR(S, SIZE(TOK) + 2, SIZE(S) - SIZE(TOK) - 1)"
    "        GT(SIZE(S),0) :S(LOOP)"
    "DONE    end")
  (is (clojure.core/and (= "a" ($$ 'T1)) (= "b" ($$ 'T2)) (= "c" ($$ 'T3)))))

(deftest t_collatz_steps_6
  "Collatz sequence from 6: 6->3->10->5->16->8->4->2->1; steps=8"
  (prog-steplimit 2000 200
    "        N = 6"
    "        STEPS = 0"
    "LOOP    EQ(N,1) :S(DONE)"
    "        STEPS = STEPS + 1"
    "        EQ(REMDR(N,2),0) :S(EVEN)"
    "        N = 3 * N + 1"
    "        :(LOOP)"
    "EVEN    N = N / 2"
    "        :(LOOP)"
    "DONE    end")
  (is (= 8 ($$ 'STEPS))))

