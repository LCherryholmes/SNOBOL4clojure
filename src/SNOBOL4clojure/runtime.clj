(ns SNOBOL4clojure.runtime
  ;; The SNOBOL4 GOTO-driven statement interpreter.
  ;; Walks the loaded statement table produced by CODE, evaluating
  ;; each statement body and dispatching on success/failure gotos.
  (:require [SNOBOL4clojure.env      :refer [<STNO> <LABL> <CODE>
                                              snobol-return! snobol-freturn!
                                              snobol-nreturn! snobol-end!]]
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
       ;; tgt is a keyword (from emitter) or string/integer
       (if (special-targets tgt)
         (dispatch-special! tgt)
         tgt))]
    (try
      (loop [current (saddr at)]
        (when-let [key (skey current)]
          ;; Check for special label at the *current* position (e.g. labelled RETURN)
          (when (special-targets key)
            (dispatch-special! key))
          (when-let [stmt (@<CODE> key)]
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
          :end     nil   ; normal program end
          :return  nil   ; bubble up to DEFINE wrapper
          :freturn (throw e)  ; bubble up to DEFINE wrapper
          :nreturn (throw e)  ; bubble up to DEFINE wrapper
          (throw e))))))

