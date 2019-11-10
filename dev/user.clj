(ns user
  (:require [sci.core :as sci]
            [sci.impl.interpreter :as i]))

#_(defn import [x]
    nil)

(sci/eval-string "str/trim" #_{:bindings {'import import}})


(clojure.pprint/pprint (i/opts->ctx nil))


(i/init-env! (atom {})  nil nil nil)
