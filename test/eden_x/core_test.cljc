(ns eden-x.core-test
  (:require [clj-yaml.core :as y]
            [clojure.edn :as edn]
            [clojure.test :refer :all]
            [eden-x.core :as eden]
            [jsonista.core :as j]))

(deftest simple-evals
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
