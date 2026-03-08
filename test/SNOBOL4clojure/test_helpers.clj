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
  "
  (:require [clojure.test :refer [is]]
            [SNOBOL4clojure.core :refer [RUN CODE]]))

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
                            :stdout (with-out-str (RUN (CODE src)))}
                           (catch clojure.lang.ExceptionInfo e
                             (case (get (ex-data e) :snobol/signal)
                               :end       {:exit :ok :stdout ""}
                               :step-limit {:exit :step-limit
                                            :steps (get (ex-data e) :steps)
                                            :thrown (.getMessage e)}
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
          val  (get (:vars r) var-sym)]
      (if (expected-fn val)
        ;; Still correct at mid — divergence must be later
        (bisect-divergence src (inc mid) hi var-sym expected-fn)
        ;; Wrong at mid — divergence is at mid or earlier
        (if (= lo mid)
          {:step mid :value val :vars (:vars r)}
          (bisect-divergence src lo (dec mid) var-sym expected-fn))))))

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
