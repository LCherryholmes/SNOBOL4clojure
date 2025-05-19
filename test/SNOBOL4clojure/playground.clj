; (:require [core.matrix :as matrix])
; (:require [numeric.expresso.core])
; (:require [clojure.core.matrix.operators :as matrix])
; (:require [clojure.core.match :as match])
; (:require [clojure.core.logic :as logic])
; numeric tower
;---------------------------------------------------------------------------------------------------
(instance? Class expr)
;---------------------------------------------------------------------------------------------------
(def proto-data-name  #"^([A-Za-z][0-9-.A-Z_a-z]+)\((.*)$")
(def proto-data-field #"^([0-9-.A-Z_a-z]+)[,)](.*)$")
(defn proto-data [S]
  (let [[_ name rem] (re-find proto-data-name S)]
    (loop [rem rem fields []]
      (if (= rem "") [(symbol name) fields]
        (let [[_ field rem] (re-find proto-data-field rem)]
          (recur rem (conj fields (symbol field))))))))
(defn DATA! [S]
  (let [[name fields] (proto-data S)]
    (list 'do
      (apply list 'defprotocol (symbol (str \& name))
        (reduce #(conj %1 (list %2 ['this] ['this '_])) [] fields))
      (apply list 'deftype name
        (reduce #(conj %1 (with-meta %2 {:unsynchronized-mutable true})) [] fields)
        (symbol (str \& name))
        (reduce
          #(conj %1
            (list %2 ['this] %2)
            (list %2 ['this '_] (list 'set! %2 '_))) [] fields)))))
(defn DATA [S] (let [data (DATA! S)] (binding [*print-meta* true] (out data)) (eval data)))
(DATA "tree(t,v,n,c)")
(def x (tree. \+ nil 2 [3 4]))
(t x)
(t x \-)
(t x)
;---------------------------------------------------------------------------------------------------
(for [s (re-seq proto_data "tree(t,v,n,c)")] (println s))
;---------------------------------------------------------------------------------------------------
(deftype A [x y z]); ClojureScript
(def a (A. 1 2))
(.-x a)
(set! (.-x a) 19)

(defprotocol &A (x [this] [this _]) (y [this] [this _]))
(deftype A
  [^:unsynchronized-mutable x ^:volatile-mutable y]
  &A (x [this] x)
     (x [this _] (set! x _))
     (y [this] x)
     (y [this _] (set! y _)))
(def a (A. 1 2))
(x a)
(y a)
(x a 19)
(y a 19)

(definterface &NAME (n []) (n [_]))
(deftype NAME [^:unsynchronized-mutable n] &NAME (n [this] n) (n [this _] (set! n _)))
(def N (NAME. 'Roman))
(pprint N)
(.x a)
(.y a)
(.x a 19)
(.y a 19)
;---------------------------------------------------------------------------------------------------
  (out (CODE "hello OUTPUT = 'Hello World!' :F(END)"))
  (out (CODE "goodbye OUTPUT = 'Goodbye.' :(END)"))
;---------------------------------------------------------------------------------------------------
(comment (array (for [i (range 3)] (for [j (range 3)] (str i j))))); core.matrix

(let [π = 3.141592653589793])
(defn rotataion [turns]
  (let [a (* 2.0 π turns)]
    [[   (cos a)  (sin a)]
     [(- (sin a)) (cos a)]]))
;---------------------------------------------------------------------------------------------------
(declare dq path part)
(declare Roman n)
(defn dq [_path] (binding [dq ε path _path part ε] (RUN :dq) dq));DEFINE('dq(path)part')
(defn Roman-binding [_n] (binding [Roman ε n _n] (RUN :Roman) Roman))
(defn Roman-save-restore [_n]
 '(let [_Roman ($$ 'Roman) __n ($$ 'n)]
    (def Roman ε)
    (def n _n)
    (RUN :Roman)
    (def n __n)
    (let [__Roman Roman]
      (def Roman _Roman)
      __Roman)))
;---------------------------------------------------------------------------------------------------
(def LABELS-Roman {3 :Roman 6 :RomanEnd 8 :END})
(def STMTNOS-Roman {:Roman 3 :RomanEnd 6 :END 8})
(def CODE-Roman {
1         ['(DEFINE "Roman(n)units")]
2         ['(= romanXlat "0,1I,2II,3III,4IV,5V,6VI,7VII,8VIII,9IX,") {:G :RomanEnd}]
:Roman    ['(?= n [(RPOS 1) (. (LEN 1) units)]) {:F :RETURN}]
4         ['(? romanXlat [units (. (BREAK ",") units)]) {:F :FRETURN}]
5         ['(= Roman [(REPLACE (Roman n) "IVXLCDM" "XLCDM**") units]) {:S :RETURN, :F :FRETURN}]
:RomanEnd []
7         ['(Roman "MMXXI")]
:END      []
})
;---------------------------------------------------------------------------------------------------
(defn files [directories]
  (reduce (fn [files directory]
    (reduce (fn [files file]
      (let [filenm (str file)]
        (if (re-find #"^.+\.(sno|spt|inc|SNO|SPT|INC)$" filenm)
          (conj files filenm) files)))
      files
      (file-seq (io/file directory))))
    []
    directories))
(def dirs ["./src/sno" "./src/inc" "./src/test ./src/rinky"])
;---------------------------------------------------------------------------------------------------
(def SNO [
"copy OUTPUT = INPUT :S(copy)F(END)"
" n = 0 ;copy OUTPUT = INPUT :F(done) ; n = n + 1 :(copy) ;done OUTPUT = \"Program copied \" n \" lines.\" ;END"
" &TRIM = 1 ;nextl chars = chars + SIZE(INPUT) :F(done); lines = lines + 1 :(nextl) ;done OUTPUT = chars \" characters, \" +lines \" lines read.\" ;END"
" &TRIM = 1
  TERMINAL = 'Enter test lines, terminate with EOF'
* Read input line, convert lower case to upper.
loop s = REPLACE(TERMINAL, &LCASE, &UCASE) :F(END)
* Check for palindrome:
  TERMINAL = IDENT(s, REVERSE(s)) 'Palindrome!' :S(loop)
  TERMINAL = 'No, try again.' :(loop)
END
"
  " A = (X ? Y) (Q ? P)"
  " IDENT(,, TERMINAL = 'Sneaky!')"
])
;---------------------------------------------------------------------------------------------------
(defn doit []
  (case 1
    1 (doseq [s SNO] (compile-stmt s))
    2 (doseq [filenm (files dirs)]
        (let [program (slurp filenm)]
          (println ";------------------------------------------------------ " filenm)
          ))
    3 (doseq [filenm (files dirs)]
        (with-open [rdr (io/reader filenm)]
          (doseq [line (line-seq rdr)]
            (let [ast (parse-command line) code (coder ast 1)]
              (println code)))))
  ))
;---------------------------------------------------------------------------------------------------
(println (Math/pow 2 2))
;---------------------------------------------------------------------------------------------------
(comment

		(def x 10)
		(def ^:dynamic x 10)
		(defn assign [n new] (alter-var-root n (fn [old] new)))
		(assign #'x 20)
		(defn tryit [] x)
		(println (binding [x 20] x))
		(println (binding [x 20] (tryit)))
		(import '[clojure.lang Var])
		(Var/create 42); root binding
		(let [V (.setDynamic (Var/create 0))]
		  (do (var-get V); 0
		      (with-bindings {V 42} (var-get V)))); 42
		(find-var SNOBOL4clojure.core/SEQ)
		(var SEQ)
		(var-get x)
		(var-set x val)
  (intern ns name)
  (intern ns name val)

		(defmacro test (println &form &env))
		(-> {} (assoc :a 1) (assoc :b 2))
		(println x)
		(println x)
		(println x)
		(println x)

		(the-ns 'SNOBOL4clojure.core)
		(ns-map 'SNOBOL4clojure.core)
		(ns-aliases 'SNOBOL4clojure.string)
		(ns-publics 'SNOBOL4clojure.core)
		(set! symbol expr)

		(deftype Address [no label])
		(defmethod key Address [a] (if (.label a) (.label a) (.no a)))
		(->Address at); construct via factory
		(Address. at); construct
)
;---------------------------------------------------------------------------------------------------
;(definline skey [address] (let [[no label] address] (if label label no)))


(def SNO [
   ""
   "0"
   "1"
;  "0.5"
;  "''"
;  "'a'"
;  "'ab'"
;  "'abc'"
   "A"
   "A[0]"
   "A[x]"
   "A[x, y]"
   "A[x, y][z]"
   "S"
   "S T"
   "S T U"
   "S T U V"
   "S T U V W"
   "N"
   "N | O"
   "N | O | P"
   "N | O | P | Q"
   "N | O | P | Q | R"
   "A B | R A | C A | D A | B R A"
;  "S = E"
;  "S ? P"
;  "S ? P = E"
;  "F()"
   "F(x)"
   "F(x y)"
   "F(x, y)"
   "F(x y z)"
   "F(x, y z)"
   "F(x, y, z)"
   "F(w, x, y, z)"
   "F(w, x y, z)"
   "F(w, x, y z)"
   "F(w x, y z)"
   "F(w, x y z)"
   "F(w x y z)"
;  "F (x)"
;  "F (x, y)"
;  "F (x, y, z)"
;  "F (x, y, z, p1, p2, p3)"
;  "F G(x)"
   "F(x) G(y)"
   "F(x, y) G(z)"
   "F(x, y, z, p1, p2, p3) G(z)"
   "SPAN(digits)"
;  "
;(SPAN(digits)
;  ('.' FENCE(SPAN(digits) | epsilon) | epsilon)
;  ('E' | 'e')
;  ('+' | '-' | epsilon)
;  SPAN(digits)
;| SPAN(digits) '.' FENCE(SPAN(digits) | epsilon)
;)"
])



(deftest christmas-tree
   (is (= "" ))
)

(deftest compiling-expression
  (testing "FIXME, I fail." (is (= 0 1)))
)

(def SNO [
  ""
  " "
  "L"
  "L "
  "L X = 10 :(G)"
  "L S P :S(S)"
  "L E = (1 + 2) * 10 :F(F)"
  "L :S(S)F(F)"
  " S"
  " S "
  "L S"
  "L S ="
  "L S = E"
  "L S P"
  "L S P ="
  "L S P = R :F(F)S(S)"
  "L (S ? P = R) :F(F)S(S)"
])

; :partial :true
; :start :rule-name
; :total true
;(insta/parser (clojure.java.io/resource "myparser.bnf"))
;(defparser p "S = 1*'a'" :input-format :abnf :output-format :enlive)
;(time (def p (insta/parser "S = A B; A = 'a'+; B = 'b'+")))
;(time (defparser p         "S = A B; A = 'a'+; B = 'b'+"))
;(def ambiguous (insta/parser "S = A A; A = 'a'*;"))
;(println (insta/parse ambiguous "aaaaaa"))
;(println (insta/parses ambiguous "aaaaaa"))

(slurp "/tmp/test.txt")
(use 'clojure.java.io)
(with-open [rdr (reader "/tmp/test.txt")]
  (doseq [line (line-seq rdr)]
    (println line)))
(with-open [wrtr (writer "/tmp/test.txt")]
  (.write wrtr "Line to be written"))
(spit "/tmp/test.txt" "Line to be written")
(use 'clojure.java.io)
(with-open [wrtr (writer "/tmp/test.txt" :append true)]
  (.write wrtr "Line to be appended"))
(spit "/tmp/test.txt" "Line to be written" :append true)
(reader (file "/tmp/test.txt"))
(writer (file "tmp/test.txt"))
(System/getProperty "user.dir")
(def directory (clojure.java.io/file "/path/to/directory"))
(def files (file-seq directory))
(take 10 files)
