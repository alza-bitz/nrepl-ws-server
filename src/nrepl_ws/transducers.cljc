(ns nrepl-ws.transducers)

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