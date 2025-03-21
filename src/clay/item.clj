(ns clay.item 
  (:require
   [scicloj.clay.v2.item :as item])
  (:import [clay.readers Plotly]))

(defn react-js-plotly [{:as context
                        {:keys [data layout config]
                         :or {layout {}
                              config {}}} :value}]
  {:hiccup [:div
            {:style (item/extract-style context)}
            [:> (Plotly.)
             {:data data
              :layout layout
              :config config}]]})