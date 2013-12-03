(ns pre-project.google-search
  (:require [clojure.contrib.properties :as properties]
            [org.httpkit.client :as http]
            [cheshire.core :as json])
  (:import [java.net URLEncoder]))


(def ^:private ^:const config (properties/read-properties "config/google_search.properties"))
(def ^:private ^:const google-api-key (get config "api-key"))
(def ^:private ^:const google-custom-search-id (get config "custom-search-id"))


;; Search

(defn- parse-item
  [item]
  (let [title (get-in item ["title"])
        link (get-in item ["link"])]
    {:title title, :link link}))

(defn- parse-search-result
  [data]
  (let [items (get-in (json/parse-string data) ["items"])]
    (map parse-item items)))

(defn search
  [query]
  (let [url "https://www.googleapis.com/customsearch/v1"
        options {:query-params {:key google-api-key
                                :cx google-custom-search-id
                                :q (URLEncoder/encode query "UTF-8")}}
        {:keys [status headers body error]} @(http/get url options)]
    (if-not error
      [(parse-search-result body) nil]
      [nil error])))


;; Examples

;; (println (search "test"))
