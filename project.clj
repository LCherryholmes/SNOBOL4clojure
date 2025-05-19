(defproject SNOBOL4clojure "0.2.0"
  :description "SNOBOL4 pattern matching."
  :url ""
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
    [org.clojure/clojure "1.10.3"]
    [org.clojure/tools.trace "0.7.11"]
  ; [org.clojure/math.numeric-tower "0.0.4"]
  ; [aysylu/loom "1.0.2"]
    [instaparse "1.4.10"]
    [net.mikera/core.matrix "0.62.0"]
  ; [expresso "0.2.2"]
    [criterium "0.4.6"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :main ^:skip-aot SNOBOL4clojure.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
