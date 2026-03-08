(ns SNOBOL4clojure.test-sprint12
  "Sprint 12 tests: CONVERT, DATA/FIELD, SORT/RSORT."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]))

(use-fixtures :each (fn [f] (GLOBALS *ns*) (f)))

;; ─────────────────────────────────────────────────────────────────────────────
;; 12.1  CONVERT — type coercion matrix
;; ─────────────────────────────────────────────────────────────────────────────

(deftest convert-string-string
  (RUN " a = 'hello'\n b = convert(a,'STRING')\n")
  (is (= "hello" ($$ 'B))))

(deftest convert-string-integer
  (RUN " a = '42'\n b = convert(a,'INTEGER')\n")
  (is (= 42 ($$ 'B))))

(deftest convert-string-integer-overflow
  (RUN " d = 'failure'\n a = '999999999999999999999999999999'\n b = convert(a,'INTEGER') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-string-real
  (RUN " a = '3.14'\n b = convert(a,'REAL')\n")
  (is (= 3.14 ($$ 'B))))

(deftest convert-string-real-overflow
  (RUN " d = 'failure'\n a = '1e9999'\n b = convert(a,'REAL') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-string-pattern
  (RUN " a = 'abc'\n b = convert(a,'PATTERN')\n")
  (is (= "PATTERN" (DATATYPE ($$ 'B)))))

(deftest convert-string-name
  (RUN " a = 'myvar'\n b = convert(a,'NAME')\n")
  (is (= "NAME" (DATATYPE ($$ 'B)))))

(deftest convert-string-array-fails
  (RUN " d = 'failure'\n a = 'hello'\n b = convert(a,'ARRAY') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-string-table-fails
  (RUN " d = 'failure'\n a = 'hello'\n b = convert(a,'TABLE') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-integer-string
  (RUN " a = 32767\n b = convert(a,'STRING')\n c = datatype(b)\n")
  (is (= "32767" ($$ 'B)))
  (is (= "STRING" ($$ 'C))))

(deftest convert-integer-integer
  (RUN " a = 100\n b = convert(a,'INTEGER')\n")
  (is (= 100 ($$ 'B))))

(deftest convert-integer-real
  (RUN " a = 10\n b = convert(a,'REAL')\n c = datatype(b)\n")
  (is (= 10.0 ($$ 'B)))
  (is (= "REAL" ($$ 'C))))

(deftest convert-integer-pattern
  (RUN " a = 99\n b = convert(a,'PATTERN')\n")
  (is (= "PATTERN" (DATATYPE ($$ 'B)))))

(deftest convert-integer-name
  (RUN " a = 123\n b = convert(a,'NAME')\n")
  (is (= "NAME" (DATATYPE ($$ 'B)))))

(deftest convert-integer-table-fails
  (RUN " d = 'failure'\n a = 1\n b = convert(a,'TABLE') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-real-string
  (RUN " a = 3.14\n b = convert(a,'STRING')\n")
  (is (= "3.14" ($$ 'B))))

(deftest convert-real-integer
  (RUN " a = 9.9\n b = convert(a,'INTEGER')\n")
  (is (= 9 ($$ 'B))))

(deftest convert-real-integer-overflow
  (RUN " d = 'failure'\n a = 3.14e200\n b = convert(a,'INTEGER') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-real-real
  (RUN " a = 2.5\n b = convert(a,'REAL')\n")
  (is (= 2.5 ($$ 'B))))

(deftest convert-real-pattern
  (RUN " a = 1.5\n b = convert(a,'PATTERN')\n")
  (is (= "PATTERN" (DATATYPE ($$ 'B)))))

(deftest convert-pattern-pattern
  (RUN " d = 'failure'\n a = any('abc')\n b = convert(a,'PATTERN') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "success" ($$ 'D))))

(deftest convert-pattern-string-fails
  (RUN " d = 'failure'\n a = any('abc')\n b = convert(a,'STRING') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-pattern-integer-fails
  (RUN " d = 'failure'\n a = any('abc')\n b = convert(a,'INTEGER') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-array-to-table
  (RUN (str " b = array('3,2')\n"
            " b<1,1> = 'x'\n b<1,2> = 10\n"
            " b<2,1> = 'y'\n b<2,2> = 20\n"
            " b<3,1> = 'z'\n b<3,2> = 30\n"
            " a = convert(b,'TABLE')\n"
            " vx = item(a,'x')\n"
            " vy = item(a,'y')\n"
            " vz = item(a,'z')\n"))
  (is (= "TABLE" (DATATYPE ($$ 'A))))
  (is (= 10 ($$ 'VX)))
  (is (= 20 ($$ 'VY)))
  (is (= 30 ($$ 'VZ))))

(deftest convert-array-to-table-wrong-shape-fails
  (RUN " d = 'failure'\n b = array('5')\n a = convert(b,'TABLE') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-array-to-table-wrong-cols-fails
  (RUN " d = 'failure'\n b = array('5,3')\n a = convert(b,'TABLE') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-table-to-array
  (RUN (str " t = table()\n"
            " t<'a'> = 1\n"
            " t<'b'> = 2\n"
            " t<'c'> = 3\n"
            " b = convert(t,'ARRAY')\n"
            " proto = prototype(b)\n"))
  (is (= "ARRAY" (DATATYPE ($$ 'B))))
  (is (= "1:3,1:2" ($$ 'PROTO))))

(deftest convert-table-table
  (RUN " d = 'failure'\n t = table()\n t<1> = 2\n b = convert(t,'TABLE') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "success" ($$ 'D))))

(deftest convert-table-string-fails
  (RUN " d = 'failure'\n t = table()\n b = convert(t,'STRING') :F(DONE)\n d = 'success'\nDONE\n")
  (is (= "failure" ($$ 'D))))

(deftest convert-name-string
  (RUN " a = 'quick'\n b = convert(a,'NAME')\n c = datatype(b)\n d = convert(b,'STRING')\n")
  (is (= "NAME" ($$ 'C)))
  (is (= "quick" ($$ 'D))))

(deftest datatype-through-invoke
  (RUN " a = 'hello'\n b = datatype(a)\n")
  (is (= "STRING" ($$ 'B)))
  (RUN " a = 42\n b = datatype(a)\n")
  (is (= "INTEGER" ($$ 'B)))
  (RUN " a = 3.14\n b = datatype(a)\n")
  (is (= "REAL" ($$ 'B)))
  (RUN " a = table()\n b = datatype(a)\n")
  (is (= "TABLE" ($$ 'B)))
  (RUN " a = array('3')\n b = datatype(a)\n")
  (is (= "ARRAY" ($$ 'B))))

;; ─────────────────────────────────────────────────────────────────────────────
;; 12.2  DATA / FIELD — program-defined data types
;; ─────────────────────────────────────────────────────────────────────────────

(deftest data-basic-constructor-accessors
  (RUN (str " data('COMPLEX(REAL,IMAG)')\n"
            " x = complex(3.2,-2.0)\n"
            " i = imag(x)\n"
            " r = real(x)\n"))
  (is (= -2.0 ($$ 'I)))
  (is (= 3.2  ($$ 'R))))

(deftest data-string-fields
  (RUN (str " data('COMPLEX(REAL,IMAG)')\n"
            " x = complex('AAA','BBB')\n"
            " i = imag(x)\n"
            " r = real(x)\n"))
  (is (= "BBB" ($$ 'I)))
  (is (= "AAA" ($$ 'R))))

(deftest data-pattern-fields
  (RUN (str " data('COMPLEX(REAL,IMAG)')\n"
            " x = complex(any('ABC'),span('123'))\n"
            " i = imag(x)\n"
            " r = real(x)\n"))
  (is (= "PATTERN" (DATATYPE ($$ 'I))))
  (is (= "PATTERN" (DATATYPE ($$ 'R)))))

(deftest data-datatype-returns-typename
  (RUN (str " data('POINT(X,Y)')\n"
            " p = point(10,20)\n"
            " t = datatype(p)\n"))
  (is (= "POINT" ($$ 'T))))

(deftest data-field-write
  (RUN (str " data('COMPLEX(REAL,IMAG)')\n"
            " x = complex('AAA','BBB')\n"
            " imag(x) = 45\n"
            " real(x) = -5\n"
            " i = imag(x)\n"
            " r = real(x)\n"))
  (is (= 45 ($$ 'I)))
  (is (= -5 ($$ 'R))))

(deftest field-returns-field-name
  (RUN (str " data('COMPLEX(REAL,IMAG)')\n"
            " f1 = field('COMPLEX',1)\n"
            " f2 = field('COMPLEX',2)\n"))
  (is (= "REAL" ($$ 'F1)))
  (is (= "IMAG" ($$ 'F2))))

(deftest field-out-of-range-fails
  (RUN (str " d = 'failure'\n"
            " data('COMPLEX(REAL,IMAG)')\n"
            " f = field('COMPLEX',3) :F(DONE)\n"
            " d = 'success'\nDONE\n"))
  (is (= "failure" ($$ 'D))))

;; ─────────────────────────────────────────────────────────────────────────────
;; 12.3  SORT / RSORT
;; ─────────────────────────────────────────────────────────────────────────────

(deftest sort-table-returns-array
  (RUN (str " t = table()\n"
            " t<'a'> = 30\n"
            " t<'b'> = 10\n"
            " t<'c'> = 20\n"
            " s = sort(t)\n"
            " v1 = s<1,2>\n"
            " v2 = s<2,2>\n"
            " v3 = s<3,2>\n"))
  (is (= "ARRAY" (DATATYPE ($$ 'S))))
  (is (= 10 ($$ 'V1)))
  (is (= 20 ($$ 'V2)))
  (is (= 30 ($$ 'V3))))

(deftest rsort-table-returns-array
  (RUN (str " t = table()\n"
            " t<'a'> = 30\n"
            " t<'b'> = 10\n"
            " t<'c'> = 20\n"
            " s = rsort(t)\n"
            " v1 = s<1,2>\n"
            " v2 = s<2,2>\n"
            " v3 = s<3,2>\n"))
  (is (= "ARRAY" (DATATYPE ($$ 'S))))
  (is (= 30 ($$ 'V1)))
  (is (= 20 ($$ 'V2)))
  (is (= 10 ($$ 'V3))))
