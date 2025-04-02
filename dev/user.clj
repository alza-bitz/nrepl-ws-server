(ns user
  (:require 
   [clojure.core.async :as async]
   [nrepl-ws.client :as client]
   [nrepl-ws.server.core :as server]))

;; Start the server
(comment
  (def server (server/start-server :port 7888)))

(comment
  server)

;; Register different fn for plotly
;; TODO from the client, I get ClassNotFoundException for clay.readers.Plotly
;; I think readers needs to also be manually loaded first before loading item
(comment
  (require '(clay readers item))
  (require '(scicloj.clay.v2 prepare))
  (scicloj.clay.v2.prepare/add-preparer!
   :kind/plotly
   #'clay.item/react-js-plotly))

(comment
  @scicloj.clay.v2.prepare/*kind->preparer
  )

(comment
  (def client (client/create-client
               (str "ws://localhost:"
                    (get-in server [:port])))))

(comment
  (client/send! client {:op "eval"
                        :code "(+ 2 3)"}))

(comment
  (let [response (async/<!! (:msg-ch client))]
    (println response)))

;; Stop the client
(comment
  (client/close! client))

;; Stop the server
(comment
  (server/stop-server server))