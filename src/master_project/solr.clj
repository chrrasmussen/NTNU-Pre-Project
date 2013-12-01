(ns master-project.solr
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(declare insert-document-request get-popular-words-request)

(defn insert-document
  [id title content callback]
  (insert-document-request id title content callback #()))

;; (insert-document "ID" "TITLE" "CONTENT" println)

(defn get-popular-words
  [id callback]
  (get-popular-words-request id callback #()))

(get-popular-words "Test2.txt" println)

;; Private

(defn- generate-json-data
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
                 :body (generate-json-data id title content)}]
    (http/post url options (insert-document-response-handler success failure))))

(defn- construct-get-popular-words-url
  [id]
  (let [url-format "http://localhost:8983/solr/tvrh?q=id:%s&tv.tf_idf=true&wt=json"]
    (format url-format id)))

(defn- parse-word
  [key value]
  ({:word key
    :tf-idf value}))

(defn- parse-get-popular-words-result
  [body]
  (let [words-vector (get-in (json/parse-string body) ["termVectors" 3 3])
        ;; ["a", ["tf-idf", 0.5], "b", ["tf-idf", 0.25]]

        words-map (apply hash-map words-vector)
        ;; {"a" ["tf-idf", 0.5], "b" ["tf-idf", 0.25]}

        words-map-list (for [[k v] words-map] {:word k, :tf-idf (second v)})
        ;; ({:word "a" :tf-idf 0.5} {:word "b" :tf-idf 0.25})

        sorted-words-map-list (sort-by :tf-idf words-map-list)
        ;; ({:word "a" :tf-idf 0.25} {:word "b" :tf-idf 0.5})

        sorted-words (map :word sorted-words-map-list)]
        ;; ("a" "b")
    (reverse sorted-words)))

(defn- get-popular-words-response-handler
  [success failure]
  (fn [{:keys [status headers body error]}]
    (if-not error
      (success (parse-get-popular-words-result body))
      (failure error))))

(defn- get-popular-words-request
  [id success failure]
  (let [url (construct-get-popular-words-url id)
        options {}]
    (http/get url options (get-popular-words-response-handler success failure))))
