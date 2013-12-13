(ns pre-project.solr
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))


(def ^:private ^:const base-url "http://localhost:8983/solr")

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
  [collection doc-id title content]
  (let [url (format "%s/%s/update?commit=true" base-url collection)
        options {:headers {"Content-Type" "application/json"}
                 :body (generate-insert-document-json-data doc-id title content)}
        {:keys [status headers body error]} @(http/post url options)]
    (if-not error
      [(parse-insert-document-result body) nil]
      [nil error])))


;; Get popular words

(defn- parse-get-popular-words-result
  [body]
  (let [words-vector (get-in (json/parse-string body) ["termVectors" 3 3])
        ;; => ["a", ["tf", 1, "df", 2, "tf-idf", 0.5], "b", ["tf", 1, "df", 4, "tf-idf", 0.25]]

        words-map (apply hash-map words-vector)
        ;; => {"a" ["tf", 1, "df", 2, "tf-idf", 0.5], "b" ["tf", 1, "df", 4, "tf-idf", 0.25]}
;;         _ (println words-map)
        words-map-list (for [[k v] words-map] {:word k
                                               :tf (nth 2 v)
                                               :df (nth 4 v)
                                               :tf-idf (nth 6 v)})
        ;; => ({:word "a", :tf 1, :df 2, :tf-idf 0.5} {:word "b", :tf 1, :df 4, :tf-idf 0.25})

        sorted-words-map-list (sort-by :tf-idf words-map-list)]
        ;; => ({:word "b", :tf 1, :df 2, :tf-idf 0.25} {:word "a", :tf 1, :df 4, :tf-idf 0.5})
    (reverse sorted-words-map-list)))

(defn get-popular-words
  [collection doc-id]
  (let [url (format "%s/%s/tvrh" base-url collection)
        escaped-doc-id (clojure.string/join "\\ " (clojure.string/split doc-id #" "))
        options {:query-params {:q (str "id:" escaped-doc-id)
                                :tv.all true
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
  [collection]
  (let [url (format "%s/%s/update?commit=true" base-url collection)
        options {:headers {"Content-Type" "application/json"}
                 :body (generate-clear-database-json-data)}
        {:keys [status headers body error]} @(http/post url options)]
    (if-not error
      [(parse-clear-database-result body) nil]
      [nil error])))
