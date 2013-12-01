(ns master-project.core
  (:require [clojure.java.io :as io]
            [master-project.tika :as tika]
            [master-project.solr :as solr]
            [master-project.google-translate :as google-translate]
            [master-project.google-search :as google-search]))

(defn get-text-files
  [path]
  (let [directory (io/file path)
        files (file-seq directory)
        text-file? #(-> % .getName (.endsWith ".txt"))]
    (filter text-file? files)))

(defn translate-file-at-path
  [path callback]
  (let [text (extract-text path)]
    (translate text callback)))

;; (println (get-text-files "/Users/skohorn/Sites/Faginnhold/"))

;; (translate-file-at-path "/Users/skohorn/Sites/Faginnhold/Faginnhold.txt" #(println %))
