(ns pre-project.text-extraction
  (:import [org.jsoup Jsoup]))


;; Extract-text
;; pagetree, spacegraph, htmlcomment
(defn find-all-macros
  [html]
  (let [doc (Jsoup/parse html)
        macros (.getElementsByTag doc "ac:macro")]
    macros))
;;     (distinct (map #(.attr % "ac:name") macros))))

(defn- in?
  "true if seq contains elm"
  [coll element]
  (some #(= element %) coll))

(defn extract-text
  [file]
  (let [doc (Jsoup/parse file "utf-8")
        macro-blacklist '("metadata-list", "code", "plantuml")
        remove-macros #(if (in? macro-blacklist (.attr % "ac:name")) (.remove %) nil)]
    (println (.select doc "code"))
    ;; Remove blacklisted macros
    (doall (map remove-macros (.getElementsByTag doc "ac:macro")))

    ;; Remove all parameters
    (-> doc (.getElementsByTag "ac:parameter") .remove)

    ;; Get text from special links
    (doall (map #(.text % (.attr % "ri:content-title")) (.getElementsByTag doc "ri:page")))
    (.html doc)))


;; (def path "data/documents/Faginnhold.txt")
;; (def path "data/documents/Objektorientert programmering/Objektorientert programmering.txt")
;; (def path "data/documents/Ti.txt")
;; (let [file (clojure.java.io/as-file path)]
;;   (println (find-all-macros (extract-text file))))