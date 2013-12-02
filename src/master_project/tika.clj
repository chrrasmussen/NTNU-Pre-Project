(ns master-project.tika
  (:import [org.apache.tika.parser.html HtmlParser]
           [org.apache.tika.sax BodyContentHandler]
           [org.apache.tika.metadata Metadata]))

(declare extract-text)

(println (extract-text "data/documents/Faginnhold.txt"))


;; Extract-text

(defn- clean-whitespace
  [text]
  (clojure.string/replace text #"\s+" " "))

(defn extract-text
  [path]
  (let [file (clojure.java.io/file path)
        pdf-parser (new HtmlParser)
        content-handler (new BodyContentHandler)
        metadata (new Metadata)]
    (with-open [input (java.io.FileInputStream. file)]
      (.parse pdf-parser input content-handler metadata))
    (-> content-handler .toString clean-whitespace)))
