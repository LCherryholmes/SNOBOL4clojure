(ns SNOBOL4clojure.catalog.t_convert
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_convert))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_ascii_A
  "ASCII('A') = 65"
  (prog
    "        I = ASCII('A')"
    "end"
  )
    (is (= 65 ($$ (quote I))))
)

(deftest worm_ascii_Z
  "ASCII('Z') = 90"
  (prog
    "        I = ASCII('Z')"
    "end"
  )
    (is (= 90 ($$ (quote I))))
)

(deftest worm_ascii_a
  "ASCII('a') = 97"
  (prog
    "        I = ASCII('a')"
    "end"
  )
    (is (= 97 ($$ (quote I))))
)

(deftest worm_ascii_z
  "ASCII('z') = 122"
  (prog
    "        I = ASCII('z')"
    "end"
  )
    (is (= 122 ($$ (quote I))))
)

(deftest worm_ascii_0
  "ASCII('0') = 48"
  (prog
    "        I = ASCII('0')"
    "end"
  )
    (is (= 48 ($$ (quote I))))
)

(deftest worm_ascii_9
  "ASCII('9') = 57"
  (prog
    "        I = ASCII('9')"
    "end"
  )
    (is (= 57 ($$ (quote I))))
)

(deftest worm_ascii_space
  "ASCII(' ') = 32"
  (prog
    "        I = ASCII(' ')"
    "end"
  )
    (is (= 32 ($$ (quote I))))
)

(deftest worm_char_65
  "CHAR(65) = 'A'"
  (prog
    "        C = CHAR(65)"
    "end"
  )
    (is (= "A" ($$ (quote C))))
)

(deftest worm_char_90
  "CHAR(90) = 'Z'"
  (prog
    "        C = CHAR(90)"
    "end"
  )
    (is (= "Z" ($$ (quote C))))
)

(deftest worm_char_97
  "CHAR(97) = 'a'"
  (prog
    "        C = CHAR(97)"
    "end"
  )
    (is (= "a" ($$ (quote C))))
)

(deftest worm_char_48
  "CHAR(48) = '0'"
  (prog
    "        C = CHAR(48)"
    "end"
  )
    (is (= "0" ($$ (quote C))))
)

(deftest worm_char_32
  "CHAR(32) = ' '"
  (prog
    "        C = CHAR(32)"
    "end"
  )
    (is (= " " ($$ (quote C))))
)

(deftest worm_replace_fn_hello
  "REPLACE('hello','aeiou...','AEIOU...') = 'hEllO'"
  (prog
    "        R = REPLACE('hello', 'aeiou', 'AEIOU')"
    "end"
  )
    (is (= "hEllO" ($$ (quote R))))
)

(deftest worm_replace_fn_HELLO
  "REPLACE('HELLO','ABCDEF...','abcdef...') = 'hello'"
  (prog
    "        R = REPLACE('HELLO', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"
    "end"
  )
    (is (= "hello" ($$ (quote R))))
)

(deftest worm_replace_fn_abc
  "REPLACE('abc','abc...','xyz...') = 'xyz'"
  (prog
    "        R = REPLACE('abc', 'abc', 'xyz')"
    "end"
  )
    (is (= "xyz" ($$ (quote R))))
)

(deftest worm_replace_fn_abc_1
  "REPLACE('abc','ab...','XY...') = 'XYc'"
  (prog
    "        R = REPLACE('abc', 'ab', 'XY')"
    "end"
  )
    (is (= "XYc" ($$ (quote R))))
)

(deftest worm_replace_fn_aabbcc
  "REPLACE('aabbcc','abc...','ABC...') = 'AABBCC'"
  (prog
    "        R = REPLACE('aabbcc', 'abc', 'ABC')"
    "end"
  )
    (is (= "AABBCC" ($$ (quote R))))
)

(deftest worm_integer_from_str
  "INTEGER('42') succeeds => 42"
  (prog
    "        I = INTEGER('42') :S(YES)F(NO)"
    "YES     R = I             :(END)"
    "NO      R = 'fail'"
    "END"
  )
    (is (= 42 ($$ (quote R))))
)

(deftest worm_integer_from_int
  "INTEGER(42) succeeds => 42"
  (prog
    "        I = INTEGER(42) :S(YES)F(NO)"
    "YES     R = I           :(END)"
    "NO      R = 'fail'"
    "END"
  )
    (is (= 42 ($$ (quote R))))
)

(deftest worm_integer_from_real
  "INTEGER(3.7) truncates => 3"
  (prog
    "        I = INTEGER(3.7) :S(YES)F(NO)"
    "YES     R = I            :(END)"
    "NO      R = 'fail'"
    "END"
  )
    (is (= 3 ($$ (quote R))))
)

(deftest worm_string_from_int
  "STRING(42) => '42'"
  (prog
    "        R = STRING(42)"
    "end"
  )
    (is (= "42" ($$ (quote R))))
)

(deftest worm_string_from_real
  "STRING(3.14) => '3.14'"
  (prog
    "        R = STRING(3.14)"
    "end"
  )
    (is (= "3.14" ($$ (quote R))))
)

(deftest worm_datatype_string
  "DATATYPE('hello') = 'string'"
  (prog
    "        R = DATATYPE('hello')"
    "end"
  )
    (is (= "string" ($$ (quote R))))
)

(deftest worm_datatype_integer
  "DATATYPE(42) = 'integer'"
  (prog
    "        R = DATATYPE(42)"
    "end"
  )
    (is (= "integer" ($$ (quote R))))
)

(deftest worm_datatype_real
  "DATATYPE(3.14) = 'real'"
  (prog
    "        R = DATATYPE(3.14)"
    "end"
  )
    (is (= "real" ($$ (quote R))))
)

(deftest worm_datatype_pattern
  "DATATYPE(ANY('a')) = 'pattern'"
  (prog
    "        R = DATATYPE(ANY('a'))"
    "end"
  )
    (is (= "pattern" ($$ (quote R))))
)

(deftest worm_datatype_array
  "DATATYPE(ARRAY(3)) = 'array'"
  (prog
    "        R = DATATYPE(ARRAY(3))"
    "end"
  )
    (is (= "array" ($$ (quote R))))
)

(deftest worm_datatype_table
  "DATATYPE(TABLE()) = 'table'"
  (prog
    "        R = DATATYPE(TABLE())"
    "end"
  )
    (is (= "table" ($$ (quote R))))
)

