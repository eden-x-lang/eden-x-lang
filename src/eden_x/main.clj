(ns eden-x.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [eden-x.core :as eden]
            [eden-x.impl.pipe-signal-handler :refer [handle-pipe! pipe-signal-received?]]
            [eden-x.utils :as utils]))

(def ^:private cli-options
  [["-h" "--help" "Show this help text"]
   [nil "--type TYPE" "edn, json or yaml"
    :default "edn"
    :default-fn #(-> % s/lower-case keyword)
    :parse-fn #(-> % s/lower-case keyword)
    :validate [#(#{:edn :json :yaml} %) "Must be one of edn, json or yaml"]]
   [nil "--compact" "Render in a compact fashion"]
   [nil "--watch" "Run in watch mode"]
   [nil "--hash" "Return the semantic hash of the provided file"]
   [nil "--file FILE" "Read from a file instead of standard input"]
   [nil "--output FILE" "Write to a file instead of standard output"]
   [nil "--eval EVAL" "Evaluates provided snippet instead of input file"]
   [nil "--version" "Display version"]])

(defn ^:private print-help [summary]
  (println
   (->> ["\nCompiles eden-x code"
         "\nUsage: eden-x [[--type TYPE] [--hash | --watch]"
         "               [--file FILE | --eval EVAL] [--output FILE]"
         "               [--compact] [--version]]"
         "\nAvailable options:"]
        (s/join "\n")))
  (println summary)
  0)

(defn ^:private print-version []
  (println (str "eden-x v" (s/trim (slurp (io/resource "EDEN_X_VERSION")))))
  0)

#_(defn ^:private validate-args [args]
    (let [{:keys [arguments summary errors options] :as parsed} (parse-opts args cli-options)
          {:keys [help version watch hash type]} options
          [file] arguments]
      (cond
        help
        
        (print-help summary)
        version
        (print-version)
        :else
        (print-help summary))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn main [& args]
  (handle-pipe!)
  (let [{:keys [arguments summary errors options] :as parsed} (parse-opts args cli-options)
        {:keys [help version watch hash type]} options
        [file] arguments]
    (cond
      help
      (print-help summary)
      version
      (print-version)
      :else
      (print-help summary))))

(defn -main
  [& args]
  (let [exit-code (apply main args)]
    (System/exit exit-code)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scratch

#_(main "--help"
        )
