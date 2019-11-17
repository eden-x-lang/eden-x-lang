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
    :default-fn #(-> % :type s/lower-case keyword)
    :parse-fn #(-> % s/lower-case keyword)
    :validate [#(#{:edn :json :yaml} %) "Must be one of edn, json or yaml"]]

   [nil "--compact" "Render in a compact fashion"]

   [nil "--silent" "Does not show any warnings"]

   [nil "--watch" "Run in watch mode"]

   [nil "--hash" "Return the semantic hash of the provided file"]

   [nil "--file FILE" "Read from a file instead of standard input"]

   [nil "--output FILE" "Write to a file instead of standard output"]

   [nil "--eval EVAL" "Evaluates provided snippet instead of input file"]

   [nil "--version" "Display version"]])

(defn ^:private help-message [summary]
  (->> ["\nCompiles eden-x code"
        "\nUsage: eden-x [[--type TYPE] [--hash | --watch]"
        "               [--file FILE | --eval EVAL] [--output FILE]"
        "               [--compact] [--silent] [--version]]"
        "\nAvailable options:"
        summary]
       (s/join "\n")))

(defn ^:private version-message []
  (str "eden-x v" (s/trim (slurp (io/resource "EDEN_X_VERSION")))))

(defn ^:private validate-args [args]
  (let [{:keys [summary errors options] :as parsed} (parse-opts args cli-options)
        {:keys [type compact silent watch hash file output eval help version]} options]
    (cond
      errors
      {:errors errors}
      help
      {:show-message (help-message summary)}
      version
      {:show-message (version-message)}
      (and file eval)
      {:errors ["Please provide just one of \"--file FILE\" or \"--eval EVAL\""]}
      (and hash watch)
      {:errors ["Please provide just one of \"--hash\" or \"--watch\""]}
      (and eval watch)
      {:errors ["Can't \"--watch\" with \"--eval EVAL\". Use \"--file FILE\" instead"]}
      (and hash (nil? file))
      {:errors ["Please provide \"--file FILE\" when using \"--watch\""]}
      (and hash file)
      {:hash-file file}
      file
      {:input-file file
       :output-file output
       :watch (boolean watch)
       :compact (boolean compact)
       :silent (boolean silent)
       :type type}
      eval
      {:eval-string eval
       :output-file output
       :compact (boolean compact)
       :silent (boolean silent)
       :type type}
      :else
      {:show-message (help-message summary)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn main [& args]
  (handle-pipe!)
  (let [{:keys [show-message errors
                input-file output-file hash-file eval-string
                type compact silent watch]} (validate-args args)
        opts {:compact compact
              :silent silent
              :type type}]
    (cond
      errors
      (do (utils/error "Argument error." errors))
      show-message
      (do (println show-message)
          0)
      input-file
      ;; TODO wrap in try/catch + the other params
      (do (println (eden/run-file input-file opts))
          0)
      eval-string
      (do (println (eden/run-string eval-string opts))
          0))))

(defn -main
  [& args]
  (let [exit-code (apply main args)]
    (System/exit exit-code)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scratch

#_(main "--type" "yaml" "--file" "test/edns/function.edn"
        "--compact")
