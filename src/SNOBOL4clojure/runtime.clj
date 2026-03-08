(ns SNOBOL4clojure.runtime
  "The SNOBOL4 GOTO-driven statement interpreter.

  ## &STCOUNT / &STLIMIT  (18B.1-18B.2)
  &STCOUNT is reset to 0 at RUN entry and incremented on every statement
  execution (the inner when-let [stmt ...] branch — i.e. every real statement
  body dispatch, not blank/comment lines).
  When &STCOUNT exceeds &STLIMIT a :step-limit ExceptionInfo is thrown.
  Default &STLIMIT is 2147483647 (effectively unlimited).
  Tests set it low to catch infinite loops without waiting for wall-clock timeout.

  ## &TRACE / TRACE hooks  (18B TRACE feature)
  LABEL traces fire at every statement whose key has a registration.
  Statement-level trace (wildcard '*') prints each statement number + label.
  Pattern traces are handled in match.clj via *trace* dynamic var."
  (:require [SNOBOL4clojure.env   :refer [<STNO> <LABL> <CODE>
                                           &STCOUNT &STLIMIT &TRACE
                                           snobol-return! snobol-freturn!
                                           snobol-nreturn! snobol-end!
                                           snobol-steplimit!]]
            [SNOBOL4clojure.trace :refer [fire-label! statement-trace-active?]]
            [SNOBOL4clojure.operators :refer [EVAL!]]))

;; Special goto targets that trigger control-flow signals rather than label jumps.
(def ^:private special-targets
  #{"RETURN" "return" "FRETURN" "freturn" "NRETURN" "nreturn" "END" "end"
    :RETURN :return :FRETURN :freturn :NRETURN :nreturn :END :end})

(defn- dispatch-special! [target]
  (case (clojure.string/upper-case (name target))
    "RETURN"  (snobol-return!)
    "FRETURN" (snobol-freturn!)
    "NRETURN" (snobol-nreturn!)
    "END"     (snobol-end!)))

(defn RUN [at]
  (letfn
    [(skey  [address] (let [[no label] address] (if label label no)))
     (saddr [at]      (cond
                        (keyword? at) [(@<STNO> at) at]
                        (string?  at) [(@<STNO> at) at]
                        (integer? at) [at (@<LABL> at)]))
     (goto! [tgt]
       (if (special-targets tgt)
         (dispatch-special! tgt)
         tgt))]
    (reset! &STCOUNT 0)
    (try
      (loop [current (saddr at)]
        (when-let [key (skey current)]
          ;; Special-label check (e.g. labelled RETURN/END)
          (when (special-targets key)
            (dispatch-special! key))
          (when-let [stmt (@<CODE> key)]
            ;; ── &STCOUNT / &STLIMIT ──────────────────────────────────────────
            (let [n (swap! &STCOUNT inc)]
              (when (> n @&STLIMIT)
                (snobol-steplimit! n))
              ;; ── LABEL trace ────────────────────────────────────────────────
              (when (pos? @&TRACE)
                (fire-label! key)
                (when (statement-trace-active?)
                  (binding [*out* *err*]
                    (println (format ">>> stmt %5d  %s" (dec n) (str key)))))))
            ;; ── Execute statement ────────────────────────────────────────────
            (let [ferst  (first stmt)
                  seqond (second stmt)
                  body   (if (map? ferst) seqond ferst)
                  goto   (if (map? ferst) ferst  seqond)]
              (if (EVAL! body)
                ;; success branch
                (let [tgt (or (:G goto) (when (contains? goto :S) (:S goto)))]
                  (if tgt
                    (do (goto! tgt) (recur (saddr tgt)))
                    (recur (saddr (inc (current 0))))))
                ;; failure branch
                (let [tgt (or (:G goto) (when (contains? goto :F) (:F goto)))]
                  (if tgt
                    (do (goto! tgt) (recur (saddr tgt)))
                    (recur (saddr (inc (current 0)))))))))))
      (catch clojure.lang.ExceptionInfo e
        (case (get (ex-data e) :snobol/signal)
          :end        nil          ; normal program end
          :return     nil          ; bubble up to DEFINE wrapper
          :step-limit (throw e)    ; propagate — caught by test-helpers/harness
          :freturn    (throw e)    ; bubble up to DEFINE wrapper
          :nreturn    (throw e)    ; bubble up to DEFINE wrapper
          (throw e))))))
