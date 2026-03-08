(ns SNOBOL4clojure.compiler
  ;; CODE! / CODE: compile SNOBOL4 source text into a labeled statement table
  ;; and load it into the runtime's global statement store.
  (:require [clojure.string          :as string]
            [SNOBOL4clojure.env      :refer [STNO <STNO> <LABL> <CODE>]]
            [SNOBOL4clojure.grammar  :refer [parse-statement]]
            [SNOBOL4clojure.emitter  :refer [emitter]]))

;; ── Source tokenizer ──────────────────────────────────────────────────────────
(defn- comment? [cmd] (re-find #"^\*" cmd))
(defn- control? [cmd] (re-find #"^\-" cmd))
(defn- error-node [info] (list 'ERROR (:line info) (:column info) (:text info)))

(defn- split-source
  "Split SNOBOL4 source into raw statement strings.
   Handles:
   - Newline as EOS
   - Semicolon as EOS only when NOT inside a quoted string (single or double quotes)
   - Continuation lines beginning with + or . (appended to previous statement)
   Returns a seq of raw strings (without trailing EOS char)."
  [src]
  (let [chars (str src "\n")
        n     (count chars)]
    (loop [i 0 cur (StringBuilder.) stmts [] in-sq false in-dq false]
      (if (>= i n)
        (let [s (string/trimr (str cur))]
          (if (seq s) (conj stmts s) stmts))
        (let [ch (.charAt chars i)]
          (cond
            ;; Toggle single-quote state
            (and (= ch \') (not in-dq))
            (recur (inc i) (.append cur ch) stmts (not in-sq) in-dq)
            ;; Toggle double-quote state
            (and (= ch \") (not in-sq))
            (recur (inc i) (.append cur ch) stmts in-sq (not in-dq))
            ;; Semicolon outside quotes = EOS
            (and (= ch \;) (not in-sq) (not in-dq))
            (let [s (string/trimr (str cur))]
              (recur (inc i) (StringBuilder.) (if (seq s) (conj stmts s) stmts) false false))
            ;; Newline
            (= ch \newline)
            (let [s (string/trimr (str cur))]
              (if (seq s)
                ;; Look ahead: is next line a continuation (+/.)?
                (let [next-i (inc i)
                      next-ch (when (< next-i n) (.charAt chars next-i))]
                  (if (and next-ch (or (= next-ch \+) (= next-ch \.)))
                    ;; Continuation: consume + or . and keep building
                    (recur (+ i 2) (.append cur \space) stmts false false)
                    ;; Real EOS
                    (recur (inc i) (StringBuilder.) (conj stmts s) false false)))
                (recur (inc i) (StringBuilder.) stmts false false)))
            :else
            (recur (inc i) (.append cur ch) stmts in-sq in-dq)))))))

;; ── CODE! — parse source into {CODES NOS LABELS} ─────────────────────────────
(defn CODE! [S]
  (let [raw-stmts (split-source (str S))]
    (loop [stmts raw-stmts NO 1 CODES {} NOS {} LABELS {}]
      (if (empty? stmts)
        [CODES NOS LABELS]
        (let [command (first stmts)]
          (cond
            (comment? command)(recur (rest stmts) NO CODES NOS LABELS)
            (control? command)(recur (rest stmts) NO CODES NOS LABELS)
            :else
            (let [stmt   (string/replace command #"\r" "")
                  ast    (parse-statement stmt)
                  code   (emitter ast)]
              (if (and (map? code) (:reason code))
                (recur (rest stmts) (inc NO) (assoc CODES NO (error-node code)) NOS LABELS)
                (let [label  (:label code)
                      body   (:body  code)
                      goto   (:goto  code)
                      key    (if label label NO)
                      code   (reduce #(conj %1 %2) [] [body goto])
                      nos    (if (keyword? key) (assoc NOS   key NO) NOS)
                      labels (if (keyword? key) (assoc LABELS NO key) LABELS)
                      codes  (assoc CODES key code)]
                  (recur (rest stmts) (inc NO) codes nos labels))))))))))

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
