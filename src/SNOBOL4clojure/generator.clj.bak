(ns SNOBOL4clojure.generator
  "Worm generator for SNOBOL4 test programs.

   Design principles
   ─────────────────
   • Typed variable pools give programs a legible, idiomatic appearance:
       Labels   — L1, L2, L3 …
       Integers — I, J, K, L, M, N
       Reals    — A, B, C, D, E, F
       Strings  — S, T, X, Y, Z
       Patterns — P, Q, R
   • A small fixed vocabulary of integer and real literals keeps arithmetic
     tractable and avoids edge-cases (no zeros as divisors, no overflows).
   • Programs are assembled from a linear sequence of typed *moves*
     (assignment, output, arithmetic, comparison+branch, pattern match/replace,
     string concat, loop, DEFINE/call).  Each move is independently valid
     SNOBOL4; the sequence as a whole is a complete runnable program.
   • 'Worm' means the generator grows the program one statement at a time,
     threading state (which variables hold values, which labels exist) so that
     later statements can safely reference earlier ones.
   • Two tiers — rand-* (probabilistic) and gen-* (exhaustive lazy sequences).
  "
  (:require [clojure.string :as str]))

;; ── Fixed vocabulary ──────────────────────────────────────────────────────────

(def int-vars    '[I J K L M N])
(def real-vars   '[A B C D E F])
(def str-vars    '[S T X Y Z])
(def pat-vars    '[P Q R])

;; Literals that stay well-behaved (no div-by-zero, no overflow)
(def int-lits    [1 2 3 4 5 6 7 8 9 10 25 100])
(def real-lits   [1.0 1.5 2.0 2.5 3.0 3.14 0.5 10.0])
(def str-lits    ["'alpha'" "'beta'" "'gamma'" "'hello'" "'world'"
                  "'foo'" "'bar'" "'baz'" "'SNOBOL'" "'test'"])
;; Simple patterns (no capture, just literal and primitive patterns)
(def pat-lits    ["'a'" "'e'" "'o'" "'l'" "'o'"
                  "ANY('aeiou')" "SPAN('abcdefghijklmnopqrstuvwxyz')"
                  "LEN(1)" "LEN(2)" "LEN(3)"])

;; Arithmetic ops safe for integers
(def arith-ops   ["+" "-" "*"])
;; Comparison ops (result: success/failure)
(def cmp-ops-int ["EQ" "NE" "GT" "LT" "GE" "LE"])
(def cmp-ops-str ["IDENT" "DIFFER"])

;; ── Formatting helpers ────────────────────────────────────────────────────────

(defn- indent
  "Emit a statement with 8-space indent (no label)."
  [body]
  (str "        " body))

(defn- labelled
  "Emit a statement with a label in column 1 (padded to 8 chars)."
  [lbl body]
  (let [pad (max 1 (- 8 (count (name lbl))))]
    (str (name lbl) (apply str (repeat pad " ")) body)))

(defn- goto-s  [lbl] (str " :S(" (name lbl) ")"))
(defn- goto-f  [lbl] (str " :F(" (name lbl) ")"))
(defn- goto-sf [sl fl] (str " :S(" (name sl) ")F(" (name fl) ")"))

;; ── Worm state ────────────────────────────────────────────────────────────────
;;
;; State tracks which typed variables have been initialised and which labels
;; exist, so later moves only reference variables that are live.

(defn- fresh-state []
  {:lines     []        ; accumulated source lines
   :live-int  #{}       ; integer vars that have been assigned
   :live-real #{}       ; real vars
   :live-str  #{}       ; string vars
   :live-pat  #{}       ; pattern vars
   :labels    #{}       ; defined labels
   :next-lbl  1})       ; counter for L1, L2 …

(defn- next-label [st]
  (let [n (:next-lbl st)
        lbl (symbol (str "L" n))]
    [lbl (update st :next-lbl inc)]))

(defn- emit [st & lines]
  (update st :lines into lines))

(defn- live? [st pool] (seq (get st pool)))

;; ── Individual moves ──────────────────────────────────────────────────────────

(defn- move-assign-int
  "Assign a literal integer to an int var."
  [st rng]
  (let [v   (rand-nth int-vars)
        lit (rand-nth int-lits)]
    (-> st
        (emit (indent (str v " = " lit)))
        (update :live-int conj v))))

(defn- move-assign-real
  "Assign a literal real to a real var."
  [st rng]
  (let [v   (rand-nth real-vars)
        lit (rand-nth real-lits)]
    (-> st
        (emit (indent (str v " = " lit)))
        (update :live-real conj v))))

(defn- move-assign-str
  "Assign a literal string to a string var."
  [st rng]
  (let [v   (rand-nth str-vars)
        lit (rand-nth str-lits)]
    (-> st
        (emit (indent (str v " = " lit)))
        (update :live-str conj v))))

(defn- move-arith
  "Assign integer arithmetic result to an int var.
   Requires at least one live int var or uses literals."
  [st rng]
  (let [op  (rand-nth arith-ops)
        lhs (if (and (live? st :live-int) (< (rand) 0.6))
              (str (rand-nth (vec (:live-int st))))
              (str (rand-nth int-lits)))
        rhs (str (rand-nth int-lits))   ; always a literal on rhs — avoids 0
        v   (rand-nth int-vars)]
    (-> st
        (emit (indent (str v " = " lhs " " op " " rhs)))
        (update :live-int conj v))))

(defn- move-concat
  "Concatenate two string values into a string var."
  [st rng]
  (let [live (vec (:live-str st))
        lhs  (if (and (seq live) (< (rand) 0.6))
               (str (rand-nth live))
               (rand-nth str-lits))
        rhs  (if (and (seq live) (< (rand) 0.5))
               (str (rand-nth live))
               (rand-nth str-lits))
        v    (rand-nth str-vars)]
    (-> st
        (emit (indent (str v " = " lhs " " rhs)))
        (update :live-str conj v))))

(defn- move-output-int [st rng]
  (when (live? st :live-int)
    (let [v (rand-nth (vec (:live-int st)))]
      (emit st (indent (str "OUTPUT = " v))))))

(defn- move-output-str [st rng]
  (let [src (if (and (live? st :live-str) (< (rand) 0.7))
              (str (rand-nth (vec (:live-str st))))
              (rand-nth str-lits))]
    (emit st (indent (str "OUTPUT = " src)))))

(defn- move-output-lit [st rng]
  (emit st (indent (str "OUTPUT = " (rand-nth str-lits)))))

(defn- move-cmp-branch
  "Comparison with :S/:F branch to a fresh label pair with convergence."
  [st rng]
  (when (live? st :live-int)
    (let [v          (rand-nth (vec (:live-int st)))
          op         (rand-nth cmp-ops-int)
          lit        (rand-nth int-lits)
          [ls st]    (next-label st)
          [lf st]    (next-label st)
          [lskip st] (next-label st)]
      (-> st
          (emit (indent (str op "(" v "," lit ")" (goto-sf ls lf))))
          (emit (labelled ls (str "OUTPUT = '" (name ls) " branch'")))
          (emit (indent (str ":(" (name lskip) ")")))
          (emit (labelled lf (str "OUTPUT = '" (name lf) " branch'")))
          (emit (labelled lskip ""))
          (update :labels conj ls lf lskip)))))

(defn- move-pat-assign
  "Assign a pattern literal to a pattern var."
  [st rng]
  (let [v   (rand-nth pat-vars)
        lit (rand-nth pat-lits)]
    (-> st
        (emit (indent (str v " = " lit)))
        (update :live-pat conj v))))

(defn- move-pat-match
  "Match a pattern against a string var (success/failure branch)."
  [st rng]
  (let [subj (if (and (live? st :live-str) (< (rand) 0.7))
               (str (rand-nth (vec (:live-str st))))
               (rand-nth str-lits))
        pat  (if (and (live? st :live-pat) (< (rand) 0.6))
               (str (rand-nth (vec (:live-pat st))))
               (rand-nth pat-lits))
        [ls st] (next-label st)
        [lf st] (next-label st)
        [lskip st] (next-label st)]
    (-> st
        (emit (indent (str subj " " pat (goto-sf ls lf))))
        (emit (labelled ls "OUTPUT = 'matched'"))
        (emit (indent (str ":(" (name lskip) ")")))
        (emit (labelled lf "OUTPUT = 'no match'"))
        (emit (labelled lskip ""))
        (update :labels conj ls lf lskip))))

(defn- move-pat-replace
  "Pattern replace: subj PAT = repl, then output the subject."
  [st rng]
  (when (live? st :live-str)
    (let [v    (rand-nth (vec (:live-str st)))
          pat  (if (and (live? st :live-pat) (< (rand) 0.5))
                 (str (rand-nth (vec (:live-pat st))))
                 (rand-nth pat-lits))
          repl (rand-nth str-lits)]
      (-> st
          (emit (indent (str v " " pat " = " repl)))
          (emit (indent (str "OUTPUT = " v)))))))

(defn- move-size
  "Output SIZE of a string."
  [st rng]
  (when (live? st :live-str)
    (let [v (rand-nth (vec (:live-str st)))]
      (emit st (indent (str "OUTPUT = SIZE(" v ")"))))))

(defn- move-loop
  "Counted loop: initialise I, loop body outputs I, increment, branch back."
  [st rng]
  (let [limit    (rand-nth [3 4 5])
        counter  'I
        [lloop st] (next-label st)
        [lend  st] (next-label st)]
    (-> st
        (emit (indent (str counter " = 1")))
        (emit (labelled lloop (str "OUTPUT = " counter)))
        (emit (indent (str counter " = " counter " + 1")))
        (emit (indent (str "LE(" counter "," limit ") :S(" (name lloop) ")")))
        (update :live-int conj counter)
        (update :labels conj lloop lend))))

;; ── Move table ────────────────────────────────────────────────────────────────

(def ^:private all-moves
  [;; always available
   {:w 10 :needs nil      :fn move-assign-int}
   {:w 8  :needs nil      :fn move-assign-real}
   {:w 10 :needs nil      :fn move-assign-str}
   {:w 6  :needs nil      :fn move-output-lit}
   {:w 5  :needs nil      :fn move-pat-assign}
   ;; need at least one live var
   {:w 8  :needs :live-int  :fn move-arith}
   {:w 8  :needs :live-str  :fn move-concat}
   {:w 7  :needs :live-int  :fn move-output-int}
   {:w 7  :needs :live-str  :fn move-output-str}
   {:w 5  :needs :live-int  :fn move-cmp-branch}
   {:w 4  :needs :live-str  :fn move-pat-match}
   {:w 4  :needs :live-str  :fn move-pat-replace}
   {:w 3  :needs :live-str  :fn move-size}
   {:w 3  :needs nil         :fn move-loop}])

(defn- eligible-moves [st]
  (filter (fn [{:keys [needs]}]
            (or (nil? needs) (live? st needs)))
          all-moves))

(defn- weighted-rand [moves]
  (let [total (reduce + (map :w moves))
        r     (* (rand) total)]
    (loop [remaining moves
           acc       0]
      (let [{:keys [w fn]} (first remaining)
            acc (+ acc w)]
        (if (or (>= acc r) (= 1 (count remaining)))
          fn
          (recur (rest remaining) acc))))))

;; ── Program assembly ─────────────────────────────────────────────────────────

(defn- finalise
  "Add the END label and join lines."
  [st]
  (str/join "\n" (conj (:lines st) "end")))

(defn rand-program
  "Generate a random SNOBOL4 program with n-moves statements.
   Returns source string."
  ([] (rand-program (+ 3 (rand-int 6))))
  ([n-moves]
   (loop [st  (fresh-state)
          n   n-moves]
     (if (zero? n)
       (finalise st)
       (let [moves    (eligible-moves st)
             move-fn  (weighted-rand moves)
             new-st   (move-fn st nil)]
         (recur (or new-st st) (dec n)))))))

;; ── Exhaustive (gen-*) tier ───────────────────────────────────────────────────
;; Simple systematic sequences — every combination at a given complexity level.

(defn gen-assign-int
  "Lazy seq of all (var = lit) integer assignments."
  []
  (for [v int-vars, lit int-lits]
    (str (indent (str v " = " lit)) "\n"
         (indent (str "OUTPUT = " v)) "\n"
         "end")))

(defn gen-assign-str
  "Lazy seq of all (var = lit) string assignments + output."
  []
  (for [v str-vars, lit str-lits]
    (str (indent (str v " = " lit)) "\n"
         (indent (str "OUTPUT = " v)) "\n"
         "end")))

(defn gen-arith
  "Lazy seq of all (var = lhs op rhs) arithmetic programs."
  []
  (for [v   int-vars
        op  arith-ops
        lhs int-lits
        rhs (filter pos? int-lits)]   ; no rhs zero for safety
    (str (indent (str v " = " lhs " " op " " rhs)) "\n"
         (indent (str "OUTPUT = " v)) "\n"
         "end")))

(defn gen-concat
  "Lazy seq of all (var = s1 s2) concatenation programs."
  []
  (for [v  str-vars
        s1 str-lits
        s2 str-lits
        :when (not= s1 s2)]
    (str (indent (str v " = " s1 " " s2)) "\n"
         (indent (str "OUTPUT = " v)) "\n"
         "end")))

(defn gen-pat-match
  "Lazy seq of pattern-match programs (string lit vs pattern lit)."
  []
  (for [s str-lits
        p pat-lits]
    (str (indent (str "S = " s)) "\n"
         (indent (str "S " p " :S(HIT)F(MISS)")) "\n"
         "HIT     OUTPUT = 'matched'\n"
         "        :(DONE)\n"
         "MISS    OUTPUT = 'no match'\n"
         "DONE\n"
         "end")))

(defn gen-cmp
  "Lazy seq of integer comparison programs."
  []
  (for [op  cmp-ops-int
        lhs int-lits
        rhs int-lits
        :when (not= lhs rhs)]
    (str (indent (str op "(" lhs "," rhs ") :S(YES)F(NO)")) "\n"
         "YES     OUTPUT = 'yes'\n"
         "        :(DONE)\n"
         "NO      OUTPUT = 'no'\n"
         "DONE\n"
         "end")))

;; ── Batch generation ─────────────────────────────────────────────────────────

(defn rand-batch
  "Generate n random programs of varying size."
  [n]
  (repeatedly n rand-program))

(defn systematic-batch
  "Return a lazy seq of all systematic (gen-*) programs."
  []
  (concat
    (gen-assign-int)
    (gen-assign-str)
    (gen-arith)
    (gen-concat)
    (gen-cmp)
    (gen-pat-match)))
