(ns nrepl-ws.transducers)

(defn partition-when [pred]
  (fn [rf]
    (let [buf (atom [])]
      (fn
        ([] (println "arity 0 init:") (rf))
        ([result] (println "arity 1 completion:" @buf result) result)
        ([result input]
         (println "arity 2 step:" @buf result input)
         (swap! buf conj input)
         (if (pred input)
           (let [fin @buf] (reset! buf []) (rf result fin))
           result))))))