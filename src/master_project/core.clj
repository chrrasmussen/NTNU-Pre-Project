(ns master-project.core
  (:require [clojure.core.async :as async :refer :all :rename {map a-map, into a-into, reduce a-reduce, merge a-merge, partition-by a-partition-by, partition a-partition, take a-take}]
            [clojure.java.io :as io]
            [master-project.tika :as tika]
            [master-project.solr :as solr]
            [master-project.google-translate :as google-translate]
            [master-project.google-search :as google-search]))

;; (defn get-text-files
;;   [path]
;;   (let [directory (io/file path)
;;         files (file-seq directory)
;;         text-file? #(-> % .getName (.endsWith ".txt"))]
;;     (filter text-file? files)))

;; (defn translate-file-at-path
;;   [path callback]
;;   (let [text (extract-text path)]
;;     (translate text callback)))

;; (defn train
;;   [path]
;;   ())

;; (defn- search
;;   [words]
;;   (let [query (clojure.string/join " " words)]
;;     (println (str "Searching for: " query))
;;     (google-search/search query #(println (take 3 %)))))


;; (defn fn-with-callback
;;   [value callback]
;;   (callback value))

;; (fn-with-callback "test" println)

;; (let
;;   [ch (chan)]
;;   (go (fn-with-callback '("a" "b") #(>!! ch %)))
;;   (go (println (<!! ch))))

(defn get-links
  [docid]
  (let [words-ch (chan)
        links-ch (chan)]
    (println (str "Finding links for docid: " docid))
    (go (solr/get-popular-words docid #(>!! words-ch %)))
    (go (google-search/search (clojure.string/join " " (<!! words-ch)) #(>!! links-ch %)))
    (go (println (take 3 (<!! links-ch))))))

(get-links "book2")
;; (get-links "book3")

;; (println (get-text-files "/Users/skohorn/Sites/Faginnhold/"))

;; (translate-file-at-path "/Users/skohorn/Sites/Faginnhold/Faginnhold.txt" #(println %))
