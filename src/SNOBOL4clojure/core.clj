(ns SNOBOL4clojure.core
  "SNOBOL4/Clojure — public entry point.
   Loads all sub-namespaces and re-exports the public API via def aliases
   so that (require '[SNOBOL4clojure.core :refer :all]) works unchanged."
  (:gen-class)
  (:require [clojure.pprint            :as pp]
            [SNOBOL4clojure.env        :as env]
            [SNOBOL4clojure.primitives :as prim]
            [SNOBOL4clojure.match      :as match]
            [SNOBOL4clojure.patterns   :as pat]
            [SNOBOL4clojure.operators  :as ops]
            [SNOBOL4clojure.functions  :as fns]
            [SNOBOL4clojure.grammar    :as gram]
            [SNOBOL4clojure.emitter    :as emit]
            [SNOBOL4clojure.compiler   :as comp]
            [SNOBOL4clojure.runtime    :as rt]
            [SNOBOL4clojure.trace      :as trace]
            [SNOBOL4clojure.transpiler :as xpile]
            [SNOBOL4clojure.vm         :as vm])
  (:refer-clojure :exclude [= + - * / num]))

;; ── Re-export env ─────────────────────────────────────────────────────────────
(def  ε          env/ε)
(def  η          env/η)
(def  GLOBALS    env/GLOBALS)
(def  snobol-set! env/snobol-set!)
(def  &ALPHABET  env/&ALPHABET)
(def  &ANCHOR    env/&ANCHOR)
(def  &DIGITS    env/&DIGITS)
(def  &DUMP      env/&DUMP)
(def  &ERRLIMIT  env/&ERRLIMIT)
(def  &ERRTEXT   env/&ERRTEXT)
(def  &ERRTYPE   env/&ERRTYPE)
(def  &FTRACE    env/&FTRACE)
(def  &FULLLSCAN env/&FULLLSCAN)
(def  &LASTNO    env/&LASTNO)
(def  &LCASE     env/&LCASE)
(def  &MAXLNGTH  env/&MAXLNGTH)
(def  &PROFILE   env/&PROFILE)
(def  &TRACE     env/&TRACE)
(def  &TRIM      env/&TRIM)
(def  &STCOUNT   env/&STCOUNT)
(def  &STLIMIT   env/&STLIMIT)
(def  &UCASE     env/&UCASE)
(def  INPUT$     env/INPUT$)
(def  OUTPUT$    env/OUTPUT$)
(def  TERMINAL$  env/TERMINAL$)
;; deftype — import directly
(import SNOBOL4clojure.env.NAME)
(def DATATYPE    env/DATATYPE)
(def ARRAY       env/ARRAY)
(def TABLE       env/TABLE)
(def SET         env/SET)
(def $$          env/$$)
(def equal       env/equal)
(def not-equal   env/not-equal)
(def Σ+          env/Σ+)
(def num         env/num)
(def out         env/out)
(def reference   env/reference)

;; ── Re-export patterns ────────────────────────────────────────────────────────
(def ANY     pat/ANY)
(def BREAK   pat/BREAK)
(def BREAKX  pat/BREAKX)
(def NOTANY  pat/NOTANY)
(def NSPAN   pat/NSPAN)
(def SPAN    pat/SPAN)
(def ARBNO   pat/ARBNO)
(def FENCE   pat/FENCE)
(def LEN     pat/LEN)
(def POS     pat/POS)
(def RPOS    pat/RPOS)
(def RTAB    pat/RTAB)
(def TAB     pat/TAB)
(def ARB     pat/ARB)
(def BAL     pat/BAL)
(def BOL     pat/BOL)
(def EOL     pat/EOL)
(def REM     pat/REM)
(def ABORT   pat/ABORT)
(def FAIL    pat/FAIL)
(def SUCCEED pat/SUCCEED)

;; ── Re-export match engine ────────────────────────────────────────────────────
(def MATCH      match/MATCH)
(def SEARCH     match/SEARCH)
(def FULLMATCH  match/FULLMATCH)
(def REPLACE    match/REPLACE)
(def COLLECT!  match/COLLECT!)

;; ── Re-export operators / EVAL ────────────────────────────────────────────────
(def =     ops/=)
(def ?     ops/?)
(def ?=    ops/?=)
(def &     ops/&)
(def +     ops/+)
(def -     ops/-)
(def *     ops/*)
(def /     ops//)
(def $     ops/$)
(def .     ops/.)
(def |     ops/|)
(def at    ops/at)
(def tilde ops/tilde)
(def !     ops/!)
(def **    ops/**)
(def %     ops/%)
(def sharp ops/sharp)
(def EQ    ops/EQ)
(def NE    ops/NE)
(def LE    ops/LE)
(def LT    ops/LT)
(def GE    ops/GE)
(def GT    ops/GT)
(def LEQ   ops/LEQ)
(def LNE   ops/LNE)
(def LLE   ops/LLE)
(def LLT   ops/LLT)
(def LGE   ops/LGE)
(def LGT   ops/LGT)
(def IDENT  ops/IDENT)
(def DIFFER ops/DIFFER)
(def EVAL  ops/EVAL)
(def EVAL! ops/EVAL!)
(def INVOKE ops/INVOKE)
(def APPLY  ops/APPLY)

;; ── Re-export functions ───────────────────────────────────────────────────────
(def SIZE    fns/SIZE)
(def REPLACE fns/REPLACE)
(def DUPL    fns/DUPL)
(def TRIM    fns/TRIM)
(def REVERSE fns/REVERSE)
(def DATA    fns/DATA)
(def DATA!   fns/DATA!)
(def FIELD   fns/FIELD)
(def ASCII   fns/ASCII)
(def CHAR    fns/CHAR)
(def REMDR   fns/REMDR)
(def INTEGER fns/INTEGER)
(def REAL    fns/REAL)
(def STRING  fns/STRING)
(def CONVERT fns/CONVERT)
(def COPY    fns/COPY)
(def SORT    fns/SORT)
(def RSORT   fns/RSORT)
(def ITEM    fns/ITEM)
(def PROTOTYPE fns/PROTOTYPE)
(def data-type-registry fns/data-type-registry)

;; ── Re-export compiler / runtime ─────────────────────────────────────────────
(def CODE!             comp/CODE!)
(def CODE              comp/CODE)
(def RUN               rt/RUN)
;; Stage 23A — EDN cache
(def ir->edn           comp/ir->edn)
(def edn->ir           comp/edn->ir)
(def CODE-ir           comp/CODE-ir)
(def CODE-memo         comp/CODE-memo)
(def CODE-cached       comp/CODE-cached)
(def compile-to-file   comp/compile-to-file)
(def load-ir           comp/load-ir)
(def clear-memo!       comp/clear-memo!)
(def memo-stats        comp/memo-stats)
(def precompile-corpus! comp/precompile-corpus!)

;; ── -main ─────────────────────────────────────────────────────────────────────
(defn -main "SNOBOL4/Clojure." [& _args]
  (def BED (EVAL '[(POS 0) (| "B" "R") (| "E" "EA") (| "D" "DS") (RPOS 0)]))
  (pp/pprint BED)
  (? "READS" BED))

;; ── Re-export transpiler (Stage 23B) ─────────────────────────────────────────
(def transpile          xpile/transpile)
(def transpile-ir       xpile/transpile-ir)
(def load-transpiled!   xpile/load-transpiled!)
(def run-transpiled!    xpile/run-transpiled!)
(def bench-compare      xpile/bench-compare)

;; ── Re-export VM (Stage 23C) ──────────────────────────────────────────────────
(def compile-ir         vm/compile-ir)
(def compile-src        vm/compile-src)
(def run-vm!            vm/run-vm!)
(def run-program!       vm/run-program!)
(def bench-compare-vm   vm/bench-compare-vm)
;; Opcode constants
(def OP-HALT            vm/OP-HALT)
(def OP-EXEC            vm/OP-EXEC)
(def OP-EXEC-S          vm/OP-EXEC-S)
(def OP-EXEC-F          vm/OP-EXEC-F)
(def OP-EXEC-SF         vm/OP-EXEC-SF)
(def OP-JUMP            vm/OP-JUMP)
(def OP-SIGNAL          vm/OP-SIGNAL)

;; ── Re-export trace API ───────────────────────────────────────────────────────
;; These are available to SNOBOL4 programs via INVOKE and to Clojure tests.
(def trace-register!     trace/trace!)
(def trace-stop!         trace/stoptr!)
(def trace-clear!        trace/clear-all-traces!)
(def trace-enable-full!  trace/enable-full-trace!)
(def trace-disable-full! trace/disable-full-trace!)
(def trace-output        trace/*trace-output*)

;; ── Re-export debug/probe API ─────────────────────────────────────────────────
(def snapshot!  env/snapshot!)
