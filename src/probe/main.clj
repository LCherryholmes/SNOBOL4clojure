(ns probe.main
  (:require [SNOBOL4clojure.compiler :as c]
            [SNOBOL4clojure.env :as env]
            [SNOBOL4clojure.test-helpers :refer [run-with-timeout]]
            [SNOBOL4clojure.core]))
(defn -main [& _]
  (env/GLOBALS (create-ns 'probe.env))
  (let [src (c/preprocess-includes "-INCLUDE 'testpgms-test4.spt'" ["corpus/spitbol"])
        _ (println "Source line count:" (count (clojure.string/split-lines src)))
        [codes _ _] (c/CODE! src)
        _ (println "Statement count:" (count codes))
        r (run-with-timeout src 15000)]
    (println "exit:" (:exit r))
    (println "stdout nil?" (nil? (:stdout r)))
    (when (:thrown r) (println "thrown:" (:thrown r))))
  (System/exit 0))
