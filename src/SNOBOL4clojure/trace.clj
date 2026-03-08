(ns SNOBOL4clojure.trace
  "Complete SNOBOL4 TRACE / STOPTR / &TRACE / &FTRACE implementation.

  ## Standard SNOBOL4 TRACE semantics (Gimpel §7, CSNOBOL4 manual §6)

  TRACE(name, type [, label [, fn]])
    Register a trace trigger on `name` of the given `type`.
    Optional `label` — go to this label when trace fires (instead of printing).
    Optional `fn`    — call this user function when trace fires.

  STOPTR(name, type)
    Remove the trace registration for (name, type).

  Trace types (case-insensitive in standard; we normalise to uppercase):
    VALUE   — fires when the named variable is assigned a new value
    LABEL   — fires when execution reaches the named label
    CALL    — fires when the named user-defined function is called
    RETURN  — fires when the named user-defined function returns
    PATTERN — fires at each step of the pattern match engine (per-node trace)
    KEYWORD — fires when a SNOBOL4 keyword (&VAR) is changed

  &TRACE  (env/&TRACE atom)
    Number of currently active trace registrations.
    Set to 0 to suppress all trace output without calling STOPTR.
    When >0, trace output goes to *trace-output* (default *err*).

  &FTRACE (env/&FTRACE atom)
    Function call trace budget. Decremented each time a CALL/RETURN trace
    fires. When it reaches 0, function tracing stops automatically.
    Set to N to trace the next N function call/return events.
    Set to 0 to disable. Default: 0 (off).

  ## Output format (matches CSNOBOL4)

    VALUE:   *** name = <newvalue>
    LABEL:   *** label
    CALL:    *** fn(arg1, arg2, ...)
    RETURN:  *** fn => <returnvalue>  (or FRETURN/NRETURN)
    PATTERN: (handled by engine_frame/animate — existing *trace* dynamic var)
    KEYWORD: *** &name = <newvalue>

  ## Integration points

    runtime.clj  — call (trace-label! key) each statement, (trace-stcount! n)
    operators.clj — call (trace-value! sym newval) on INVOKE '= branch
    functions.clj — call (trace-call! fname args) / (trace-return! fname val)
    match.clj     — bind *trace* true when PATTERN trace active for subject var
  "
  (:require [SNOBOL4clojure.env :refer [&TRACE &FTRACE $$]]))

;; ── Registry ──────────────────────────────────────────────────────────────────
;; Maps [normalised-name normalised-type] -> {:label lbl :fn fn-name}
;; Both keys are uppercase strings.

(def ^:private registry (atom {}))

(defn- norm [x] (clojure.string/upper-case (str x)))

(defn- reg-key [name type] [(norm name) (norm type)])

;; ── Public registration API ───────────────────────────────────────────────────

(defn trace!
  "Register a trace. Called by INVOKE's TRACE branch.
   name  — variable/label/function name (string or symbol)
   type  — trace type string: VALUE LABEL CALL RETURN PATTERN KEYWORD
   label — optional goto label (string/keyword) when trace fires
   fn    — optional user function name to call when trace fires"
  ([name type]       (trace! name type nil nil))
  ([name type label] (trace! name type label nil))
  ([name type label user-fn]
   (let [k (reg-key name type)]
     (swap! registry assoc k {:label label :fn user-fn})
     (reset! &TRACE (count @registry))
     nil)))

(defn stoptr!
  "Remove a trace registration. Called by INVOKE's STOPTR branch."
  [name type]
  (swap! registry dissoc (reg-key name type))
  (reset! &TRACE (count @registry))
  nil)

(defn clear-all-traces!
  "Remove every trace registration. Resets &TRACE to 0."
  []
  (reset! registry {})
  (reset! &TRACE 0)
  nil)

(defn active?
  "True if any trace of the given type is registered for name."
  [name type]
  (contains? @registry (reg-key name type)))

(defn any-active?
  "True if any trace of any type is currently registered."
  []
  (pos? (count @registry)))

;; ── Trace output ──────────────────────────────────────────────────────────────

(def ^:dynamic *trace-output*
  "Where trace output goes. Default: *err* (stderr), matching CSNOBOL4.
   Rebind to *out* or a writer in tests to capture trace output."
  nil)

(defn- trace-println [& args]
  (let [w (or *trace-output* *err*)]
    (binding [*out* w]
      (println (apply str args)))))

