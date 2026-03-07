(ns SNOBOL4clojure.runtime
  ;; The SNOBOL4 GOTO-driven statement interpreter.
  ;; Walks the loaded statement table produced by CODE, evaluating
  ;; each statement body and dispatching on success/failure gotos.
  (:require [SNOBOL4clojure.env      :refer [<STNO> <LABL> <CODE>]]
            [SNOBOL4clojure.operators :refer [EVAL!]]))

(defn RUN [at]
  (letfn
    [(skey  [address] (let [[no label] address] (if label label no)))
     (saddr [at]      (cond
                        (keyword? at) [(@<STNO> at) at]
                        (string?  at) [(@<STNO> at) at]
                        (integer? at) [at (@<LABL> at)]))]
    (loop [current (saddr at)]
      (when-let [key (skey current)]
        (when-let [stmt (@<CODE> key)]
          (let [ferst  (first stmt)
                seqond (second stmt)
                body   (if (map? ferst) seqond ferst)
                goto   (if (map? ferst) ferst  seqond)]
            (if (EVAL! body)
              (if (contains? goto :G) (recur (saddr (:G goto)))
                (if (contains? goto :S) (recur (saddr (:S goto)))
                  (recur (saddr (inc (current 0))))))
              (if (contains? goto :G) (recur (saddr (:G goto)))
                (if (contains? goto :F) (recur (saddr (:F goto)))
                  (recur (saddr (inc (current 0)))))))))))))
