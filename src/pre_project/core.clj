(ns pre-project.core
  (:require [clojure.core.async :as async :refer :all :rename {map a-map, into a-into, reduce a-reduce, merge a-merge, partition-by a-partition-by, partition a-partition, take a-take}]
            [clojure.java.io :as io]
            [pre-project.tika :as tika]
            [pre-project.solr :as solr]
            [pre-project.google-translate :as google-translate]
            [pre-project.google-search :as google-search]
            [selmer.parser :refer :all]))


;; Train

(defn- get-text-files
  [path]
  (let [directory (io/file path)
        files (file-seq directory)
        text-file? #(-> % .getName (.endsWith ,,, ".txt"))]
    (filter text-file? files)))

(defn- insert-file
  [file collection]
  (let [filename (.getName file)

        doc-id filename

        title-id (str filename "#title")
        title (clojure.string/replace filename #".txt$" "")
        [translated-title _] (google-translate/translate title-id title)

        text-id (str filename "#content")
        text (tika/extract-text file)
        [translated-text _] (google-translate/translate text-id text)

        [success _] (solr/insert-document collection doc-id translated-title translated-text)]
    success))

(defn train
  [path collection]
  (let [text-files (get-text-files path)
        inserted-files-status (map #(insert-file % collection) text-files)]
    inserted-files-status))


;; Get links

(defn get-links
  [collection doc-id]
  (let [[words _] (solr/get-popular-words collection doc-id)
        query (clojure.string/join " " (take 5 (map :word words)))
        [links _] (google-search/search query)]
    links))


;; Examples

;; (println (take 5 (get-links "Collection-rammeverket.txt")))
;; (println (train "data/documents/"))
;; (println (insert-file (io/file "data/documents/Faginnhold.txt")))

;; Tika
;; (println (tika/extract-text (get-text-file "data/documents/" "Tilstand og oppførsel.txt")))

;; Solr
;; (println (solr/insert-document "en" "ID" "TITLE" "CONTENT"))
(let [[words _] (solr/get-popular-words "en" "Collection-rammeverket.txt")]
  (println (take 5 words)))
;; (println (solr/clear-database "en"))

;; Google Translate
;; (println (google-translate/translate "Dette er en test"))
;; (println (google-translate/translate "ID" "Dette er en test"))

;; Google Search
;; (println (google-search/search "test"))

(defn- get-average-tf-idf
  ([n words]
   (let [top-words (take n words)
         sum (reduce + (map :tf-idf top-words))
         avg (/ sum (count top-words))]
     avg))
  ([words]
   (get-average-tf-idf 5 words)))

(defn- get-stats-for-document
  [file]
    (let [filename (.getName file)
          doc-id filename
          text (tika/extract-text file)
          [words _] (solr/get-popular-words doc-id)]
      {:doc-id doc-id
       :max-tf-idf (:tf-idf (first words))
       :avg-tf-idf (get-average-tf-idf 1000 words)
       :text-size (count text)}))

(defn- get-stats-for-documents
  [path]
  (let [files (get-text-files path)]
    (map get-stats-for-document files)))

(let [stats (get-stats-for-documents "data/documents/")
      sorted-stats (reverse (sort-by :avg-tf-idf stats))]
  (println sorted-stats))

;; (let [doc-id "Faginnhold.txt"
;;       [words _] (solr/get-popular-words doc-id)
;;       avg (get-average-tf-idf words)]
;;   (println avg))

;; Latex

(defn- get-experiment-data
  [doc-id]
  (let [title (clojure.string/replace doc-id #".txt$" "")
        id (clojure.string/replace (clojure.string/lower-case title) #"[^\w]+" "-")
        [words _] (solr/get-popular-words doc-id)
        query (clojure.string/join " " (take 5 (map :word words)))
        [pages _] (google-search/search query)]
    {:title title
     :id id
     :query query
     :words (take 5 words)
     :pages (take 5 pages)}))

(defn- experiment-to-latex
  [doc-id]
  (let [template (slurp "templates/experiment.mustache")]
    (render template (get-experiment-data doc-id)
            {:tag-open \<
             :tag-close \>})))

(println (experiment-to-latex "JavaFX.txt"))
