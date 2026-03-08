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
                             (if (= (get (ex-data e) :snobol/signal) :end)
                               {:exit :ok :stdout ""}
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
     (is (not= :timeout (:exit r#))
         (str "TIMEOUT after " ~budget-ms "ms (x2 retries): "
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
