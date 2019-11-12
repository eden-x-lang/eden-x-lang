(ns user
  (:refer-clojure :exclude [load-file])
  (:require [buddy.core.codecs :refer [bytes->hex]]
            [buddy.core.hash :as h]
            [lambdaisland.uri :as uri]
            [clj-yaml.core :as y]
            [clojure.java.io :as io]
            [jsonista.core :as j]
            [sci.core :as sci]
            [sci.impl.interpreter :as i]))

(def ^:dynamic *base-path* "")

(defn local? [f]
  (try
    (io/as-url f)
    false
    (catch Throwable _
      (try (-> f io/file .exists)
           (catch Throwable _
             false)))))

(defn env [x]
  (when (local? *base-path*)
    (System/getenv x)))


(defn extract-base-path [f]
  (if (local? f)
    (-> f io/file .getParentFile .getAbsolutePath (str "/"))
    (-> f io/as-url (uri/join ".") str)))

(defn extract-file-content [f]
  (if (local? f)
    (-> f io/file slurp)
    (slurp f)))

(defn merge-path [base f]
  (if (local? f)
    (str base f)
    (str (uri/join base f))))

(declare load-file)

(defn default-opts []
  {:deny ['require]
   ;;:realize-max 1e7
   :bindings {'load-file load-file
              'env env}})

(defn run-string
  ([s]
   (run-string s ""))
  ([s base-path]
   (binding [*base-path* base-path]
     (sci/eval-string s (default-opts)))))

(defn run-file-path [f]
  (run-string (extract-file-content f)
              (extract-base-path f)))

(defn load-file
  ([file]
   (load-file file nil nil))
  ([file hash]
   (load-file file hash nil))
  ([file hash opts]
   (let [absolute-path (merge-path *base-path* file)]
     (when (and (nil? hash)
                (not (local? absolute-path)))
       (do (println "\nWarning!! Loading and evaluating without freeze")
           (println " - File:" absolute-path)
           (println " - This is a potentially risky operation.")
           (println " - Consider freezing this `load-file`")))
     (let [out (run-file-path absolute-path)
           sha (->> out str h/sha256 bytes->hex (str "sha256:"))]
       (if hash
         (if (= hash sha)
           out
           (throw (ex-info "Semantic mismatch of frozen hash." {:path absolute-path})))
         out)))))



(run-file-path "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/load-file.edn")

(run-file-path "test/edns/load-file.edn")

#_(run-file-path "test/edns/load-blocked.edn")

#_(run-file-path "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/env.edn")

#_(i/init-env! (atom {})  nil nil nil)

;; json

#_(j/write-value-as-string (run-file-path "test/edns/load-file.edn"))

;; yaml

#_(y/generate-string r)
