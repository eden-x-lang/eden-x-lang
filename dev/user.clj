(ns user
  (:require [buddy.core.codecs :refer [bytes->hex]]
            [buddy.core.hash :as h]
            [clj-yaml.core :as y]
            [clojure.java.io :as io]
            [jsonista.core :as j]
            [sci.core :as sci]
            [sci.impl.interpreter :as i]))

(defn env [x]
  (System/getenv x))

(defn extract-path [f]
  (-> f io/file .getParentFile .getAbsolutePath (str "/")))

(declare eval-load-file)

(defn default-opts [f]
  {:running-ctx {:base-path (extract-path f)}
   :deny ['require]
   :special-forms {'load-file eval-load-file}
   :bindings {'env env}})

(defn run-file-path [f]
  (let [c (-> f io/file slurp)
        r (sci/eval-string c (default-opts f))]
    r))

(defn eval-load-file [{:keys [running-ctx] :as ctx} expr]
  (let [file (second expr)]
    (run-file-path (str (:base-path running-ctx) file))))

(run-file-path "test/edns/load-file.edn")

(-> "foo bar" h/sha256 bytes->hex)

#_(i/init-env! (atom {})  nil nil nil)

;; json

#_(j/write-value-as-string r)

;; yaml

#_(y/generate-string r)
