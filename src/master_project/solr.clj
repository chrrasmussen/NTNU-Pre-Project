(ns master-project.solr
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(declare insert-document-request
         get-popular-words-request
         clear-database-request)

(defn insert-document
  [id title content callback]
  (insert-document-request id title content callback #()))

;; (insert-document "ID" "TITLE" "CONTENT" println)

(defn get-popular-words
  [docid callback]
  (get-popular-words-request docid callback #()))

;; (get-popular-words "book2" println)

(defn clear-database
  [callback]
  (clear-database-request callback #()))

;; (clear-database println)


;; Private

;; Insert document

(defn- generate-insert-document-json-data
  [id title content]
  (let [data [{:id id, :title title, :content content}]]
    (json/generate-string data {:escape-non-ascii false})))

(defn- parse-insert-document-result
  [body]
  (let [status (get-in (json/parse-string body) ["responseHeader" "status"])]
    (zero? status)))

(defn- insert-document-response-handler
  [success failure]
  (fn [{:keys [status headers body error]}]
    (if-not error
      (success (parse-insert-document-result body))
      (failure error))))

(defn- insert-document-request
  [id title content success failure]
  (let [url "http://localhost:8983/solr/update?commit=true"
        options {:headers {"Content-Type" "application/json"}
                 :body (generate-insert-document-json-data id title content)}]
    (http/post url options (insert-document-response-handler success failure))))


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

(defn- get-popular-words-response-handler
  [success failure]
  (fn [{:keys [status headers body error]}]
    (if-not error
      (success (parse-get-popular-words-result body))
      (failure error))))

(defn- construct-get-popular-words-url
  [id]
  (let [url-format "http://localhost:8983/solr/tvrh?q=id:%s&tv.tf_idf=true&wt=json"]
    (format url-format id)))

(defn- get-popular-words-request
  [docid success failure]
  (let [url "http://localhost:8983/solr/tvrh"
        options {:query-params {:q (str "id:" docid)
                                :tv.tf_idf true
                                :wt "json"}}]
    (http/get url options (get-popular-words-response-handler success failure))))


;; Clear database

(defn- generate-clear-database-json-data
  []
  (let [data {:delete {:query "*:*"}}]
    (json/generate-string data)))

(defn- parse-clear-database-result
  [body]
  (let [status (get-in (json/parse-string body) ["responseHeader" "status"])]
    (zero? status)))

(defn- clear-database-response-handler
  [success failure]
  (fn [{:keys [status headers body error]}]
    (if-not error
      (success (parse-clear-database-result body))
      (failure error))))

(defn- clear-database-request
  [success failure]
  (let [url "http://localhost:8983/solr/update?commit=true"
        options {:headers {"Content-Type" "application/json"}
                 :body (generate-clear-database-json-data)}]
    (http/post url options (clear-database-response-handler success failure))))
