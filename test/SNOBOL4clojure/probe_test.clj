(ns SNOBOL4clojure.probe-test
  (:require [clojure.test :refer :all]
            [SNOBOL4clojure.compiler :refer [CODE]]
            [SNOBOL4clojure.runtime :refer [RUN]]
            [SNOBOL4clojure.env :as env]))

(defn reset-rt! []
  (reset! env/STNO 0)
  (reset! env/<STNO> {})
  (reset! env/<LABL> {})
  (reset! env/<CODE> {}))

(deftest probe-6E
  (reset-rt!)
  ;; Standard SNOBOL4: jump past function body after DEFINE
  (let [prog "         DEFINE('DOUBLE(N)')       :(EXEC)\nDOUBLE   DOUBLE = N + N             :(RETURN)\nEXEC     OUTPUT = DOUBLE(3)\n         OUTPUT = DOUBLE(7)\nEND\n"]
    (let [out (with-out-str (RUN (CODE prog)))]
      (println "6E:" (pr-str out))
      (is (= "6\n14\n" out)))))
