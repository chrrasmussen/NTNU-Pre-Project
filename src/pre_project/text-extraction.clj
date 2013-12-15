(ns pre-project.text-extraction
  (:import [org.jsoup Jsoup]))


;; Extract-text

;; (defn find-all-macros
;;   [html]
;;   (let [doc (Jsoup/parse html)
;;         macros (.getElementsByTag doc "ac:macro")]
;;     macros))
;; ;;     (distinct (map #(.attr % "ac:name") macros))))

(defn- in?
  "true if seq contains elm"
  [coll element]
  (some #(= element %) coll))

(defn extract-text
  [file]
  (let [doc (Jsoup/parse file "utf-8")
        macro-blacklist '("metadata-list", "code", "plantuml", "chart", "lucidchart", "pagetree", "spacegraph", "htmlcomment") ;; excerpt
        remove-macros #(if (in? macro-blacklist (.attr % "ac:name")) (.remove %) nil)]
    ;; Remove blacklisted 'ac:macro' elements
    (doall (map remove-macros (.getElementsByTag doc "ac:macro")))

    ;; Remove all 'code' elements
    (-> doc (.getElementsByTag "code") .remove)

    ;; Remove all 'ac:parameter' elements
    (-> doc (.getElementsByTag "ac:parameter") .remove)

    ;; Get text from special links
    (doall (map #(.text % (.attr % "ri:content-title")) (.getElementsByTag doc "ri:page")))
    (.html doc)))


;; (def path "data/documents/Faginnhold.txt")
;; (def path "data/documents/Objektorientert programmering/Objektorientert programmering.txt")
;; (def path "data/documents/Eclipse/Refactoring/Refactoring.txt")
;; (let [file (clojure.java.io/as-file path)]
;;   (println (extract-text file)))
