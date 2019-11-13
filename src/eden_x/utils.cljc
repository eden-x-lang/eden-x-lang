(ns eden-x.utils)

(defn warning [title details]
  (binding [*out* *err*]
    (println "\nWARNING!!" title)
    (doseq [detail details]
      (println "-" detail))
    (println)))

(defn error [title details]
  (binding [*out* *err*]
    (println "\nERROR!!" title)
    (doseq [detail details]
      (println "-" detail))
    (println)))
