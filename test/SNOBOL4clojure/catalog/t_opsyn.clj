(ns SNOBOL4clojure.catalog.t-opsyn
  "Sprint 25E — OPSYN tests.
   OPSYN(new, old, n): n=0 function synonym, n=1 unary op, n=2 binary op."
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.core :refer :all]
            [SNOBOL4clojure.env :as env]
            [SNOBOL4clojure.test-helpers :refer [run-with-timeout]]))

(use-fixtures :each (fn [f] (GLOBALS) (reset! env/<CHANNELS> {}) (f)))

;; ── Function synonym (n=0) ────────────────────────────────────────────────────
(deftest t-opsyn-fn-synonym-builtin
  "OPSYN('MYSIZE','SIZE',0) — alias for built-in SIZE."
  (let [r (run-with-timeout
             "        OPSYN('MYSIZE','SIZE',0)\n        OUTPUT = MYSIZE('hello')\nEND\n" 2000)]
    (is (= :ok (:exit r)))
    (is (= "5\n" (:stdout r)))))

(deftest t-opsyn-fn-synonym-user-defined
  "OPSYN alias for a user-defined function."
  (let [r (run-with-timeout
             "        DEFINE('DOUBLE(X)')
        OPSYN('DBL','DOUBLE',0)
        OUTPUT = DBL(7)
        :(END)
DOUBLE  DOUBLE = X + X :(RETURN)
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "14\n" (:stdout r)))))

(deftest t-opsyn-fn-alias-gt
  "OPSYN('ISGT','GT',0) — alias for GT comparison function."
  (let [r (run-with-timeout
             "        OPSYN('ISGT','GT',0)
        ISGT(10,3) :F(NO)
        OUTPUT = 'yes'
        :(END)
NO      OUTPUT = 'no'
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "yes\n" (:stdout r)))))

(deftest t-opsyn-fn-alias-size-with-result
  "OPSYN alias returns the same value as the original."
  (let [r (run-with-timeout
             "        OPSYN('SZ','SIZE',0)
        S = 'abcde'
        N = SZ(S)
        OUTPUT = N
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "5\n" (:stdout r)))))

;; ── Binary operator synonym (n=2) ─────────────────────────────────────────────
(deftest t-opsyn-binary-plus-as-concat
  "OPSYN('+','SADD',2) redefines + to call user function SADD (string concat)."
  (let [r (run-with-timeout
             "        DEFINE('SADD(A,B)')
        OPSYN('+','SADD',2)
        OUTPUT = 'hello' + ' world'
        :(END)
SADD    SADD = A B :(RETURN)
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "hello world\n" (:stdout r)))))

(deftest t-opsyn-binary-builtin-still-works-after-alias
  "Defining a new alias does not affect the original symbol."
  (let [r (run-with-timeout
             "        OPSYN('PLUS','GT',0)
        OUTPUT = 3 + 4
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "7\n" (:stdout r)))))

(deftest t-opsyn-binary-custom-multiply
  "OPSYN('*','MYMUL',2) — redefine * to add instead (nonsense but tests dispatch)."
  (let [r (run-with-timeout
             "        DEFINE('MYMUL(A,B)')
        OPSYN('*','MYMUL',2)
        OUTPUT = 3 * 4
        :(END)
MYMUL   MYMUL = A + B :(RETURN)
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "7\n" (:stdout r)))))

;; ── Concat-function alias via OPSYN ───────────────────────────────────────────
(deftest t-opsyn-fn-concat-alias
  "OPSYN for user function that does concatenation."
  (let [r (run-with-timeout
             "        DEFINE('JOIN(A,B)')
        OPSYN('GLUE','JOIN',0)
        OUTPUT = GLUE('foo','bar')
        :(END)
JOIN    JOIN = A B :(RETURN)
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "foobar\n" (:stdout r)))))

;; ── OPSYN with empty old-name (no-op / undefine) ──────────────────────────────
(deftest t-opsyn-empty-old-noop
  "OPSYN('X','',0) with empty old-name registers a no-op fn; does not crash."
  (let [r (run-with-timeout
             "        OPSYN('NOOP','',0)\n        OUTPUT = 'ok'\nEND\n" 2000)]
    (is (= :ok (:exit r)))
    (is (= "ok\n" (:stdout r)))))

;; ── OPSYN chaining ────────────────────────────────────────────────────────────
(deftest t-opsyn-chain
  "OPSYN of an OPSYN — alias of an alias."
  (let [r (run-with-timeout
             "        OPSYN('SZ1','SIZE',0)
        OPSYN('SZ2','SZ1',0)
        OUTPUT = SZ2('hello')
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "5\n" (:stdout r)))))

;; ── OPSYN in AI-SNOBOL style: operator as function ───────────────────────────
(deftest t-opsyn-apply-synonym
  "APPLY a synonymized function — tests that OPSYN entries are visible to APPLY."
  (let [r (run-with-timeout
             "        DEFINE('ADD(A,B)')
        OPSYN('MYADD','ADD',0)
        OUTPUT = APPLY('MYADD',3,4)
        :(END)
ADD     ADD = A + B :(RETURN)
END
" 2000)]
    (is (= :ok (:exit r)))
    (is (= "7\n" (:stdout r)))))
