(ns pre-project.tika
  (:import [org.apache.tika.parser.html HtmlParser]
           [org.apache.tika.sax BodyContentHandler]
           [org.apache.tika.metadata Metadata]))


;; Extract-text

(defn- clean-whitespace
  [text]
  (clojure.string/replace text #"\s+" " "))

(defn extract-text
  [file]
  (let [pdf-parser (new HtmlParser)
        content-handler (new BodyContentHandler)
        metadata (new Metadata)]
    (with-open [input (java.io.FileInputStream. file)]
      (.parse pdf-parser input content-handler metadata))
    (-> content-handler .toString clean-whitespace)))
