(ns master-project.core
  (:require [clojure.core.async :as async :refer :all :rename {map a-map, into a-into, reduce a-reduce, merge a-merge, partition-by a-partition-by, partition a-partition, take a-take}]
            [clojure.java.io :as io]
            [master-project.tika :as tika]
            [master-project.solr :as solr]
            [master-project.google-translate :as google-translate]
            [master-project.google-search :as google-search]))

(declare get-links
         train)

(println (get-links "Eclipse.txt"))

;; (println (train "data/documents/"))

;; (println (insert-file (io/file "data/documents/Faginnhold.txt")))

;; (println (solr/clear-database))

;; Train

(defn- get-text-files
  [path]
  (let [directory (io/file path)
        files (file-seq directory)
        text-file? #(-> % .getName (.endsWith ".txt"))]
    (filter text-file? files)))

(defn- insert-file
  [file]
  (let [path (.getPath file)
        filename (.getName file)

        docid filename

        title-id (str path "#title")
        title (.substring filename 0 (.lastIndexOf filename "."))
        [translated-title _] (google-translate/translate title-id title)

        text-id (str path "#content")
        text (tika/extract-text (.getPath file))
        [translated-text _] (google-translate/translate text-id text)

        [success _] (solr/insert-document docid translated-title translated-text)]
    success))

(defn train
  [path]
  (let [files (get-text-files path)
        inserted-files (map insert-file files)]
    inserted-files))


;; Get links

(defn get-links
  [docid]
  (let [[words _] (solr/get-popular-words docid)
        query (clojure.string/join " " (take 5 words))
        [links _] (google-search/search query)]
    links))
