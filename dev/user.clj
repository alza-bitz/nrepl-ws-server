(ns user
  (:require
   [clojure.core.async :as async]
   [nrepl-ws.client :as client]
   [nrepl-ws.server.main :refer [system-config]]
   [scicloj.clay.v2.api :as clay]
   [integrant.core :as ig]
   ))

;; Start the server
(comment
  (def system (ig/init system-config)))

(comment
  system)

;; Register custom fn for plotly
(comment
  (require '[clay.item])
  (scicloj.clay.v2.prepare/add-preparer!
   :kind/plotly
   #'clay.item/react-js-plotly))

;; Confirm custom fn is registered
(comment
  @scicloj.clay.v2.prepare/*kind->preparer)

;; Create client
(comment
  (def client (client/create-client
               (str "ws://localhost:"
                    (get-in system [:server/ws :port])))))

;; Send eval op
(comment
  (client/send! client {:op "eval"
                        :code "(+ 2 3)"}))

;; Get result
(comment
  (async/<!! (:msg-ch client)))

;; Stop the client
(comment
  (client/close! client))

;; Stop the server
(comment
  (ig/halt! system))