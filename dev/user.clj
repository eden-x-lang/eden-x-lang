(ns user
  (:refer-clojure :exclude [load-file])
  (:require [buddy.core.codecs :refer [bytes->hex]]
            [buddy.core.hash :as h]
            [clj-yaml.core :as y]
            [clojure.java.io :as io]
            [jsonista.core :as j]
            [sci.core :as sci]
            [sci.impl.interpreter :as i]))

(def ^:dynamic *base-path* "")

(defn env [x]
  (System/getenv x))

(defn extract-path [f]
  (-> f io/file .getParentFile .getAbsolutePath (str "/")))

(declare load-file)

(defn default-opts []
  {:deny ['require]
   :bindings {'load-file load-file
              'env env}})

(defn run-file-path [f]
  (let [c (-> f io/file slurp)
        r (binding [*base-path* (extract-path f)]
            (sci/eval-string c (default-opts)))]
    r))

(defn load-file [file]
  (run-file-path (str *base-path* file)))

(run-file-path "test/edns/load-file.edn")

(-> "foo bar" h/sha256 bytes->hex)

#_(i/init-env! (atom {})  nil nil nil)

;; json

#_(j/write-value-as-string (run-file-path "test/edns/load-file.edn"))

;; yaml

#_(y/generate-string r)
