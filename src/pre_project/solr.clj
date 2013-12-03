(ns pre-project.solr
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))


;; Insert document

(defn- generate-insert-document-json-data
  [id title content]
  (let [data [{:id id, :title title, :content content}]]
    (json/generate-string data)))

(defn- parse-insert-document-result
  [body]
  (let [status (get-in (json/parse-string body) ["responseHeader" "status"])]
    (zero? status)))

(defn insert-document
  [id title content]
  (let [url "http://localhost:8983/solr/update?commit=true"
        options {:headers {"Content-Type" "application/json"}
                 :body (generate-insert-document-json-data id title content)}
        {:keys [status headers body error]} @(http/post url options)]
    (if-not error
      [(parse-insert-document-result body) nil]
      [nil error])))


;; Get popular words

(defn- parse-get-popular-words-result
  [body]
  (let [words-vector (get-in (json/parse-string body) ["termVectors" 3 3])
        ;; => ["a", ["tf-idf", 0.5], "b", ["tf-idf", 0.25]]

        words-map (apply hash-map words-vector)
        ;; => {"a" ["tf-idf", 0.5], "b" ["tf-idf", 0.25]}

        words-map-list (for [[k v] words-map] {:word k, :tf-idf (second v)})
        ;; => ({:word "a" :tf-idf 0.5} {:word "b" :tf-idf 0.25})

        sorted-words-map-list (sort-by :tf-idf words-map-list)
        ;; => ({:word "b" :tf-idf 0.25} {:word "a" :tf-idf 0.5})

        sorted-words (map :word sorted-words-map-list)]
        ;; => ("b" "a")
    (reverse sorted-words)))

(defn get-popular-words
  [docid]
  (let [url "http://localhost:8983/solr/tvrh"
        escaped-docid (clojure.string/join "\\ " (clojure.string/split docid #" "))
        options {:query-params {:q (str "id:" escaped-docid)
                                :tv.tf_idf true
                                :wt "json"}}
        {:keys [status headers body error]} @(http/post url options)]
    (if-not error
      [(parse-get-popular-words-result body) nil]
      [nil error])))


;; Clear database

(defn- generate-clear-database-json-data
  []
  (let [data {:delete {:query "*:*"}}]
    (json/generate-string data)))

(defn- parse-clear-database-result
  [body]
  (let [status (get-in (json/parse-string body) ["responseHeader" "status"])]
    (zero? status)))

(defn clear-database
  []
  (let [url "http://localhost:8983/solr/update?commit=true"
        options {:headers {"Content-Type" "application/json"}
                 :body (generate-clear-database-json-data)}
        {:keys [status headers body error]} @(http/post url options)]
    (if-not error
      [(parse-clear-database-result body) nil]
      [nil error])))
