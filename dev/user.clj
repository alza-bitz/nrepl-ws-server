(ns user
  (:require
   [clojure.core.async :as async]
   [nrepl-ws.client :as client]
   [nrepl-ws.server.core :as server]
   [scicloj.clay.v2.api :as clay]))

;; Start the server
(comment
  (def server (server/start-server :port 7888)))

(comment
  server)

;; Start clay server
(comment
  (clay/start! {:browse false
                :port 7890}))

;; Register different fn for plotly
(comment
  (require '[clay.item])
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

;; Stop clay server
(comment
  (scicloj.clay.v2.api/stop!))

(comment
  (let [code-bad (slurp "dev/bad.clj")
        code-good (clojure.string/replace code-bad "\"" "\\\"")]
    (eval (read-string (format "(spit \"/tmp/clay_getting_started.clj\" \"%s\")" code-good)))))