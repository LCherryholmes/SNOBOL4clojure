(ns SNOBOL4clojure.test-helpers
  "Shared test utilities for SNOBOL4clojure test suites.

  ## The Halting Problem & Per-Test Timeouts

  SNOBOL4 programs can loop forever (e.g. :S(SELF) with no exit).
  It is mathematically impossible to statically determine whether any
  given SNOBOL4 program will terminate (Rice's theorem / Halting Problem).
  Therefore every test that executes a SNOBOL4 program MUST run under a
  wall-clock timeout to prevent `lein test` from hanging indefinitely.

  ## How to use

  Replace the bare `prog` macro with `prog-timeout`:

      (deftest my-test
        \"Expected run: <1ms\"
        (prog-timeout 500                        ; budget in ms
          \"        S = 'hello'\"
          \"END\")
        (is (= \"hello\" ($$ 'S))))

  For tests known/expected to be infinite (used to verify loop detection):

      (deftest my-infinite-test
        \"Expected run: INFINITE — must timeout\"
        (let [result (prog-budget 200
                       \"L       :(L)\"
                       \"END\")]
          (is (= :timeout (:exit result)))))

  ## Timeout budget conventions

  | Budget (ms) | Use case |
  |-------------|----------|
  | 100         | Trivial: assignment, single match, no loops |
  | 500         | Normal: small programs, bounded loops (< 100 iters) |
  | 2000        | Complex: ARB, ARBNO, backtracking-heavy patterns |
  | :infinite   | Document-only marker; always wrap with prog-budget |

  ## Retry policy

  On first timeout the test retries ONCE (JVM warmup / GC jitter can cause
  spurious timeouts). If it times out twice it is reported as a test failure
  via `clojure.test/is`, not a hang.

  ## Sprint 18C — Step-probe bisection debugger

  ### `run-to-step` / `probe-at`
  Run a program for exactly N statements then inspect variable state:
      (run-to-step src 50)    ; => {:exit :step-limit :steps 50 :vars {I 5} ...}
      (probe-at src 50 'I)    ; => 5

  ### `bisect-divergence`
  Binary-search for the first step at which a variable deviates from a
  predicate.  Useful for isolating bugs in long-running programs:
      (bisect-divergence src 1 1000 'I #(<= % 100))
  Returns {:step N :value <wrong-value> :vars <snapshot>} or nil.

  ### `probe-test` macro
  Assert variable values at specific step counts:
      (probe-test {10 {'I 10} 50 {'I 50}} \"        I=0\" \"LOOP...\" \"END\")

  ### `run-with-restart`
  Stop at N steps, inspect/patch state, then resume:
      (def h (run-with-restart src 50))
      (get (:vars h) 'I)         ; inspect
      (snobol-set! 'I 99)        ; patch
      ((:resume h) 20)           ; run 20 more steps

  ### CRITICAL: `=` shadowing hazard
  Test namespaces using `[SNOBOL4clojure.core :refer :all]` have `=` replaced
  by SNOBOL4's IR-building `=` operator.  In predicate lambdas defined in such
  a namespace, `(= v 0)` builds the IR list `(= v 0)` — always truthy!

  Always use `clojure.core/=` in predicates passed to bisect-divergence when
  the predicate is defined in a test namespace:
      ;; WRONG (= is SNOBOL4's operator, returns a truthy list):
      (bisect-divergence src 1 100 'I #(= % 5))
      ;; CORRECT:
      (bisect-divergence src 1 100 'I #(clojure.core/= % 5))
  "
  (:require [clojure.test :refer [is]]
            [SNOBOL4clojure.core :refer [RUN CODE CODE-memo preprocess-includes]]))

;; ── Low-level timed executor ───────────────────────────────────────────────────

(defn run-with-timeout
  "Run (RUN (CODE src)) under a wall-clock budget.
   Returns {:exit :ok :stdout <str>} or {:exit :timeout :stdout nil}.
   Retries once on timeout (to absorb JVM warmup / GC jitter).
   Budget-ms default is 2000."
  ([src] (run-with-timeout src 2000))
  ([src budget-ms]
   (letfn [(attempt []
             (let [result-p (promise)
                   f (future
                       (deliver result-p
                         (try
                           {:exit :ok
                            :stdout (with-out-str (RUN (CODE-memo src)))}
                           (catch clojure.lang.ExceptionInfo e
                             (case (get (ex-data e) :snobol/signal)
                               :end       {:exit :ok :stdout ""}
                               :step-limit {:exit   :step-limit
                                            :steps  (get (ex-data e) :steps)
                                            :thrown (.getMessage e)
                                            ;; 18C.9: auto-snapshot at step-limit stop
                                            :vars   (SNOBOL4clojure.env/snapshot!)}
                               {:exit :error :thrown (.getMessage e)}))
                           (catch Exception e
                             {:exit :error :thrown (.getMessage e)}))))]
               (or (deref result-p budget-ms nil)
                   (do (future-cancel f) {:exit :timeout :stdout nil}))))]
     ;; Retry once on timeout
     (let [r (attempt)]
       (if (= (:exit r) :timeout)
         (attempt)
         r)))))

;; ── Test-level macros ─────────────────────────────────────────────────────────

(defmacro prog-timeout
  "Run SNOBOL4 program lines under a wall-clock budget (ms).
   Fails the test (via is) if it times out after 1 retry.
   Normal usage: side-effects land in global env; check with ($$ 'VAR).

   Example:
     (prog-timeout 500
       \"        S = 'hello'\"
       \"END\")"
  [budget-ms & lines]
  `(let [src# ~(clojure.string/join "\n" (map str lines))
         r#   (run-with-timeout src# ~budget-ms)]
     (is (not (#{:timeout :step-limit} (:exit r#)))
         (str (if (= :step-limit (:exit r#))
                (str "STEP-LIMIT after " (:steps r#) " steps: ")
                (str "TIMEOUT after " ~budget-ms "ms (x2 retries): "))
              (pr-str (first (clojure.string/split-lines src#)))))
     r#))

(defmacro prog
  "Convenience alias: run program with the default 2000ms budget.
   Drop-in replacement for the bare (prog ...) macro in test files."
  [& lines]
  `(prog-timeout 2000 ~@lines))

(defmacro prog-include
  "Like prog but resolves -INCLUDE directives relative to include-dirs.
   include-dirs is a seq of directory path strings (evaluated at runtime).
   src is a single source string expression (evaluated at runtime).

   Example:
     (prog-include [\"corpus/gimpel/SNOBOL4\"]
       \"-INCLUDE 'push.inc'\\n        OUTPUT = 'hello'\\nEND\")"
  [include-dirs src]
  `(let [raw#  ~src
         src#  (preprocess-includes raw# ~include-dirs)
         r#    (run-with-timeout src# 5000)]
     (is (not (#{:timeout :step-limit} (:exit r#)))
         (str "prog-include timed out or hit step-limit: "
              (pr-str (first (clojure.string/split-lines raw#)))))
     r#))

(defmacro prog-infinite
  "Document that a program is EXPECTED to run forever.
   Asserts that it DOES time out within budget-ms.
   Use this for tests that verify loop-detection / :S(SELF) behaviour."
  [budget-ms & lines]
  `(let [src# ~(clojure.string/join "\n" (map str lines))
         r#   (run-with-timeout src# ~budget-ms)]
     (is (= :timeout (:exit r#))
         (str "Expected TIMEOUT but program returned: " (:exit r#)))
     r#))

;; ── Step-limit helpers ────────────────────────────────────────────────────────

(defn set-steplimit!
  "Set &STLIMIT to n. Use before prog-timeout to bound a potentially looping
   program in the engine itself (not just wall clock).
   Smaller n = faster failure detection for genuinely infinite programs.
   Reset to 2147483647 to restore default (unlimited)."
  [n]
  (reset! SNOBOL4clojure.env/&STLIMIT n))

(defmacro prog-steplimit
  "Run program with both a wall-clock budget AND an in-engine step limit.
   Fails the test if either the step limit or the timeout is exceeded.
   Use for loop tests where you know an upper bound on iterations."
  [budget-ms max-steps & lines]
  `(do
     (reset! SNOBOL4clojure.env/&STLIMIT ~max-steps)
     (let [r# (prog-timeout ~budget-ms ~@lines)]
       (reset! SNOBOL4clojure.env/&STLIMIT 2147483647)
       r#)))

;; ── Step-probe / bisection debugger (Sprint 18C) ─────────────────────────────

(defn run-to-step
  "Run SNOBOL4 program `src` for exactly `n` statements then stop.
   Returns a map:
     :exit    — :ok (finished naturally) | :step-limit | :error | :timeout
     :steps   — &STCOUNT at termination
     :vars    — snapshot of all user variable bindings at termination
     :stdout  — any OUTPUT produced up to step n
     :thrown  — exception message if :error

   This is the core probe primitive. Use it to inspect program state
   at any point during execution — the foundation of bisection debugging.

   Example:
     (run-to-step \"        I = 0\\nLOOP    I = I + 1\\n        LT(I,10) :S(LOOP)\\nEND\" 5)
     ;=> {:exit :step-limit :steps 5 :vars {I 5} :stdout \"\"}"
  [src n]
  ;; Reset compiler state so each call starts from a clean slate.
  ;; Without this, accumulated CODE entries from prior calls shift statement
  ;; numbers and leave stale variable state that corrupts bisect-divergence.
  (reset! SNOBOL4clojure.env/STNO  0)
  (reset! SNOBOL4clojure.env/<STNO> {})
  (reset! SNOBOL4clojure.env/<LABL> {})
  (reset! SNOBOL4clojure.env/<CODE> {})
  (reset! SNOBOL4clojure.env/&STLIMIT n)
  (let [r (run-with-timeout src 5000)]
    (reset! SNOBOL4clojure.env/&STLIMIT 2147483647)
    (assoc r
           :steps @SNOBOL4clojure.env/&STCOUNT
           :vars  (SNOBOL4clojure.env/snapshot!))))

(defn probe-at
  "Run src to step n and return the value of variable `var-sym` at that point.
   Convenience wrapper around run-to-step.

   Example:
     (probe-at loop-src 50 'I)  ;=> 5"
  [src n var-sym]
  (let [r (run-to-step src n)]
    (get (:vars r) var-sym)))

(defn bisect-divergence
  "Binary-search for the lowest step N (in [lo..hi]) at which the value of
   `var-sym` in our engine diverges from `expected-fn` — a 1-arg predicate
   that returns true when the value is correct.

   Returns {:step N :value <wrong-value> :vars <full-snapshot>}
   or nil if no divergence found in [lo..hi].

   Example: find when I stops being (= step-count / 2):
     (bisect-divergence src 1 1000 'I #(= % (/ (run-to-step src N) 2)))

   Practical use:
     Find the exact step where our engine diverges from oracle output.
     Run CSNOBOL4/SPITBOL with &STLIMIT=N prepended to get oracle state at N,
     then compare with (run-to-step src N)."
  [src lo hi var-sym expected-fn]
  (if (> lo hi)
    nil
    (let [mid  (quot (+ lo hi) 2)
          r    (run-to-step src mid)
          val  (get (:vars r) var-sym)
          ok?  (expected-fn val)]
      (if (= lo hi)
        ;; Base case: single step — return if pred fails, nil if holds
        (when-not ok? {:step mid :value val :vars (:vars r)})
        (if ok?
          ;; Correct at mid — first failure must be later
          (bisect-divergence src (inc mid) hi var-sym expected-fn)
          ;; Wrong at mid — first failure is at mid or earlier
          (or (bisect-divergence src lo (dec mid) var-sym expected-fn)
              {:step mid :value val :vars (:vars r)}))))))

(defmacro probe-test
  "Assert variable values at specific step counts during program execution.
   probe-points is a map of {step-count {var-sym expected-value}}.
   Fails at the first step/variable pair that doesn't match.

   Use this for loop correctness tests: verify invariants at N, 2N, 3N steps.

   Example:
     (probe-test
       {1  {'I 1}
        5  {'I 5}
        10 {'I 10}}
       \"        I = 0\"
       \"LOOP    I = I + 1\"
       \"        LT(I,10)  :S(LOOP)\"
       \"END\")"
  [probe-points & lines]
  (let [src (clojure.string/join "\n" (map str lines))]
    `(doseq [[step# expectations#] (sort-by key ~probe-points)]
       (let [r#    (run-to-step ~src step#)
             vars# (:vars r#)]
         (doseq [[var# expected#] expectations#]
           (clojure.test/is (= expected# (get vars# var#))
                            (str "At step " step# ", expected " var#
                                 " = " (pr-str expected#)
                                 " but got " (pr-str (get vars# var#)))))))))

;; ── Restart mode (Sprint 18C.6) ───────────────────────────────────────────────

(defn run-with-restart
  "Run src for exactly n statements, then return a restart handle.

   The restart handle is a map:
     :resume  — (fn [n2]) — run n2 MORE statements from current position
     :vars    — full variable snapshot at the stop point
     :steps   — &STCOUNT at stop
     :exit    — :step-limit | :ok | :error
     :stdout  — output produced so far

   Workflow:
     1. (def h (run-with-restart src 50))   ; stop after 50 stmts
     2. (get (:vars h) 'I)                  ; inspect variable I
     3. (snobol-set! 'I 99)                 ; patch a variable
     4. ((:resume h) 20)                    ; run 20 more statements
     5. (snapshot!)                         ; inspect state again

   This gives you an interactive REPL debugger for SNOBOL4 programs.
   Variable state is intact in the Clojure namespace atoms between calls.

   WARNING: run-with-restart does NOT call GLOBALS — it uses the current
   namespace.  Always call GLOBALS before the first run-with-restart in
   a test or REPL session."
  [src n]
  (let [r (run-to-step src n)]
    (assoc r
           :resume (fn [n2]
                     ;; Don't re-CODE — use current statement pointer
                     (reset! SNOBOL4clojure.env/&STLIMIT n2)
                     (reset! SNOBOL4clojure.env/&STCOUNT 0)
                     (let [next-stmt @SNOBOL4clojure.env/<STNO>
                           resume-r  (try
                                       {:exit   :ok
                                        :stdout (with-out-str
                                                  (SNOBOL4clojure.runtime/RUN
                                                    ;; Resume from statement after stop
                                                    (inc @SNOBOL4clojure.env/STNO)))}
                                       (catch clojure.lang.ExceptionInfo e
                                         (case (get (ex-data e) :snobol/signal)
                                           :end        {:exit :ok  :stdout ""}
                                           :step-limit {:exit :step-limit
                                                        :steps (get (ex-data e) :steps)}
                                           {:exit :error :thrown (.getMessage e)}))
                                       (catch Exception e
                                         {:exit :error :thrown (.getMessage e)})
                                       (finally
                                         (reset! SNOBOL4clojure.env/&STLIMIT 2147483647)))]
                       (assoc resume-r :vars (SNOBOL4clojure.env/snapshot!)))))))
