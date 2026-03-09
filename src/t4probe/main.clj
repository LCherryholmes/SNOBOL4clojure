(ns t4probe
  (:require [SNOBOL4clojure.compiler :as c]
            [SNOBOL4clojure.env :as env]
            [SNOBOL4clojure.core]))
(defn -main [& _]
  (env/GLOBALS (create-ns 't4probe.env))
  (let [src (c/preprocess-includes "-INCLUDE 'testpgms-test4.spt'" ["corpus/spitbol"])
        [codes nos labels] (c/CODE! src)]
    (doseq [[k v] codes]
      (when (not (map? (second v)))
        (when (some? (second v))
          (println "NON-MAP GOTO:" k "->" v (type (second v)))))))
  (System/exit 0))
