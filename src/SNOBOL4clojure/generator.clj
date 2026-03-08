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

;; ── Sprint 18: gen-by-length — length-band grammar worm ──────────────────────
;;
;; Design: generate all syntactically valid programs at each character-length
;; band (or up to N samples for larger bands).  The canonical variable pools
;; and fixture initialisation are always prepended so every program is
;; self-contained and runnable by the harness.

(def ^:private canonical-fixtures
  "Standard fixture preamble — initialises all typed pools."
  (str/join "\n"
    ["        I = 0"
     "        J = 1"
     "        K = 2"
     "        L = 3"
     "        M = 4"
     "        N = 5"
     "        S = 'hello'"
     "        T = 'world'"
     "        X = 'foo'"
     "        Y = 'bar'"
     "        Z = 'baz'"]))

(defn- with-fixtures
  "Wrap body lines with canonical fixtures + END."
  [& body-lines]
  (str canonical-fixtures "\n"
       (str/join "\n" body-lines) "\n"
       "END"))

;; ── Length-band building blocks ───────────────────────────────────────────────
;; Each builder returns a seq of (body-line ...) vectors; caller wraps fixtures.

(defn- band-assign-int []
  (for [v int-vars lit int-lits]
    [(indent (str v " = " lit))
     (indent (str "OUTPUT = " v))]))

(defn- band-assign-str []
  (for [v str-vars lit str-lits]
    [(indent (str v " = " lit))
     (indent (str "OUTPUT = " v))]))

(defn- band-arith []
  (for [v int-vars op arith-ops lhs int-lits rhs (filter pos? int-lits)]
    [(indent (str v " = " lhs " " op " " rhs))
     (indent (str "OUTPUT = " v))]))

(defn- band-cmp []
  (for [op cmp-ops-int lhs int-lits rhs int-lits :when (not= lhs rhs)]
    [(indent (str op "(" lhs "," rhs ") :S(YES)F(NO)"))
     "YES     OUTPUT = 'yes'"
     "        :(DONE)"
     "NO      OUTPUT = 'no'"
     "DONE"]))

(defn- band-concat []
  (for [v str-vars s1 str-lits s2 str-lits :when (not= s1 s2)]
    [(indent (str v " = " s1 " " s2))
     (indent (str "OUTPUT = " v))]))

(defn- band-size []
  (for [v str-vars]
    [(indent (str "OUTPUT = SIZE(" v ")"))]))

(defn- band-pat-match []
  (for [s str-vars p pat-lits]
    [(indent (str s " " p " :S(HIT)F(MISS)"))
     "HIT     OUTPUT = 'matched'"
     "        :(DONE)"
     "MISS    OUTPUT = 'no match'"
     "DONE"]))

(defn- band-pat-replace []
  (for [v str-vars p pat-lits repl str-lits]
    [(indent (str v " " p " = " repl))
     (indent (str "OUTPUT = " v))]))

(defn- band-loop []
  ;; Bounded loops I=1..N, N in {3,4,5}
  (for [limit [3 4 5]]
    ["        I = 1"
     (str "LOOP    OUTPUT = I")
     "        I = I + 1"
     (str "        LE(I," limit ") :S(LOOP)")]))

(defn- band-ident []
  (for [s1 str-lits s2 str-lits :when (not= s1 s2)]
    [(indent (str "IDENT(" s1 "," s2 ") :S(YES)F(NO)"))
     "YES     OUTPUT = 'same'"
     "        :(DONE)"
     "NO      OUTPUT = 'different'"
     "DONE"]))

(defn- band-len-match []
  (for [n [1 2 3 4 5] v str-vars]
    [(indent (str v " LEN(" n ") :S(HIT)F(MISS)"))
     "HIT     OUTPUT = 'matched'"
     "        :(DONE)"
     "MISS    OUTPUT = 'no match'"
     "DONE"]))

(defn- band-any-match []
  (for [cs ["'aeiou'" "'bcdfg'" "'0123456789'"] v str-vars]
    [(indent (str v " ANY(" cs ") :S(HIT)F(MISS)"))
     "HIT     OUTPUT = 'matched'"
     "        :(DONE)"
     "MISS    OUTPUT = 'no match'"
     "DONE"]))

(defn- band-span-match []
  (for [cs ["'abcdefghijklmnopqrstuvwxyz'" "'0123456789'" "'helo'"] v str-vars]
    [(indent (str v " SPAN(" cs ") :S(HIT)F(MISS)"))
     "HIT     OUTPUT = 'matched'"
     "        :(DONE)"
     "MISS    OUTPUT = 'no match'"
     "DONE"]))

