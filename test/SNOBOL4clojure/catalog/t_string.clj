(ns SNOBOL4clojure.catalog.t_string
  "Worm catalog — migrated from test_worm1000.clj.
   Each deftest encodes ONE semantic fact about SNOBOL4.
   Shape A: atomic (single assignment or match), budget 2000ms.
   Shape B: bounded loop, budget 2000ms, documented bound."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.test-helpers :refer [prog prog-timeout prog-infinite]]))

(GLOBALS *ns*)
(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote SNOBOL4clojure.catalog.t_string))) (f)))

;; prog, prog-timeout, prog-infinite imported from test-helpers (Halting Problem defence).
;; All tests run under 2000ms wall-clock budget (default prog macro).

(deftest worm_concat_ab
  "S = 'a' 'b' => 'ab'"
  (prog
    "        S = 'a' 'b'"
    "end"
  )
    (is (= "ab" ($$ (quote S))))
)

(deftest worm_concat_abc
  "S = 'a' 'b' 'c' => 'abc'"
  (prog
    "        S = 'a' 'b' 'c'"
    "end"
  )
    (is (= "abc" ($$ (quote S))))
)

(deftest worm_concat_empty_left
  "S = '' 'hello' => 'hello'"
  (prog
    "        S = '' 'hello'"
    "end"
  )
    (is (= "hello" ($$ (quote S))))
)

(deftest worm_concat_empty_right
  "S = 'hello' '' => 'hello'"
  (prog
    "        S = 'hello' ''"
    "end"
  )
    (is (= "hello" ($$ (quote S))))
)

(deftest worm_concat_both_empty
  "S = '' '' => ''"
  (prog
    "        S = '' ''"
    "end"
  )
    (is (= "" ($$ (quote S))))
)

(deftest worm_concat_int_str
  "S = 42 'x' => '42x'"
  (prog
    "        S = 42 'x'"
    "end"
  )
    (is (= "42x" ($$ (quote S))))
)

(deftest worm_concat_str_int
  "S = 'x' 42 => 'x42'"
  (prog
    "        S = 'x' 42"
    "end"
  )
    (is (= "x42" ($$ (quote S))))
)

(deftest worm_concat_int_int
  "S = 1 2 => '12'"
  (prog
    "        S = 1 2"
    "end"
  )
    (is (= "12" ($$ (quote S))))
)

(deftest worm_concat_spaces
  "S = 'hello' ' ' 'world' => 'hello world'"
  (prog
    "        S = 'hello' ' ' 'world'"
    "end"
  )
    (is (= "hello world" ($$ (quote S))))
)

(deftest worm_concat_long_concat
  "S = 'foo' 'bar' 'baz' 'qux' => 'foobarbazqux'"
  (prog
    "        S = 'foo' 'bar' 'baz' 'qux'"
    "end"
  )
    (is (= "foobarbazqux" ($$ (quote S))))
)

(deftest worm_size_empty
  "SIZE('') => 0"
  (prog
    "        I = SIZE('')"
    "end"
  )
    (is (= 0 ($$ (quote I))))
)

(deftest worm_size_one
  "SIZE('a') => 1"
  (prog
    "        I = SIZE('a')"
    "end"
  )
    (is (= 1 ($$ (quote I))))
)

(deftest worm_size_five
  "SIZE('hello') => 5"
  (prog
    "        I = SIZE('hello')"
    "end"
  )
    (is (= 5 ($$ (quote I))))
)

(deftest worm_size_spaces
  "SIZE('  ') => 2"
  (prog
    "        I = SIZE('  ')"
    "end"
  )
    (is (= 2 ($$ (quote I))))
)

(deftest worm_size_ten
  "SIZE('1234567890') => 10"
  (prog
    "        I = SIZE('1234567890')"
    "end"
  )
    (is (= 10 ($$ (quote I))))
)

(deftest worm_trim_trailing
  "TRIM('hello   ') = 'hello'"
  (prog
    "        R = TRIM('hello   ')"
    "end"
  )
    (is (= "hello" ($$ (quote R))))
)

(deftest worm_trim_no_trailing
  "TRIM('hello') = 'hello'"
  (prog
    "        R = TRIM('hello')"
    "end"
  )
    (is (= "hello" ($$ (quote R))))
)

(deftest worm_trim_keeps_leading
  "TRIM('  hello  ') keeps leading spaces"
  (prog
    "        R = TRIM('  hello  ')"
    "end"
  )
    (is (= "  hello" ($$ (quote R))))
)

(deftest worm_reverse_hello
  "REVERSE('hello') = 'olleh'"
  (prog
    "        R = REVERSE('hello')"
    "end"
  )
    (is (= "olleh" ($$ (quote R))))
)

(deftest worm_reverse_empty
  "REVERSE('') = ''"
  (prog
    "        R = REVERSE('')"
    "end"
  )
    (is (= "" ($$ (quote R))))
)

(deftest worm_reverse_single
  "REVERSE('a') = 'a'"
  (prog
    "        R = REVERSE('a')"
    "end"
  )
    (is (= "a" ($$ (quote R))))
)

(deftest worm_dupl_basic
  "DUPL('ab',3) = 'ababab'"
  (prog
    "        R = DUPL('ab',3)"
    "end"
  )
    (is (= "ababab" ($$ (quote R))))
)

(deftest worm_dupl_zero
  "DUPL('ab',0) = ''"
  (prog
    "        R = DUPL('ab',0)"
    "end"
  )
    (is (= "" ($$ (quote R))))
)

(deftest worm_dupl_one
  "DUPL('xyz',1) = 'xyz'"
  (prog
    "        R = DUPL('xyz',1)"
    "end"
  )
    (is (= "xyz" ($$ (quote R))))
)