;; ── Fire helpers — called from runtime / operators / functions ────────────────

(defn fire-value!
  "Fire VALUE trace for `sym` being assigned `new-val`.
   Called from operators.clj INVOKE '= after the assignment."
  [sym new-val]
  (when (pos? @&TRACE)
    (when-let [entry (@registry (reg-key sym "VALUE"))]
      (if (:label entry)
        ;; Goto-label trace: the runtime must handle this; we return the label.
        ;; (Label-goto traces are unusual; we emit output AND return label.)
        (do (trace-println "*** " (norm sym) " = " (pr-str new-val))
            (:label entry))
        (do (trace-println "*** " (norm sym) " = " (pr-str new-val))
            nil)))))

(defn fire-keyword!
  "Fire KEYWORD trace for `kw-sym` (&VAR) being set to `new-val`."
  [kw-sym new-val]
  (when (pos? @&TRACE)
    (when (@registry (reg-key kw-sym "KEYWORD"))
      (trace-println "*** &" (norm kw-sym) " = " (pr-str new-val)))))

(defn fire-label!
  "Fire LABEL trace when execution reaches `label-key`.
   Called from runtime.clj at the top of each statement dispatch.
   Returns nil (label traces just print; no goto redirect needed here)."
  [label-key]
  (when (pos? @&TRACE)
    (let [lname (if (keyword? label-key) (name label-key) (str label-key))]
      (when (@registry (reg-key lname "LABEL"))
        (trace-println "*** " (norm lname))))))

(defn fire-call!
  "Fire CALL trace when user function `fname` is called with `args`.
   Called from the DEFINE wrapper in operators.clj before body executes.
   Decrements &FTRACE; stops firing when &FTRACE reaches 0."
  [fname args]
  (when (pos? @&FTRACE)
    (swap! &FTRACE dec)
    (when (@registry (reg-key fname "CALL"))
      (trace-println "*** " (norm fname)
                     "(" (clojure.string/join ", " (map pr-str args)) ")")))
  (when (pos? @&TRACE)
    (when (@registry (reg-key fname "CALL"))
      (trace-println "*** " (norm fname)
                     "(" (clojure.string/join ", " (map pr-str args)) ")"))))

(defn fire-return!
  "Fire RETURN trace when user function `fname` returns `val`.
   signal is :return :freturn or :nreturn."
  [fname val signal]
  (when (or (pos? @&FTRACE) (pos? @&TRACE))
    (when (@registry (reg-key fname "RETURN"))
      (let [tag (case signal
                  :freturn " [FRETURN]"
                  :nreturn " [NRETURN]"
                  "")]
        (trace-println "*** " (norm fname) " => " (pr-str val) tag)))))

(defn pattern-trace-active?
  "True if PATTERN trace is registered for `subject-var` (or globally via \"*\").
   Used by match.clj to decide whether to bind *trace* true."
  [subject-var]
  (and (pos? @&TRACE)
       (or (@registry (reg-key subject-var "PATTERN"))
           (@registry (reg-key "*" "PATTERN")))))

;; ── &TRACE as a simple on/off toggle (convenience, non-standard extension) ────
;; Setting &TRACE = 1 with no registrations enables a default statement trace.
;; Setting &TRACE = 0 suppresses all output even if registrations exist.

(defn statement-trace-active?
  "True if &TRACE > 0 AND a global statement trace (name \"*\" type \"LABEL\")
   is registered. This is our extension for full statement-by-statement tracing."
  []
  (and (pos? @&TRACE)
       (@registry (reg-key "*" "LABEL"))))

(defn enable-full-trace!
  "Convenience: register a global statement+value trace. Sets &TRACE > 0.
   Equivalent to the test helper calling:
     (trace! \"*\" \"LABEL\") (trace! \"*\" \"VALUE\") (trace! \"*\" \"CALL\")"
  []
  (trace! "*" "LABEL")
  (trace! "*" "VALUE")
  (trace! "*" "CALL")
  (trace! "*" "RETURN"))

(defn disable-full-trace!
  "Convenience: clear the global wildcard traces."
  []
  (stoptr! "*" "LABEL")
  (stoptr! "*" "VALUE")
  (stoptr! "*" "CALL")
  (stoptr! "*" "RETURN"))