(defn- band-break-match []
  (for [cs ["'o'" "'l'" "'d'"] v str-vars]
    [(indent (str v " BREAK(" cs ") . T :S(HIT)F(MISS)"))
     "HIT     OUTPUT = T"
     "        :(DONE)"
     "MISS    OUTPUT = 'no match'"
     "DONE"]))

(defn- band-capture []
  (for [v str-vars p ["LEN(3)" "LEN(2)" "SPAN('abcdefghijklmnopqrstuvwxyz')" "ANY('aeiou')"]]
    [(indent (str v " " p " . T :S(HIT)F(MISS)"))
     "HIT     OUTPUT = T"
     "        :(DONE)"
     "MISS    OUTPUT = 'no match'"
     "DONE"]))

(defn- band-trim []
  (for [v str-vars]
    [(indent (str "OUTPUT = TRIM(" v ")"))]))

(defn- band-substr-cap []
  ;; Capture a substring then output it
  (for [v str-vars]
    [(indent (str v " LEN(3) $ T :S(HIT)F(MISS)"))
     "HIT     OUTPUT = T"
     "        :(DONE)"
     "MISS    OUTPUT = 'no match'"
     "DONE"]))

(defn- band-define-simple []
  ;; Simple DEFINE / call — factorial via iteration
  [["        DEFINE('DOUBLE(X)')"
    "        I = DOUBLE(5)"
    "        OUTPUT = I"
    "        :(END)"
    "DOUBLE  DOUBLE = X * 2 :S(RETURN)F(FRETURN)"
    "END"]])   ; NOTE: these are full programs, not body-lines

;; ── Assemble band programs ────────────────────────────────────────────────────

(defn- bodies->programs
  "Convert a seq of body-line vectors to full programs (with fixtures)."
  [bodies]
  (map (fn [lines] (apply with-fixtures lines)) bodies))

