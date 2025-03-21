(ns noj-getting-started
  (:require
   [clojure.string :as str]
   [scicloj.clay.v2.api :as clay]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.tableplot.v1.plotly :as plotly]
   [tablecloth.api :as tc]))

;; In this tutorial we analyse the data of the
;; [Clojure Calendar Feed](https://clojureverse.org/t/the-clojure-events-calendar-feed-turns-2/).

;; ## Setup

(defonce feed-string
  (slurp "https://www.clojurians-zulip.org/feeds/events.ics"))

;; ## Initial exploration

(kind/hiccup
 [:div {:style {:max-height "400px"
                :overflow-y :auto}}
  (kind/code
   feed-string)])

;; ## Data parsing

(def feed-dataset
  (-> feed-string
      (str/split #"END:VEVENT\nBEGIN:VEVENT")
      (->> (map (fn [event-string]
                  (-> event-string
                      str/split-lines
                      (->> (map (fn [event-line]
                                  (when-let [k (re-find #"URL:|SUMMARY:|DTSTART:" event-line)]
                                    [(-> k
                                         (str/replace ":" "")
                                         str/lower-case
                                         keyword)
                                     (-> event-line
                                         (str/replace k ""))])))
                           (into {}))))))
      (tc/dataset {:parser-fn {:dtstart [:local-date-time
                                         "yyyyMMdd'T'HHmmss'Z'"]}})
      (tc/set-dataset-name "Clojure Calendar Feed")))

;; ## Alex added: Convert dtstart column to java.util.Date so that #inst tagged literals work
(def feed-dataset (tc/map-columns feed-dataset :dtstart #(java-time/java-date (java-time/zoned-date-time % "UTC"))))

;; ## Exploring the dataset

feed-dataset

;; ## Plotting the time-series

(-> feed-dataset
    (tc/order-by [:dtstart])
    (tc/add-column :count
                   (fn [ds]
                     (range (tc/row-count ds))))
    (plotly/layer-point {:=x :dtstart
                         :=y :count}))

;; ## Alex added: trying to understand kindly

(def kind-plotly (-> feed-dataset
                     (tc/order-by [:dtstart])
                     (tc/add-column :count
                                    (fn [ds]
                                      (range (tc/row-count ds))))
                     (plotly/layer-point {:=x :dtstart
                                          :=y :count})))

(-> {:value (kind/plotly kind-plotly)}
    scicloj.kindly-advice.v1.api/advise
    kind/pprint)

;; ## Alex added: trying to understand clay using kindly

(clay/make-hiccup {:single-form (quote (-> feed-dataset
                                           (tc/order-by [:dtstart])
                                           (tc/add-column :count
                                                          (fn [ds]
                                                            (range (tc/row-count ds))))
                                           (plotly/layer-point {:=x :dtstart
                                                                :=y :count})))})

;; ## Alex added: trying to understand clojure string serialization

(pr (clay/make-hiccup {:single-form (quote (-> feed-dataset
                                           (tc/order-by [:dtstart])
                                           (tc/add-column :count
                                                          (fn [ds]
                                                            (range (tc/row-count ds))))
                                           (plotly/layer-point {:=x :dtstart
                                                                :=y :count})))}))
