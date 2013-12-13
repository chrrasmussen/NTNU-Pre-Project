(ns pre-project.core
  (:require [clojure.java.io :as io]
            [pre-project.tika :as tika]
            [pre-project.solr :as solr]
            [pre-project.google-translate :as google-translate]
            [pre-project.google-search :as google-search]
            [selmer.parser :refer :all]))


(def ^:private iteration "")
(def ^:private title-suffix (str "#title" iteration))
(def ^:private text-suffix (str "#content" iteration))
(def ^:private en-collection (str "en" iteration))
(def ^:private no-collection (str "no" iteration))
(def ^:private selected-collection en-collection)
(def ^:private selected-doc-id "Faginnhold.txt")
(def ^:private data-path "data/documents/")


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
        doc-id filename

        title-id (str filename title-suffix)
        title (clojure.string/replace filename #".txt$" "")
        [translated-title _] (google-translate/translate title-id title)

        text-id (str filename text-suffix)
        text (tika/extract-text file)
        [translated-text _] (google-translate/translate text-id text)

        [en-success _] (solr/insert-document en-collection doc-id translated-title translated-text)
        [no-success _] (solr/insert-document no-collection doc-id title text)]
    [en-success no-success]))

(defn train
  [path]
  (let [text-files (get-text-files path)
        inserted-files-status (map insert-file text-files)]
    inserted-files-status))


;; Get links

(defn- translate-word
  [word]
  (assoc word :word (first (google-translate/translate (:word word) (:word word)))))

(defn get-links
  [collection doc-id]
  (let [[words _] (solr/get-popular-words collection doc-id)
        top-words (take 5 words)
        translated-words (if (= collection "no") (map translate-word top-words) top-words)
        query (clojure.string/join " " (map :word translated-words))
        [links _] (google-search/search query)]
    links))


;; Document stats


(defn- get-average-tf-idf
  ([n words]
   (let [top-words (take n words)
         sum (reduce + (map :tf-idf top-words))
         avg (/ sum (count top-words))]
     avg))
  ([words]
   (get-average-tf-idf 5 words)))

(defn- get-stats-for-document
  [file collection]
    (let [filename (.getName file)
          doc-id filename
          text (tika/extract-text file)
          [words _] (solr/get-popular-words collection doc-id)]
      {:doc-id doc-id
       :top-word (first words)
       :avg-tf-idf (get-average-tf-idf 5 words)
       :word-count (count (clojure.string/split text #"\w+"))}))

(defn- get-stats-for-documents
  [path collection]
  (let [files (get-text-files path)]
    (map #(get-stats-for-document % collection) files)))


;; Latex

(defn- get-experiment-data
  [collection doc-id]
  (let [title (clojure.string/replace doc-id #".txt$" "")
        id (clojure.string/replace (clojure.string/lower-case title) #"[^\w]+" "-")
        [words _] (solr/get-popular-words collection doc-id)
        query (clojure.string/join " " (take 5 (map :word words)))
        [pages _] (google-search/search query)]
    {:title title
     :id id
     :query query
     :words (take 5 words)
     :pages (take 5 pages)}))

(defn- experiment-to-latex
  [collection doc-id]
  (let [template (slurp "templates/experiment.mustache")]
    (render template (get-experiment-data collection doc-id)
            {:tag-open \<
             :tag-close \>})))

(defn- stats-to-latex
  [path collection]
  (let [template (slurp "templates/stats.mustache")
        stats (get-stats-for-documents path collection)
        sorted-stats (reverse (sort-by :avg-tf-idf stats))]
    (render template {:documents (take 10 sorted-stats)}
            {:tag-open \<
             :tag-close \>})))


;; Examples

;; (println (take 5 (get-links selected-collection selected-doc-id)))
;; (solr/clear-database en-collection)
;; (solr/clear-database no-collection)
;; (println (train data-path))
;; (println (insert-file (io/file (str data-path selected-doc-id))))

;; Tika
;; (println (tika/extract-text (first (get-text-files data-path))))

;; Solr
;; (println (solr/insert-document selected-collection "ID" "TITLE" "CONTENT"))
;; (let [[words _] (solr/get-popular-words selected-collection selected-doc-id)]
;;   (println (take 5 words)))
;; (println (solr/clear-database en-collection))

;; Google Translate
;; (println (google-translate/translate "Dette er en test"))
;; (println (google-translate/translate "ID" "Dette er en test"))

;; Google Search
;; (println (google-search/search "test"))

;; Document stats
(let [stats (get-stats-for-documents data-path selected-collection)
      sorted-stats (reverse (sort-by :avg-tf-idf stats))]
  (println sorted-stats))

;; Latex
;; (println (experiment-to-latex selected-doc-id))
(println (stats-to-latex data-path no-collection))
