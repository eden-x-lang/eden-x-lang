(ns eden-x.core-test
  (:refer-clojure :exclude [load-file])
  (:require [clj-yaml.core :as y]
            [clojure.edn :as edn]
            [clojure.test :refer :all]
            [eden-x.core :as eden]
            [jsonista.core :as j])
  (:import (clojure.lang ExceptionInfo)))

(def ^:private base-url
  "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/")

(deftest simple-evals-and-formatting
  (let [t-edn {:a 4 :b [0 1 2] :c {:a/a 1 :a/b 2}}
        t-dirty-edn {"a" 4 "b" [0 1 2] "c" {"a/a" 1, "a/b" 2}}
        t-s-edn "{:c #:a{:a 1, :b 2}, :b [0 1 2], :a 4}\n"
        t-s-c-edn "{:c #:a{:a 1, :b 2}, :b [0 1 2], :a 4}"
        t-s-json "{\n  \"c\" : {\n    \"a/a\" : 1,\n    \"a/b\" : 2\n  },\n  \"b\" : [ 0, 1, 2 ],\n  \"a\" : 4\n}"
        t-s-c-json "{\"c\":{\"a/a\":1,\"a/b\":2},\"b\":[0,1,2],\"a\":4}"
        t-s-yaml "c:\n  a: 1\n  b: 2\nb:\n- 0\n- 1\n- 2\na: 4\n"
        t-s-c-yaml "c: {a: 1, b: 2}\nb: [0, 1, 2]\na: 4\n"
        e "
(def x 1)
(let [y 3]
  {:a (+ x y)
   :b (mapv identity (range y))
   :c {:a/a 1 :a/b 2}})
"]
    ;; basic and edn modes
    (is (= t-edn (eden/run-string-data e)))
    (is (= t-s-edn (eden/run-string e)))
    (is (= t-edn (-> e eden/run-string edn/read-string)))
    (is (= t-s-edn (eden/run-string e {:type :edn})))
    (is (= t-edn (-> e (eden/run-string {:type :edn}) edn/read-string)))
    (is (= t-s-c-edn (eden/run-string e {:type :edn :compact true})))
    (is (= t-edn (-> e (eden/run-string {:type :edn :compact true}) edn/read-string)))
    (is (= t-s-c-edn (eden/run-string e {:compact true})))
    (is (= t-edn (-> e (eden/run-string {:compact true}) edn/read-string)))

    ;; string mode (json)
    (is (= t-s-json (-> e (eden/run-string {:type :json}))))
    (is (= t-s-c-json (-> e (eden/run-string {:type :json :compact true}))))
    (is (= t-dirty-edn (-> e (eden/run-string {:type :json}) j/read-value)) )
    (is (= t-dirty-edn (-> e (eden/run-string {:type :json :compact true}) j/read-value)) )

    ;; string mode (yaml)
    (is (= t-s-yaml (-> e (eden/run-string {:type :yaml}))))
    (is (= t-s-c-yaml (-> e (eden/run-string {:type :yaml :compact true}))))))

(deftest simple-file-should-load
  (let [r (eden/run-file-data "test/edns/simple.edn")
        t (edn/read-string (slurp "test/edns/simple.edn"))]
    (is (= t r))))

(deftest env-should-work
  (is (not= 0 (-> "(env \"PATH\")" eden/run-string-data count)))
  (is (= 0 (-> "(env \"FOO_BAR\")" eden/run-string-data count)))
  ;; now from a file
  (let [{:keys [pwd path]} (eden/run-file-data "test/edns/env.edn")]
    (is (not= 0 (count path)))
    (is (not= 0 (count pwd)))))

(deftest remote-env-should-warn-and-return-nil
  (let [{:keys [pwd path]} (eden/run-file-data (str base-url "test/edns/env.edn"))
        warnings (eden/inspect-warnings)]
    (is (nil? path))
    (is (nil? pwd))
    (is (= 2 (count warnings)))
    (is (every? #(= ::eden/illegal-remote-operation
                    (::eden/category %))
                warnings))
    (is (every? #(= "Illegal operation from remote file"
                    (::eden/title %))
                warnings))))

(deftest load-file-without-transitive
  (let [{:keys [my-value
                other-def]
         :as r} (eden/run-file-data "test/edns/load-file-without-remote-transitive.edn")]
    (is (= 11 my-value))
    (is (= 132 other-def))
    (is (= #{:other-def :my-value} (-> r keys set)))
    (is (= 0 (count (eden/inspect-warnings))))))

(deftest load-file-with-frozen-transtive
  (let [{:keys [my-value
                other-def
                online]
         :as r} (eden/run-file-data "test/edns/load-file-with-frozen-transitive.edn")]
    (is (= 11 my-value))
    (is (= 132 other-def))
    (is (= 6 online))
    (is (= #{:my-value :other-def :online} (-> r keys set)))
    (is (= 0 (count (eden/inspect-warnings))))))

(deftest load-file-unfrozen-transitive-should-warn
  (let [{:keys [online]
         :as r} (eden/run-file-data "test/edns/load-file-with-unfrozen-transitive.edn")
        warnings (eden/inspect-warnings)]
    (is (= 6 online))
    (is (= #{:online} (-> r keys set)))
    (is (= 1 (count warnings)))
    (is (= ::eden/transitive-unfrozen-load
           (-> warnings first ::eden/category)))
    (is (= "Remote file being loaded transitively without freeze"
           (-> warnings first ::eden/title)))))

;; TODO should warn for env and transitive and block
#_(deftest load-file-unfrozen-transitive-env-should-warn-and-nil-env)

#_(deftest load-file-wrong-freeze-hash-should-throw)

(deftest make-sure-load-files-are-isolated
  (is (thrown-with-msg?
       ExceptionInfo #"Could not resolve symbol"
       (eden/run-string-data "(def a 10) {:out (load-file \"test/edns/undefined.edn\")}"))))

#_(deftest should-throw-on-missing-local-file)

#_(deftest should-throw-on-missing-remote-file)

#_(deftest should-throw-on-invalid-script)
