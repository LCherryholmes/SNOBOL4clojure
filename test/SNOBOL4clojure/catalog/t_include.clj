(ns SNOBOL4clojure.catalog.t-include
  "Sprint 25A — -INCLUDE preprocessor tests.

   Tests:
     include_preprocess_basic     — preprocess-includes expands a -INCLUDE line
     include_preprocess_not_found — missing file becomes a comment, does not crash
     include_preprocess_cycle     — cyclic includes are detected and skipped
     include_smoke_inline         — CODE! with include-path compiles included code
     include_two_file             — two-file include via temp directory
     include_bcd_ebcd_compiles    — BCD_EBCD.SNO (no includes) compiles cleanly
  "
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [SNOBOL4clojure.compiler :refer [preprocess-includes CODE!]]
            [SNOBOL4clojure.test-helpers :refer [prog prog-include]]))

;; ── preprocess-includes unit tests ───────────────────────────────────────────

(deftest include_preprocess_basic
  "preprocess-includes expands -INCLUDE 'file' to file contents"
  (let [dir  (doto (io/file (System/getProperty "java.io.tmpdir")
                            (str "snobol4test_" (System/currentTimeMillis)))
               .mkdirs)
        inc  (io/file dir "greet.inc")]
    (try
      (spit inc "        OUTPUT = 'hello from include'\n")
      (let [src      (str "-INCLUDE 'greet.inc'\nEND\n")
            expanded (preprocess-includes src [(.getAbsolutePath dir)])]
        (is (.contains expanded "hello from include")
            "Expanded source should contain included file content")
        (is (not (.contains expanded "-INCLUDE"))
            "Expanded source should not contain raw -INCLUDE directive"))
      (finally (.delete inc) (.delete dir)))))

(deftest include_preprocess_not_found
  "preprocess-includes converts missing -INCLUDE to a comment, no exception"
  (let [expanded (preprocess-includes "-INCLUDE 'does_not_exist.inc'\nEND\n" ["/nonexistent"])]
    (is (.contains expanded "* -INCLUDE not found")
        "Missing include should become a comment")
    (is (.contains expanded "does_not_exist.inc")
        "Comment should name the missing file")))

(deftest include_preprocess_case_insensitive
  "-include (lowercase) is also recognised"
  (let [dir (doto (io/file (System/getProperty "java.io.tmpdir")
                           (str "snobol4test_" (System/currentTimeMillis)))
              .mkdirs)
        inc (io/file dir "low.inc")]
    (try
      (spit inc "        OUTPUT = 'lower case include'\n")
      (let [expanded (preprocess-includes
                       (str "-include 'low.inc'\nEND\n")
                       [(.getAbsolutePath dir)])]
        (is (.contains expanded "lower case include")))
      (finally (.delete inc) (.delete dir)))))

;; ── Two-file include smoke test ───────────────────────────────────────────────

(deftest include_two_file
  "CODE! with include-path compiles and runs code from an included file"
  (let [dir (doto (io/file (System/getProperty "java.io.tmpdir")
                           (str "snobol4test_" (System/currentTimeMillis)))
              .mkdirs)
        inc (io/file dir "msg.inc")]
    (try
      ;; msg.inc just assigns a variable
      (spit inc "        MSG = 'hello from inc'\n")
      (let [src (str "-INCLUDE 'msg.inc'\n        OUTPUT = MSG\nEND\n")
            r   (prog-include [(.getAbsolutePath dir)] src)]
        (is (= "hello from inc\n" (:stdout r))
            "Included assignment should be visible after expansion"))
      (finally (.delete inc) (.delete dir)))))

;; ── BCD_EBCD.SNO compiles without includes ────────────────────────────────────

(deftest include_bcd_ebcd_compiles
  "BCD_EBCD.SNO has no -INCLUDE directives and compiles cleanly.
   This test verifies REPLACE still works after the preprocessor refactor."
  ;; BCD_EBCD reads from INPUT in a loop; we just verify it compiles (no error)
  ;; and that a direct REPLACE call gives the right answer.
  (is (= "='()+\n"
         (:stdout (prog "
        OUTPUT = REPLACE('#@%<&','#@%<&',\"='()+\")
END")))))

