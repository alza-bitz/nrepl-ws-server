(ns nrepl-ws.transducers
  (:require [clojure.string :as str]))

(defn partition-when [pred]
  (fn [rf]
    (let [buf (atom [])]
      (fn
        ([] (rf))
        ([result] result)
        ([result input]
         (swap! buf conj input)
         (if (pred input)
           (let [fin @buf] (reset! buf []) (rf result fin))
           result))))))

(defn join-when [pred str-fn]
  (fn [rf]
    (let [buf (atom [])] 
      (fn
        ([] (rf))
        ([result] result)
        ([result input]
         (swap! buf conj input)
         (if (pred input)
           (let [fin @buf] (reset! buf []) (rf result (str/join (map str-fn fin))))
           result))))))