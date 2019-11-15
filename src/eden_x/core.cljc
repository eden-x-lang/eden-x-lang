(ns eden-x.core
  (:refer-clojure :exclude [load-file])
  (:require [buddy.core.codecs :refer [bytes->hex]]
            [buddy.core.hash :as h]
            [clojure.pprint :refer [pprint]]
            [clj-yaml.core :as y]
            [clojure.java.io :as io]
            [eden-x.utils :as utils]
            [jsonista.core :as j]
            [lambdaisland.uri :as uri]
            [sci.core :as sci]
            [sci.impl.interpreter :as i]))

(def ^:dynamic *base-path* "")

(def ^:dynamic *running-file* "")

(def ^:dynamic *running-file-absolute* "")

(defn ^:private local? [f]
  (try
    (io/as-url f)
    false
    (catch Throwable _
      (try (-> f io/file .exists)
           (catch Throwable _
             false)))))

(defn ^:private env [x]
  (if (local? *base-path*)
    (System/getenv (name x))
    (binding [*out* *err*]
      (utils/warning "Illegal operation from remote file"
                     ["Path:" *running-file-absolute*
                      (-> ["Using `env` from a remotely hosted file could lead "
                           "to a malicious script stealing secrets from your system. "
                           "Therefore `env` does not work in this context."]
                          (apply str))]))))

(defn ^:private extract-base-path [f]
  (if (local? f)
    (-> f io/file .getParentFile .getAbsolutePath (str "/"))
    (-> f io/as-url (uri/join ".") str)))

(defn ^:private extract-file-content [f]
  (if (local? f)
    (-> f io/file slurp)
    (slurp f)))

(defn ^:private merge-path [base f]
  (if (local? f)
    (str base f)
    (str (uri/join base f))))

(declare ^:private load-file)

(defn ^:private default-opts []
  {:deny ['require]
   ;;:realize-max 1e7 ;; FIXME: it would be great to have it but loop/recur breaks on SCI
   :bindings {'load-file load-file
              'env env}})

(declare ^:private run-file)

(defn ^:private load-file
  ([f]
   (load-file f nil nil))
  ([f hash]
   (load-file f hash nil))
  ([f hash opts]
   (let [new-absolute-path (merge-path *base-path* f)]
     (when (and (nil? hash)
                (not (local? new-absolute-path)))
       (utils/warning "Remote file being loaded transitively without freeze"
                      ["Path:" new-absolute-path
                       "This is potentially a risky operation."
                       "Consider freezing this `load-file`"
                       (str "Run `$ eden-x --hash " new-absolute-path "`")]))
     (let [out (run-file new-absolute-path)
           sha (->> out str h/sha256 bytes->hex (str "sha256:"))]
       (if hash
         (if (= hash sha)
           out
           (throw (ex-info "Semantic mismatch of frozen hash." {:path new-absolute-path})))
         out)))))

(def ^:private pretty-mapper
  (j/object-mapper
   {:pretty true}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn run-string-data [s]
  (sci/eval-string s (default-opts)))

(defn run-file-data [f]
  (binding [*base-path* (extract-base-path f)
            *running-file* f
            *running-file-absolute* (merge-path *base-path* f)]
    (run-string (extract-file-content f))))

(defn run-string
  ([s]
   (run-string s nil))
  ([s {:keys [compact type]}]
   (let [r (run-string-data s)]
     (case type
       :edn
       (if compact (str r) (with-out-str (pprint r)))
       :json
       (if compact
         (j/write-value-as-string r)
         (j/write-value-as-string r pretty-mapper))
       :yaml
       (y/generate-string r)))))

(defn run-file
  ([f]
   (run-file f nil))
  ([f opts]
   (run-string (extract-file-content f) opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scratch

(comment
  (run-file "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/load-file.edn")

  (run-file "test/edns/load-file.edn")

  #_(run-file "test/edns/load-blocked.edn")

  #_(run-file "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/env.edn")

  #_(i/init-env! (atom {})  nil nil nil)

  ;; json

  #_(j/write-value-as-string (run-file "test/edns/load-file.edn"))

  ;; yaml

  #_(y/generate-string r))
