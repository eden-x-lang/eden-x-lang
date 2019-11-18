(ns user
  (:require [eden-x.core :as eden]))

(comment
  (eden/run-file "test/edns/load-file.edn")

  (eden/run-file "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/load-file.edn"))

(eden/run-file-data "test/edns/load-file-with-unfrozen-transitive.edn")

(count (eden/inspect-warnings))

eden/environ
