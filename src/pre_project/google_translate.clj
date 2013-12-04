(ns pre-project.google-translate
  (:require [clojure.java.io :as io]
            [clojure.contrib.properties :as properties]
            [digest]
            [org.httpkit.client :as http]
            [cheshire.core :as json])
  (:import [clojure.java.io]))


(def ^:private ^:const config (properties/read-properties "config/google_translate.properties"))
(def ^:private ^:const google-api-key (get config "api-key"))
(def ^:private ^:const translate-from (get config "translate-from"))
(def ^:private ^:const translate-to (get config "translate-to"))
(def ^:private ^:const cache-path (get config "cache-path"))


;; Translate

(defn- parse-translate-result
  [body]
  (get-in (json/parse-string body) ["data" "translations" 0 "translatedText"]))

(defn- direct-translate
  [text]
  (let [url "https://www.googleapis.com/language/translate/v2"
        options {:headers {"X-HTTP-Method-Override" "GET"}
                 :query-params {:key google-api-key
                                :source translate-from
                                :target translate-to}
                 :form-params {:q text}}
        {:keys [status headers body error]} @(http/post url options)]
    (if-not error
      [(parse-translate-result body) nil]
      [nil error])))


;; Cached translate

(defn- get-cache
  [id]
  (let [hashed-id (digest/md5 id)
        path (str cache-path hashed-id ".json")]
    (if (.exists (io/as-file path))
      (json/parse-string (slurp path))
      nil)))

(defn- save-cache
  [id checksum text]
  (let [hashed-id (digest/md5 id)
        path (str cache-path hashed-id ".json")
        data {:id id
              :checksum checksum
              :text text}
        serialized-data (json/generate-string data)]
    (spit path serialized-data)))

(declare translate)
(defn- cached-translate
  [id text]
  (let [cache (get-cache id)
        checksum (digest/md5 text)
        checksum-matches? (= (get cache "checksum") checksum)]
    (if checksum-matches?
      [(get cache "text") nil]
      (let [[translated-text error] (translate text)]
        (save-cache id checksum translated-text)
        [translated-text error]))))

(defn translate
  ([text] (direct-translate text))
  ([id text] (cached-translate id text)))
