(ns eden-x.core
  (:refer-clojure :exclude [load-file])
  (:require [buddy.core.codecs :refer [bytes->hex]]
            [buddy.core.hash :as h]
            [clj-yaml.core :as y]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [jsonista.core :as j]
            [lambdaisland.uri :as uri]
            [sci.core :as sci]
            [sci.impl.interpreter :as i])
  (:import (clojure.lang ExceptionInfo)))

(def ^:dynamic *base-path* "")

(def ^:dynamic *load-breadcrumb* [])

(def ^:dynamic *running-file* "")

(def ^:dynamic *running-file-absolute* "")

(def ^:dynamic *streaming* false)

(def environ (atom {:warnings []}))

(defn ^:private reset-environ! []
  (swap! environ assoc :warnings []))

(defn ^:private conj-warning! [warning]
  (swap! environ update-in [:warnings] conj warning))

(defn ^:private local? [f]
  (if *streaming*
    true
    (try
      (io/as-url f)
      false
      (catch Throwable _
        true))))

(defn ^:private env [x]
  (if (local? *running-file-absolute*)
    (System/getenv (name x))
    (do
      (conj-warning!
       {::category ::illegal-remote-operation
        ::title "Illegal operation from remote file"
        ::details [(str "Path: " (if (not= "" *running-file-absolute*)
                                   *running-file-absolute* "N/A"))
                   "Using `env` from a remotely hosted file could lead "
                   "to a malicious script stealing secrets from your system. "
                   "Therefore `env` does not work in this context and will return nil."]})
      nil)))

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

(defn ^:private spawn-and-run-file
  ([f]
   (binding [*base-path* (extract-base-path f)
             *load-breadcrumb* (conj *load-breadcrumb* f)
             *running-file* f
             *running-file-absolute* (merge-path *base-path* f)
             *streaming* false]
     (try
       (sci/eval-string (extract-file-content *running-file-absolute*)
                        (default-opts))
       (catch ExceptionInfo ex
         (let [{:keys [row col type ::category]} (ex-data ex)]
           (cond
             category (throw ex)
             (= :sci/error type) (throw (ex-info (.getMessage ex)
                                                 {::category ::code-error
                                                  ::file f
                                                  ::load-breadcrumb *load-breadcrumb*
                                                  ::row row
                                                  ::col col})))))))))

(defn ^:private load-file
  ([f]
   (load-file f nil nil))
  ([f hash]
   (load-file f hash nil))
  ([f hash opts]
   (binding [*streaming* false]
     (let [new-absolute-path (merge-path *base-path* f)]
       (when (and (nil? hash)
                  (not (local? new-absolute-path)))
         (conj-warning! {::category ::transitive-unfrozen-load
                         ::title "Remote file being loaded transitively without freeze"
                         ::details [(str "Path: " new-absolute-path)
                                    "This is potentially a risky operation."
                                    "Consider freezing this `load-file`"
                                    (str "Run `$ eden-x --hash --file " new-absolute-path "`")]}))
       (let [out (spawn-and-run-file f)]
         (if hash
           (let [present-sha (->> out str h/sha256 bytes->hex (str "sha256:"))]
             (if (= hash present-sha)
               out
               (throw (ex-info "Semantic mismatch of frozen hash."
                               {::category ::semantic-mistmatch
                                ::path new-absolute-path})))
             out)
           out))))))

(def ^:private pretty-mapper
  (j/object-mapper
   {:pretty true}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

;; FIXME this function name is lame
(defn run-string-data
  ([s]
   (reset-environ!)
   (binding [*streaming* true]
     (try
       (sci/eval-string s (default-opts))
       (catch Throwable ex
         (let [{:keys [row col]} (ex-data ex)]
           (throw (ex-info (.getMessage ex)
                           {::category ::code-error
                            ::row row
                            ::col col}))))))))

;; FIXME this function name is lame
(defn run-file-data
  ([f]
   (reset-environ!)
   (spawn-and-run-file f)))

;; FIXME consider extracting away
;; FIXME missing reset warnings
(defn run-string
  ([s]
   (run-string s nil))
  ([s {:keys [silent compact type] :or {type :edn}}]
   (binding [*streaming* true]
     (let [r (run-string-data s)]
       (case type
         :edn
         (if compact (str r) (with-out-str (pprint r)))
         :json
         (if compact
           (j/write-value-as-string r)
           (j/write-value-as-string r pretty-mapper))
         :yaml
         (if compact
           (y/generate-string r)
           (y/generate-string r :dumper-options {:flow-style :block})))))))

;; FIXME consider extracting away
;; FIXME missing reset warnings
;; FIXME this is wrongly calling run-string and loosing *streaming* needs fix
(defn run-file
  ([f]
   (run-file f nil))
  ([f opts]
   (binding [*base-path* (extract-base-path f)
             *running-file* f
             *running-file-absolute* (merge-path *base-path* f)
             *streaming* false]
     (run-string (extract-file-content f) opts))))

(defn inspect-warnings []
  (:warnings @environ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scratch

(comment
  #_(run-file "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/load-file.edn")

  #_(run-file-data "test/edns/load-file.edn")

  #_(run-string-data "(env \"PATH\")")
  
  #_(run-file "test/edns/load-blocked.edn")

  (run-file-data "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/env.edn")
  
  #_(i/init-env! (atom {})  nil nil nil)

  ;; json

  #_(j/write-value-as-string (run-file "test/edns/load-file.edn"))

  ;; yaml

  #_(y/generate-string r))
