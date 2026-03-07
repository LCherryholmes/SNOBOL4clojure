(ns SNOBOL4clojure.compiler
  ;; CODE! / CODE: compile SNOBOL4 source text into a labeled statement table
  ;; and load it into the runtime's global statement store.
  (:require [clojure.string          :as string]
            [SNOBOL4clojure.env      :refer [STNO <STNO> <LABL> <CODE>]]
            [SNOBOL4clojure.grammar  :refer [parse-statement]]
            [SNOBOL4clojure.emitter  :refer [emitter]]))

;; ── Source tokenizer regexes ──────────────────────────────────────────────────
(def ^:private eol     #"[\n]")
(def ^:private eos     #"[;\n]")
(def ^:private skip    #"[^\n]*")
(def ^:private tokens  #"[^;\n]*")

(defn- re-cat [& rexes] (re-pattern (apply str rexes)))

(def ^:private komment (re-cat #"[*]" skip eol))
(def ^:private control (re-cat #"[-]" tokens eos))
(def ^:private kode    (re-cat #"[^;\n.+*-]" tokens "(" #"\n[.+]" tokens ")*" eos))
(def ^:private block   (re-cat komment "|" control "|" kode "|" eol))

(defn- comment? [cmd] (re-find #"^\*" cmd))
(defn- control? [cmd] (re-find #"^\-" cmd))
(defn- error-node [info] (list 'ERROR (:line info) (:column info) (:text info)))

;; ── CODE! — parse source into {CODES NOS LABELS} ─────────────────────────────
(defn CODE! [S]
  (let [blocks (re-seq block (str S "\n"))]
    (loop [block blocks NO 1 CODES {} NOS {} LABELS {}]
      (let [command (first (first block))]
        (cond
          (nil? command)    [CODES NOS LABELS]
          (comment? command)(recur (rest block) NO CODES NOS LABELS)
          (control? command)(recur (rest block) NO CODES NOS LABELS)
          true
          (let [stmt   (-> command
                           (string/replace #"[ \t]*\r?\n[+.][ \t]*" " ")
                           (string/replace #"\r?\n$" ""))
                ast    (parse-statement stmt)
                code   (emitter ast)]
            (if (and (map? code) (:reason code))
              (recur (rest block) (inc NO) (assoc CODES NO (error-node code)) NOS LABELS)
              (let [label  (:label code)
                    body   (:body  code)
                    goto   (:goto  code)
                    key    (if label label NO)
                    code   (reduce #(conj %1 %2) [] [body goto])
                    nos    (if (keyword? key) (assoc NOS   key NO) NOS)
                    labels (if (keyword? key) (assoc LABELS NO key) LABELS)
                    codes  (assoc CODES key code)]
                (recur (rest block) (inc NO) codes nos labels)))))))))

;; ── CODE — compile and load into the global statement store ──────────────────
(defn CODE [S]
  (let [[codes nos labels] (CODE! S)
        start              (inc @STNO)]
    (loop [NO 1]
      (if (> NO (count codes))
        (if (and (@<LABL> start) (@<CODE> (@<LABL> start)))
          (@<LABL> start)
          (when (@<CODE> start) start))
        (do
          (swap! STNO inc)
          (if-let [label (labels NO)]
            (do
              (swap! <CODE> #(assoc % label (codes label)))
              (swap! <LABL> #(assoc % @STNO label))
              (swap! <STNO> #(assoc % label @STNO)))
            (swap! <CODE> #(assoc % @STNO (codes NO))))
          (recur (inc NO)))))))
