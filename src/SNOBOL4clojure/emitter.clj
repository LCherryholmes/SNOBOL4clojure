(ns SNOBOL4clojure.emitter
  ;; Transform an instaparse AST into Clojure data (the SNOBOL4 IR).
  (:require [clojure.edn :as edn]
            [SNOBOL4clojure.env      :refer [subtract]]
            [SNOBOL4clojure.grammar  :refer [parse-expression]]
            [instaparse.core         :as insta]))

(defn emitter [ast]
  (insta/transform
    {:comment   (fn [cmt]    [:comment cmt])
     :control   (fn [ctl]    [:control ctl])
     :stmt      (fn [& ss]   (apply conj ss))
     :label     (fn [L]      {:label (if (re-find #"^[0-9A-Z_a-z]+$" L)
                                       (keyword L) (str L))})
     :body      (fn [B]      {:body B})
     :invoking  (fn [S]      S)
     :matching  (fn [S P]    (list '? S P))
     :replacing (fn
                  ([S P]     (list '?= S P 'epsilon))
                  ([S P R]   (list '?= S P R)))
     :assigning (fn
                  ([S]       (list '= S 'epsilon))
                  ([S R]     (list '= S R)))
     :goto      (fn [& gs]   {:goto (reduce
                                      (fn [bs b]
                                        (let [key (first b) tgt (second b)]
                                          (assoc bs key
                                            (if (symbol? tgt) (keyword tgt) tgt))))
                                      {} gs)})
     :jmp       (fn [L]      [:G L])
     :sjmp      (fn [L]      [:S L])
     :fjmp      (fn [L]      [:F L])
     :expr      (fn [x]      x)
     :asn       (fn
                  ([x]       x)
                  ([x y]     (list '= x y)))
     :mch       (fn
                  ([x]       x)
                  ([x y]     (list '? x y))
                  ([x y z]   (list '?= x y z)))
     :and       (fn
                  ([x]       x)
                  ([x y]     (list '& x y)))
     :alt       (fn
                  ([x]       x)
                  ([x & ys]  (apply list '| x ys)))
     :cat       (fn
                  ([x]       x)
                  ([x & ys]  (apply vector x ys)))
     :at        (fn
                  ([x]       x)
                  ([x y]     (list 'at x y)))
     :sum       (fn
                  ([x]       x)
                  ([x op y]  (list (symbol op) x y)))
     :hsh       (fn
                  ([x]       x)
                  ([x y]     (list 'sharp x y)))
     :div       (fn
                  ([x]       x)
                  ([x y]     (list '/ x y)))
     :mul       (fn
                  ([x]       x)
                  ([x y]     (list '* x y)))
     :pct       (fn
                  ([x]       x)
                  ([x y]     (list '% x y)))
     :xp        (fn
                  ([x]       x)
                  ([x op y]  (list (symbol op) x y)))
     :cap       (fn
                  ([x]       x)
                  ([x op y]  (list (symbol op) x y)))
     :ttl       (fn
                  ([x]       x)
                  ([x y]     (list 'tilde x y)))
     :uop       (fn
                  ([x]       x)
                  ([op y]    (case op
                               "@" (list 'at y)
                               "#" (list 'sharp y)
                               "~" (list 'tilde y)
                                   (list (symbol op) y))))
     :ndx       (fn
                  ([n]       n)
                  ([n & xs]  (apply list n xs)))
     :cnd       (fn
                  ([x]       x)
                  ([x & ys]  (apply vector 'comma x ys)))
     :inv       (fn [f & xs] (apply list f xs))
     :N         (fn [n]      (symbol n))
     :S         (fn [s]      (subs s 1 (subtract (count s) 1)))
     :I         edn/read-string
     :R         (fn [s] (Double/parseDouble s))}
    ast))

(defn parse+emit
  "Parse a SNOBOL4 expression string and emit the Clojure IR."
  [s]
  (first (emitter (parse-expression s))))
