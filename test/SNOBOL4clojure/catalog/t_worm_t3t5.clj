(ns SNOBOL4clojure.catalog.t_worm_t3t5
  "Worm catalog bands T3-T5 — generated 2026-03-08.
   T3: conditional logic, classification, loops, nested loops.
   T4: DEFINE/CALL, FRETURN, locals, chains, call-in-loop.
   T5: recursion (fact/fib/pow/sum), TABLE and ARRAY algorithms."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.env  :refer [table-get array-get]]
            [SNOBOL4clojure.test-helpers :refer [prog prog-steplimit]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_worm_t3t5))) (f)))

(deftest t_cond_and_both_true
  "GT(I,0) AND GT(J,0) both pass -> R=yes"
  (prog
    "        I = 5"
    "        J = 3"
    "        R = 'no'"
    "        GT(I,0) :F(DONE)"
    "        GT(J,0) :F(DONE)"
    "        R = 'yes'"
    "DONE    end")
  (is (= "yes" ($$ 'R))))

(deftest t_cond_and_first_false
  "GT(I,0) fails (I=0) -> R stays no"
  (prog
    "        I = 0"
    "        J = 3"
    "        R = 'no'"
    "        GT(I,0) :F(DONE)"
    "        GT(J,0) :F(DONE)"
    "        R = 'yes'"
    "DONE    end")
  (is (= "no" ($$ 'R))))

(deftest t_cond_or_first_true
  "EQ(I,1) S branch taken on first arm"
  (prog
    "        I = 1"
    "        R = 'no'"
    "        EQ(I,1) :S(HIT)"
    "        EQ(I,2) :S(HIT)"
    "        :(DONE)"
    "HIT     R = 'yes'"
    "DONE    end")
  (is (= "yes" ($$ 'R))))

(deftest t_cond_or_second_true
  "EQ(I,2) S branch taken on second arm"
  (prog
    "        I = 2"
    "        R = 'no'"
    "        EQ(I,1) :S(HIT)"
    "        EQ(I,2) :S(HIT)"
    "        :(DONE)"
    "HIT     R = 'yes'"
    "DONE    end")
  (is (= "yes" ($$ 'R))))

(deftest t_cond_or_neither
  "Neither EQ(9,1) nor EQ(9,2) -> R stays no"
  (prog
    "        I = 9"
    "        R = 'no'"
    "        EQ(I,1) :S(HIT)"
    "        EQ(I,2) :S(HIT)"
    "        :(DONE)"
    "HIT     R = 'yes'"
    "DONE    end")
  (is (= "no" ($$ 'R))))

(deftest t_classify_positive
  "GT(7,0) -> CLASS=positive"
  (prog
    "        I = 7"
    "        CLASS = 'zero'"
    "        GT(I,0) :S(POS)"
    "        LT(I,0) :S(NEG)"
    "        :(DONE)"
    "POS     CLASS = 'positive'"
    "        :(DONE)"
    "NEG     CLASS = 'negative'"
    "DONE    end")
  (is (= "positive" ($$ 'CLASS))))

(deftest t_classify_negative
  "LT(-3,0) -> CLASS=negative"
  (prog
    "        I = -3"
    "        CLASS = 'zero'"
    "        GT(I,0) :S(POS)"
    "        LT(I,0) :S(NEG)"
    "        :(DONE)"
    "POS     CLASS = 'positive'"
    "        :(DONE)"
    "NEG     CLASS = 'negative'"
    "DONE    end")
  (is (= "negative" ($$ 'CLASS))))

(deftest t_classify_zero
  "I=0 -> neither branch -> CLASS stays zero"
  (prog
    "        I = 0"
    "        CLASS = 'zero'"
    "        GT(I,0) :S(POS)"
    "        LT(I,0) :S(NEG)"
    "        :(DONE)"
    "POS     CLASS = 'positive'"
    "        :(DONE)"
    "NEG     CLASS = 'negative'"
    "DONE    end")
  (is (= "zero" ($$ 'CLASS))))

(deftest t_max_a_gt_b
  "max(8,3)=8"
  (prog
    "        A = 8"
    "        B = 3"
    "        M = B"
    "        GT(A,B) :F(DONE)"
    "        M = A"
    "DONE    end")
  (is (= 8 ($$ 'M))))

(deftest t_max_b_gt_a
  "max(3,8)=8"
  (prog
    "        A = 3"
    "        B = 8"
    "        M = B"
    "        GT(A,B) :F(DONE)"
    "        M = A"
    "DONE    end")
  (is (= 8 ($$ 'M))))

(deftest t_abs_positive
  "abs(5)=5"
  (prog
    "        I = 5"
    "        A = I"
    "        GE(I,0) :S(DONE)"
    "        A = 0 - I"
    "DONE    end")
  (is (= 5 ($$ 'A))))

(deftest t_abs_negative
  "abs(-7)=7"
  (prog
    "        I = -7"
    "        A = I"
    "        GE(I,0) :S(DONE)"
    "        A = 0 - I"
    "DONE    end")
  (is (= 7 ($$ 'A))))

(deftest t_str_cond_match_found
  "'ell' found in 'hello' -> S branch -> R=yes"
  (prog
    "        S = 'hello'"
    "        R = 'no'"
    "        S 'ell' :S(HIT)"
    "        :(DONE)"
    "HIT     R = 'yes'"
    "DONE    end")
  (is (= "yes" ($$ 'R))))

(deftest t_str_cond_match_not_found
  "'xyz' not in 'hello' -> F branch -> R=missed"
  (prog
    "        S = 'hello'"
    "        R = 'no'"
    "        S 'xyz' :F(MISS)"
    "        :(DONE)"
    "MISS    R = 'missed'"
    "DONE    end")
  (is (= "missed" ($$ 'R))))

(deftest t_str_size_gt_3
  "SIZE('hello')=5 > 3 -> R=long"
  (prog
    "        S = 'hello'"
    "        R = 'short'"
    "        GT(SIZE(S),3) :S(LONG)"
    "        :(DONE)"
    "LONG    R = 'long'"
    "DONE    end")
  (is (= "long" ($$ 'R))))

(deftest t_sum_1_to_5
  "1+2+3+4+5=15"
  (prog-steplimit 2000 50
    "        I = 1"
    "        S = 0"
    "LOOP    S = S + I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (= 15 ($$ 'S))))

(deftest t_product_1_to_5
  "5! via loop = 120"
  (prog-steplimit 2000 100
    "        I = 1"
    "        P = 1"
    "LOOP    P = P * I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (= 120 ($$ 'P))))

(deftest t_countdown_to_zero
  "I=5 countdown; final I=0"
  (prog-steplimit 2000 50
    "        I = 5"
    "LOOP    I = I - 1"
    "        GT(I,0) :S(LOOP)"
    "end")
  (is (= 0 ($$ 'I))))

(deftest t_fibonacci_7
  "fib(7)=13 via loop"
  (prog-steplimit 2000 100
    "        A = 0"
    "        B = 1"
    "        I = 2"
    "LOOP    C = A + B"
    "        A = B"
    "        B = C"
    "        I = I + 1"
    "        LE(I,7) :S(LOOP)"
    "end")
  (is (= 13 ($$ 'B))))

(deftest t_string_build_loop
  "concatenate 'x' 5 times -> xxxxx"
  (prog-steplimit 2000 50
    "        S = ''"
    "        I = 1"
    "LOOP    S = S 'x'"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (= "xxxxx" ($$ 'S))))

(deftest t_nested_loop_3x4
  "3*4 via nested add-loop = 12"
  (prog-steplimit 2000 200
    "        A = 3"
    "        B = 4"
    "        P = 0"
    "        I = 1"
    "OUTER   J = 1"
    "INNER   P = P + 1"
    "        J = J + 1"
    "        LE(J,B) :S(INNER)"
    "        I = I + 1"
    "        LE(I,A) :S(OUTER)"
    "end")
  (is (= 12 ($$ 'P))))

(deftest t_loop_sum_evens
  "sum of even numbers 2..10 = 30"
  (prog-steplimit 2000 150
    "        I = 1"
    "        S = 0"
    "LOOP    EQ(REMDR(I,2),0) :F(SKIP)"
    "        S = S + I"
    "SKIP    I = I + 1"
    "        LE(I,10) :S(LOOP)"
    "end")
  (is (= 30 ($$ 'S))))

(deftest t_define_square
  "SQUARE(X)=X*X; SQUARE(7)=49"
  (prog
    "        DEFINE('SQUARE(X)') :(SQEND)"
    "SQUARE  SQUARE = X * X  :(RETURN)"
    "SQEND   R = SQUARE(7)"
    "end")
  (is (= 49 ($$ 'R))))

(deftest t_define_cube
  "CUBE(X)=X*X*X; CUBE(3)=27"
  (prog
    "        DEFINE('CUBE(X)') :(CUEND)"
    "CUBE    CUBE = X * X * X  :(RETURN)"
    "CUEND   R = CUBE(3)"
    "end")
  (is (= 27 ($$ 'R))))

(deftest t_define_add
  "ADD(A,B)=A+B; ADD(11,22)=33"
  (prog
    "        DEFINE('ADD(A,B)') :(ADDEND)"
    "ADD     ADD = A + B  :(RETURN)"
    "ADDEND  R = ADD(11,22)"
    "end")
  (is (= 33 ($$ 'R))))

(deftest t_define_max2_first_wins
  "MAX2(A,B): MAX2(9,4)=9"
  (prog
    "        DEFINE('MAX2(A,B)') :(MXEND)"
    "MAX2    MAX2 = B"
    "        GT(A,B) :F(MAX2R)"
    "        MAX2 = A"
    "MAX2R   :(RETURN)"
    "MXEND   R = MAX2(9,4)"
    "end")
  (is (= 9 ($$ 'R))))

(deftest t_define_max2_second_wins
  "MAX2(A,B): MAX2(3,7)=7"
  (prog
    "        DEFINE('MAX2(A,B)') :(MXEND)"
    "MAX2    MAX2 = B"
    "        GT(A,B) :F(MAX2R)"
    "        MAX2 = A"
    "MAX2R   :(RETURN)"
    "MXEND   R = MAX2(3,7)"
    "end")
  (is (= 7 ($$ 'R))))

(deftest t_define_freturn_on_zero
  "SAFE_DIV(10,0) FRETURNs; caller R stays -1"
  (prog
    "        DEFINE('SAFE_DIV(A,B)') :(SDEND)"
    "SAFE_DIV EQ(B,0) :S(SDFAIL)"
    "         SAFE_DIV = A / B  :(RETURN)"
    "SDFAIL   :(FRETURN)"
    "SDEND"
    "        R = -1"
    "        R = SAFE_DIV(10,0)  :F(DONE)"
    "        R = 999"
    "DONE    end")
  (is (= -1 ($$ 'R))))

(deftest t_define_freturn_success
  "SAFE_DIV(10,2)=5"
  (prog
    "        DEFINE('SAFE_DIV(A,B)') :(SDEND)"
    "SAFE_DIV EQ(B,0) :S(SDFAIL)"
    "         SAFE_DIV = A / B  :(RETURN)"
    "SDFAIL   :(FRETURN)"
    "SDEND"
    "        R = SAFE_DIV(10,2)"
    "end")
  (is (= 5 ($$ 'R))))

(deftest t_define_with_local
  "DOUBLE(N) uses local T; DOUBLE(6)=12"
  (prog
    "        DEFINE('DOUBLE(N),T') :(DBLEND)"
    "DOUBLE  T = N * 2"
    "        DOUBLE = T  :(RETURN)"
    "DBLEND  R = DOUBLE(6)"
    "end")
  (is (= 12 ($$ 'R))))

(deftest t_define_string_fn
  "GREET(S)='Hello, ' S; GREET('world')='Hello, world'"
  (prog
    "        DEFINE('GREET(S)') :(GREND)"
    "GREET   GREET = 'Hello, ' S  :(RETURN)"
    "GREND   R = GREET('world')"
    "end")
  (is (= "Hello, world" ($$ 'R))))

(deftest t_define_chain
  "G(X)=X+1; F(X)=G(X)+1; F(3)=5"
  (prog
    "        DEFINE('G(X)') :(GEND)"
    "G       G = X + 1  :(RETURN)"
    "GEND"
    "        DEFINE('F(X)') :(FEND)"
    "F       F = G(X) + 1  :(RETURN)"
    "FEND    R = F(3)"
    "end")
  (is (= 5 ($$ 'R))))

(deftest t_define_call_in_loop
  "sum of SQ(1..4) = 1+4+9+16 = 30"
  (prog-steplimit 2000 200
    "        DEFINE('SQ(X)') :(SQEND)"
    "SQ      SQ = X * X  :(RETURN)"
    "SQEND"
    "        S = 0"
    "        I = 1"
    "LOOP    S = S + SQ(I)"
    "        I = I + 1"
    "        LE(I,4) :S(LOOP)"
    "end")
  (is (= 30 ($$ 'S))))

(deftest t_define_two_calls
  "ADD(3,4)+ADD(5,6)=18"
  (prog
    "        DEFINE('ADD(A,B)') :(ADDEND)"
    "ADD     ADD = A + B  :(RETURN)"
    "ADDEND  R = ADD(3,4) + ADD(5,6)"
    "end")
  (is (= 18 ($$ 'R))))

(deftest t_recursive_fact_0
  "fact(0)=1 base case"
  (prog
    "        DEFINE('FACT(N)') :(FEND)"
    "FACT    EQ(N,0) :S(BASE)"
    "        FACT = N * FACT(N - 1)  :(RETURN)"
    "BASE    FACT = 1  :(RETURN)"
    "FEND    R = FACT(0)"
    "end")
  (is (= 1 ($$ 'R))))

(deftest t_recursive_fact_5
  "fact(5)=120"
  (prog-steplimit 4000 1000
    "        DEFINE('FACT(N)') :(FEND)"
    "FACT    EQ(N,0) :S(BASE)"
    "        FACT = N * FACT(N - 1)  :(RETURN)"
    "BASE    FACT = 1  :(RETURN)"
    "FEND    R = FACT(5)"
    "end")
  (is (= 120 ($$ 'R))))

(deftest t_recursive_sum_5
  "rsum(5)=15"
  (prog-steplimit 4000 500
    "        DEFINE('RSUM(N)') :(RSEND)"
    "RSUM    EQ(N,0) :S(RSBASE)"
    "        RSUM = N + RSUM(N - 1)  :(RETURN)"
    "RSBASE  RSUM = 0  :(RETURN)"
    "RSEND   R = RSUM(5)"
    "end")
  (is (= 15 ($$ 'R))))

(deftest t_recursive_power_2_10
  "pow(2,10)=1024"
  (prog-steplimit 4000 2000
    "        DEFINE('POW(B,E)') :(POWEND)"
    "POW     EQ(E,0) :S(POWB)"
    "        POW = B * POW(B,E - 1)  :(RETURN)"
    "POWB    POW = 1  :(RETURN)"
    "POWEND  R = POW(2,10)"
    "end")
  (is (= 1024 ($$ 'R))))

(deftest t_recursive_fib_8
  "fib(8)=21"
  (prog-steplimit 8000 5000
    "        DEFINE('FIB(N)') :(FIBEND)"
    "FIB     LE(N,1) :S(FIBB)"
    "        FIB = FIB(N - 1) + FIB(N - 2)  :(RETURN)"
    "FIBB    FIB = N  :(RETURN)"
    "FIBEND  R = FIB(8)"
    "end")
  (is (= 21 ($$ 'R))))

(deftest t_table_char_freq
  "char freq in 'aababc': a=3 b=2 c=1"
  (prog-steplimit 2000 200
    "        T = TABLE()"
    "        S = 'aababc'"
    "        I = 1"
    "LOOP    C = SUBSTR(S,I,1)"
    "        T<C> = T<C> + 1"
    "        I = I + 1"
    "        LE(I,SIZE(S)) :S(LOOP)"
    "end")
  (is (clojure.core/and (= 3 (table-get ($$ 'T) "a")) (= 2 (table-get ($$ 'T) "b")) (= 1 (table-get ($$ 'T) "c")))))

(deftest t_table_membership
  "TABLE as set: key present after insert = 1"
  (prog
    "        T = TABLE()"
    "        T<'apple'> = 1"
    "        T<'banana'> = 1"
    "        R = T<'apple'>"
    "end")
  (is (= 1 ($$ 'R))))

(deftest t_table_missing_key_auto_zero
  "T<missing> + 1 = 1 (default 0)"
  (prog
    "        T = TABLE()"
    "        T<'x'> = T<'x'> + 1"
    "        R = T<'x'>"
    "end")
  (is (= 1 ($$ 'R))))

(deftest t_table_overwrite
  "T<k>=10 then T<k>=99; final=99"
  (prog
    "        T = TABLE()"
    "        T<'k'> = 10"
    "        T<'k'> = 99"
    "        R = T<'k'>"
    "end")
  (is (= 99 ($$ 'R))))

(deftest t_array_sum_5
  "ARRAY(5) sum {1,2,3,4,5}=15"
  (prog-steplimit 2000 100
    "        A = ARRAY(5)"
    "        A<1> = 1"
    "        A<2> = 2"
    "        A<3> = 3"
    "        A<4> = 4"
    "        A<5> = 5"
    "        S = 0"
    "        I = 1"
    "LOOP    S = S + A<I>"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (= 15 ($$ 'S))))

(deftest t_array_max_element
  "max of {3,1,4,1,5,9,2,6}=9"
  (prog-steplimit 2000 200
    "        A = ARRAY(8)"
    "        A<1> = 3"
    "        A<2> = 1"
    "        A<3> = 4"
    "        A<4> = 1"
    "        A<5> = 5"
    "        A<6> = 9"
    "        A<7> = 2"
    "        A<8> = 6"
    "        M = A<1>"
    "        I = 2"
    "LOOP    GT(A<I>,M) :F(SKIP)"
    "        M = A<I>"
    "SKIP    I = I + 1"
    "        LE(I,8) :S(LOOP)"
    "end")
  (is (= 9 ($$ 'M))))

(deftest t_array_fill_squares
  "A<I>=I*I for I=1..5; A<5>=25"
  (prog-steplimit 2000 100
    "        A = ARRAY(5)"
    "        I = 1"
    "LOOP    A<I> = I * I"
    "        I = I + 1"
    "        LE(I,5) :S(LOOP)"
    "end")
  (is (= 25 (array-get ($$ 'A) [5]))))

(deftest t_array_dot_product
  "[1,2,3].[4,5,6]=32"
  (prog-steplimit 2000 100
    "        A = ARRAY(3)"
    "        B = ARRAY(3)"
    "        A<1> = 1"
    "        A<2> = 2"
    "        A<3> = 3"
    "        B<1> = 4"
    "        B<2> = 5"
    "        B<3> = 6"
    "        D = 0"
    "        I = 1"
    "LOOP    D = D + A<I> * B<I>"
    "        I = I + 1"
    "        LE(I,3) :S(LOOP)"
    "end")
  (is (= 32 ($$ 'D))))

