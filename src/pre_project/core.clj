(ns pre-project.core
  (:require [clojure.core.async :as async :refer :all :rename {map a-map, into a-into, reduce a-reduce, merge a-merge, partition-by a-partition-by, partition a-partition, take a-take}]
            [clojure.java.io :as io]
            [pre-project.tika :as tika]
            [pre-project.solr :as solr]
            [pre-project.google-translate :as google-translate]
            [pre-project.google-search :as google-search]))


;; Train

(defn- get-text-files
  [path]
  (let [directory (io/file path)
        files (file-seq directory)
        text-file? #(-> % .getName (.endsWith ,,, ".txt"))]
    (filter text-file? files)))

(defn- insert-file
  [file]
  (let [filename (.getName file)

        docid filename

        title-id (str filename "#title")
        title (clojure.string/replace filename #".txt$" "")
        [translated-title _] (google-translate/translate title-id title)

        text-id (str filename "#content")
        text (tika/extract-text file)
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


;; Examples

(println (get-links "Collection-rammeverket.txt"))
;; (println (train "data/documents/"))
;; (println (insert-file (io/file "data/documents/Faginnhold.txt")))

;; Tika
;; (println (tika/extract-text "data/documents/Faginnhold.txt"))

;; Solr
;; (println (solr/insert-document "ID" "TITLE" "CONTENT"))
;; (println (solr/get-popular-words "Feil og advarsler i editoren.txt"))
;; (println (solr/clear-database))

;; Google Translate
;; (println (google-translate/translate "Dette er en test"))
;; (println (google-translate/translate "ID" "Dette er en test"))

;; Google Search
;; (println (google-search/search "test"))
