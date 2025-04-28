(ns nrepl-ws.server.core
  (:require
   [clojure.core.async :refer [<! >! chan go go-loop]]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [nrepl.core :as nrepl]
   [org.httpkit.server :as http]))

(def state (atom {:nrepl-session (promise)
                  :nrepl-channel (chan)}))

(defn- on-open-handler [transport]
  (fn [ch]
    (log/info "Websocket opened!" (pr-str ch))
    (let [nrepl-client (nrepl/client transport 5000)
          session-id (nrepl/new-session nrepl-client)
          nrepl-session (nrepl/client-session nrepl-client :session session-id)]
      (log/info "nREPL session created with id:" session-id)
      (swap! state assoc :nrepl-session-id session-id)
      (deliver (:nrepl-session @state) nrepl-session))))

(defn- on-receive [ch msg]
  (log/info "Received message:" msg)
  ;; TODO we probably want to set an id on the message if not already set 
  (go
    (>! (:nrepl-channel @state) {:nrepl-msg (json/read-str msg :key-fn keyword)
                                 :ws-channel ch})))

(defn- on-close [ch status-code]
  (log/info "Websocket closed with status code" status-code)
  (swap! state assoc :nrepl-session (promise) :nrepl-session-id nil))

(defn ws-handler [transport]
  (fn [request]
    (if-not (:websocket? request)
      {:status 400
       :body "Websocket connection required"}
      (http/as-channel request
                       {:on-open (on-open-handler transport)
                        :on-receive on-receive
                        :on-close on-close}))))

(defn start
  "Start a WebSocket server that connects to an nREPL server"
  [port nrepl-server]
  (let [nrepl-port (:port nrepl-server)
        nrepl-transport (nrepl/connect :port nrepl-port)
        server (http/run-server (ws-handler nrepl-transport) {:port port})]
    (go-loop []
      (let [msg (<! (:nrepl-channel @state))
            nrepl-msg (:nrepl-msg msg)]
        (log/info "Sending message to nrepl server:" nrepl-msg)
        (doseq [nrepl-res-msg (nrepl/message @(:nrepl-session @state) nrepl-msg)]
          (log/info "Received message from nrepl server:" nrepl-res-msg)
          (when (:ns nrepl-res-msg)
            (alter-var-root #'*ns* (constantly (create-ns (symbol (:ns nrepl-res-msg))))))
          (log/info "Replying to client:" nrepl-res-msg)
          (http/send! (:ws-channel msg) (json/write-str nrepl-res-msg)))
        (recur)))
    (log/infof "Started websocket server at ws://localhost:%s" port)
    {:server server
     :port   port}))

(defn stop
  "Stop the WebSocket server"
  [server]
  (log/info "Stopping WebSocket server")
  ((:server server)))