(defn gen-by-length
  "Lazy seq of [length program-src] pairs sorted by source length.
   Covers every grammatical construct at its simplest.
   Suitable for systematic coverage testing.

   Each program is self-contained and uses canonical fixture variables.
   Programs are emitted in order of increasing source length.

   Usage:
     (take 10 (gen-by-length))   ; first 10 shortest programs
     (filter #(= 1 (:band %)) (gen-by-length-annotated))  ; just T1"
  []
  (let [all-bodies (concat
                     (band-assign-int)
                     (band-assign-str)
                     (band-cmp)
                     (band-ident)
                     (band-arith)
                     (band-concat)
                     (band-size)
                     (band-trim)
                     (band-len-match)
                     (band-any-match)
                     (band-span-match)
                     (band-break-match)
                     (band-capture)
                     (band-substr-cap)
                     (band-pat-match)
                     (band-pat-replace)
                     (band-loop))
        programs (bodies->programs all-bodies)
        ;; Also splice in the standalone define programs (already full programs)
        standalone (map first (band-define-simple))]
    (->> (concat programs standalone)
         (sort-by count)
         (dedupe))))

(defn gen-by-length-annotated
  "Like gen-by-length but returns maps {:src s :length n :band k}
   where band is 0..5 based on source length:
     0  = trivial  (<= 80 chars)
     1  = simple   (<= 140 chars)
     2  = medium   (<= 200 chars)
     3  = complex  (<= 300 chars)
     4  = large    (<= 500 chars)
     5  = very large (> 500 chars)"
  []
  (map (fn [src]
         {:src    src
          :length (count src)
          :band   (cond (<= (count src)  80) 0
                        (<= (count src) 140) 1
                        (<= (count src) 200) 2
                        (<= (count src) 300) 3
                        (<= (count src) 500) 4
                        :else                5)})
       (gen-by-length)))

;; ── rand-statement — random single-statement generator ───────────────────────
;;
;; Generates a complete 1-statement program using the canonical variable pools.
;; The statement body is chosen uniformly at random from all grammatical forms.

(defn rand-statement
  "Generate a random complete SNOBOL4 program consisting of exactly one
   meaningful statement (plus fixture setup and END).
   The statement is chosen uniformly from all grammatical shapes at random.
   Returns source string."
  []
  (let [forms
        [;; T0 — atomic assignments
         (fn [] (indent (str (rand-nth int-vars) " = " (rand-nth int-lits))))
         (fn [] (indent (str (rand-nth str-vars) " = " (rand-nth str-lits))))
         ;; T1 — arithmetic
         (fn [] (let [op (rand-nth arith-ops)
                      lhs (rand-nth int-lits)
                      rhs (rand-nth (filter pos? int-lits))]
                  (indent (str (rand-nth int-vars) " = " lhs " " op " " rhs))))
         ;; T1 — concat
         (fn [] (indent (str (rand-nth str-vars) " = "
                             (rand-nth str-lits) " " (rand-nth str-lits))))
         ;; T1 — output
         (fn [] (indent (str "OUTPUT = " (rand-nth str-vars))))
         (fn [] (indent (str "OUTPUT = " (rand-nth int-vars))))
         ;; T2 — comparisons (self-contained with goto)
         (fn [] (let [op (rand-nth cmp-ops-int)
                      a (rand-nth int-lits) b (rand-nth int-lits)]
                  (str (indent (str op "(" a "," b ") :S(YES)F(NO)")) "\n"
                       "YES     OUTPUT = 'yes'\n"
                       "        :(DONE)\n"
                       "NO      OUTPUT = 'no'\n"
                       "DONE")))
         ;; T2 — string identity
         (fn [] (let [a (rand-nth str-lits) b (rand-nth str-lits)]
                  (str (indent (str "IDENT(" a "," b ") :S(YES)F(NO)")) "\n"
                       "YES     OUTPUT = 'same'\n"
                       "        :(DONE)\n"
                       "NO      OUTPUT = 'different'\n"
                       "DONE")))
         ;; T2 — SIZE
         (fn [] (indent (str "OUTPUT = SIZE(" (rand-nth str-vars) ")")))
         ;; T2 — pattern match
         (fn [] (let [v (rand-nth str-vars) p (rand-nth pat-lits)]
                  (str (indent (str v " " p " :S(HIT)F(MISS)")) "\n"
                       "HIT     OUTPUT = 'matched'\n"
                       "        :(DONE)\n"
                       "MISS    OUTPUT = 'no match'\n"
                       "DONE")))
         ;; T2 — capture
         (fn [] (let [v (rand-nth str-vars)
                      p (rand-nth ["LEN(2)" "LEN(3)" "ANY('aeiou')"])]
                  (str (indent (str v " " p " . T :S(HIT)F(MISS)")) "\n"
                       "HIT     OUTPUT = T\n"
                       "        :(DONE)\n"
                       "MISS    OUTPUT = 'no match'\n"
                       "DONE")))
         ;; T2 — replace
         (fn [] (let [v (rand-nth str-vars) p (rand-nth pat-lits)
                      r (rand-nth str-lits)]
                  (str (indent (str v " " p " = " r)) "\n"
                       (indent (str "OUTPUT = " v)))))
         ;; T2 — TRIM
         (fn [] (indent (str "OUTPUT = TRIM(" (rand-nth str-vars) ")")))
         ;; T3 — bounded loop
         (fn [] (let [n (rand-nth [3 4 5])]
                  (str "        I = 1\n"
                       "LOOP    OUTPUT = I\n"
                       "        I = I + 1\n"
                       (indent (str "LE(I," n ") :S(LOOP)")))))]
        body ((rand-nth forms))]
    (str canonical-fixtures "\n" body "\n" "END")))

;; ── Error-class programs ──────────────────────────────────────────────────────
;;
;; Generates programs that exercise each of the four error classes at each
;; length band.  The oracle determines whether these are :pass, :skip, etc.

(defn gen-error-class-programs
  "Return a seq of programs designed to exercise the four error classes:
     :normal   — runs fine
     :div-zero — integer division by zero
     :bad-goto — goto undefined label
     :syntax   — malformed statement (grammar error)
   
   Oracle determines expected behaviour — these are passed through diff-run."
  []
  (concat
   ;; :normal — clean programs
   (take 20 (gen-by-length))

   ;; :div-zero — division by zero
   [(str canonical-fixtures "\n"
         "        I = 5 / 0\n"
         "        OUTPUT = I\n"
         "END")
    (str canonical-fixtures "\n"
         "        I = 10\n"
         "        J = 0\n"
         "        K = I / J\n"
         "        OUTPUT = K\n"
         "END")]

   ;; :bad-goto — goto undefined label (runtime error)
   [(str canonical-fixtures "\n"
         "        I = 1\n"
         "        I EQ(I,1) :S(NOWHERE)\n"
         "END")
    (str canonical-fixtures "\n"
         "        :(UNDEFINED_LABEL)\n"
         "END")]

   ;; :syntax errors — malformed programs
   ["        = bad syntax\nEND"
    "NOLABEL\nEND"]))

;; ── Automated batch runner ────────────────────────────────────────────────────
;;
;; Run N programs through the three-oracle harness; save corpus records;
;; return summary statistics.  This is the main entry point for Sprint 18.4.

(defn run-worm-batch
  "Run `n` programs through the three-oracle diff harness.
   source-fn is a 0-arg function that returns a program source string.
   Returns a map:
     :records  — vector of corpus records
     :summary  — {:pass N :fail N :skip N :timeout N :pass-class N}
     :failures — vector of corpus records with :status :fail

   Example:
     (run-worm-batch 100 rand-program)
     (run-worm-batch 500 #(first (drop (rand-int 200) (gen-by-length))))

   Internally calls harness/diff-run — requires SPITBOL + CSNOBOL4 installed.
   Saves all records to resources/golden-corpus.edn."
  [n source-fn]
  (require '[SNOBOL4clojure.harness :as harness])
  (let [programs  (repeatedly n source-fn)
        records   (mapv (fn [src]
                          (try
                            ((resolve 'SNOBOL4clojure.harness/diff-run) src)
                            (catch Exception e
                              {:src src :status :error
                               :thrown (.getMessage e)})))
                        programs)
        summary   (reduce (fn [acc r]
                            (update acc (get r :status :error) (fnil inc 0)))
                          {} records)
        failures  (filterv #(= :fail (:status %)) records)]
    ((resolve 'SNOBOL4clojure.harness/save-corpus!) records)
    {:records  records
     :summary  summary
     :failures failures}))

(defn run-systematic-batch
  "Run the full systematic (gen-by-length) corpus through the harness.
   Returns the same map as run-worm-batch.
   This exhaustively covers every construct in the grammar."
  []
  (require '[SNOBOL4clojure.harness :as harness])
  (let [programs  (vec (gen-by-length))
        _         (println (str "Running " (count programs) " systematic programs..."))
        records   (vec
                    (map-indexed
                      (fn [i src]
                        (when (zero? (mod i 50))
                          (println (str "  " i "/" (count programs) "...")))
                        (try
                          ((resolve 'SNOBOL4clojure.harness/diff-run) src)
                          (catch Exception e
                            {:src src :status :error :thrown (.getMessage e)})))
                      programs))
        summary   (reduce (fn [acc r]
                            (update acc (get r :status :error) (fnil inc 0)))
                          {} records)
        failures  (filterv #(= :fail (:status %)) records)]
    ((resolve 'SNOBOL4clojure.harness/save-corpus!) records)
    {:records  records
     :summary  summary
     :failures failures
     :total    (count programs)}))

;; ── Corpus record → deftest emitter ──────────────────────────────────────────
;;
;; Convert a :fail corpus record into a pinned regression deftest.

(defn- safe-name
  "Make a string into a valid Clojure identifier."
  [s]
  (-> s
      (str/replace #"[^a-zA-Z0-9_]" "_")
      (str/replace #"^[0-9]" "x")))

(defn- quote-lines
  "Turn a multi-line SNOBOL4 source string into a seq of quoted line strings."
  [src]
  (map (fn [line] (str "    " (pr-str line)))
       (str/split-lines src)))

(defn corpus-record->deftest
  "Convert a corpus record to a Clojure deftest string.
   For :pass records: pins the expected output.
   For :fail records: pins the oracle output as expected (regression guard).
   For :skip/:timeout/:pass-class: emits a commented-out skeleton.

   oracle-stdout is extracted from :spitbol or :csnobol4 based on :oracle tag."
  [record index]
  (let [{:keys [src status oracle spitbol csnobol4]} record
        oracle-out (case oracle
                     :both     (:stdout spitbol)
                     :spitbol  (:stdout spitbol)
                     :csnobol4 (:stdout csnobol4)
                     :disagree (:stdout spitbol)
                     :both-error nil
                     nil)
        test-name  (symbol (str "worm_auto_" (format "%04d" index)))
        lines      (quote-lines src)]
    (case status
      (:pass :fail)
      (str "(deftest " test-name "\n"
           "  (let [r (run-with-timeout\n"
           "           (str/join \"\\n\"\n"
           "             [" (str/join "\n              " lines) "]))\n"
           "              2000)]\n"
           "    (is (= " (pr-str (or oracle-out "")) "\n"
           "           (str/trim-newline (or (:stdout r) \"\"))))))\n")

      ;; skip/timeout/pass-class — emit as comment
      (str ";; " (name status) " — skipped (oracle: " oracle ")\n"
           ";; (deftest " test-name " ...)\n"))))

(defn emit-regression-tests
  "Given a collection of corpus records, emit a complete Clojure test namespace
   string suitable for writing to a .clj test file.
   Only :pass and :fail records are emitted as live deftests; others commented."
  [records ns-name]
  (str "(ns " ns-name "\n"
       "  \"Auto-generated regression tests from worm corpus.\n"
       "   DO NOT EDIT — regenerate with (emit-regression-tests ...)\"\n"
       "  (:require [clojure.test :refer :all]\n"
       "            [clojure.string :as str]\n"
       "            [SNOBOL4clojure.core :refer :all]\n"
       "            [SNOBOL4clojure.test-helpers :refer [run-with-timeout]]))\n\n"
       "(GLOBALS *ns*)\n"
       "(use-fixtures :each (fn [f] (GLOBALS (find-ns (quote " ns-name "))) (f)))\n\n"
       (str/join "\n" (map-indexed corpus-record->deftest records))))
