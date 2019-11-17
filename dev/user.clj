(ns user
  (:require [eden-x.core :as eden]))

(comment
  (eden/run-file "test/edns/load-file.edn")

  (eden/run-file "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/load-file.edn"))


(println (with-out-str (binding [eden/*stderr* *out*
                                 eden/*foo* "blah blah"]
                         (eden/run-file-data "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/env.edn"))))
