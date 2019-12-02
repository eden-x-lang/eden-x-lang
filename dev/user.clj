(ns user
  (:require [eden-x.core :as eden]
            [sci.impl.analyzer :as ana]
            [sci.impl.interpreter :as i]
            [sci.impl.parser :as p]
            [clojure.tools.reader.reader-types :as r]))

(comment
  (eden/run-file "test/edns/load-file.edn")

  (eden/run-file "https://raw.githubusercontent.com/eden-x-lang/eden-x-lang/master/test/edns/load-file.edne")

  (eden/run-file-data "test/edns/load-file-without-remote-transitive.edn")
  
  (eden/run-file-data "test/edns/load-file-with-unfrozen-transitive-env.edne")

  (eden/run-string "\nbar")
  
  (count (eden/inspect-warnings))

  (eden/inspect-warnings)
  )


#_(let [f1 (eden/run-string-data "(def a 1) #(str \"4\")")
        f2 (eden/run-string-data "(def a 1) #(str \"4\")")]
    (= f1 f2)
    (meta f2))

(clojure.pprint/pprint (ana/analyze (i/opts->ctx nil)
                                    '#(str "4")))

(clojure.pprint/pprint (ana/analyze (i/opts->ctx nil)
                                    '(let [f1 #(str "4")
                                           f2 (fn [] "5")]
                                       (fn [] [(f1) (f2)]))))

(clojure.pprint/pprint (ana/analyze (i/opts->ctx nil)
                                    '(def f1 "")))

(println (clojure.repl/source-fn 'str))

(p/parse-string "(def a 4) (println a)")

(let [reader (r/indexing-push-back-reader (r/string-push-back-reader "(def a 4) (str a 5)"))]
  (p/parse-next reader nil {}))
