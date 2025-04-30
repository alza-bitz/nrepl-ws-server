(ns nrepl-ws.server.core
  (:require
   [clojure.core.async :refer [<! >! chan go go-loop]]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [nrepl.core :as nrepl]
   [org.httpkit.server :as http]))

;; TODO why httpkit? why not ring+jetty? why not aleph+manifold?
;; further, is there a core async (non-blocking) http server for clojure?
;; https://github.com/ring-clojure/ring-websocket-async

;; TODO single user mode! that is fine if we have a docker container for each user
;; TODO otherwise, consider a pool of nrepl server backends (they will all have the same classpath)
(def single-user (atom {:nrepl-session (promise)
                        :nrepl-channel (chan)
                        :nrepl-reply-channel (chan)}))

(defn- on-open-handler [transport]
  (fn [ch]
    (log/info "Websocket opened!" (pr-str ch))
    (let [nrepl-client (nrepl/client transport 5000)
          session-id (nrepl/new-session nrepl-client)
          nrepl-session (nrepl/client-session nrepl-client :session session-id)]
      (log/info "nREPL session created with id:" session-id)
      (swap! single-user assoc :nrepl-session-id session-id)
      (deliver (:nrepl-session @single-user) nrepl-session))))

(defn- on-receive [ch msg]
  (log/info "Websocket received message:" msg)
  ;; TODO assumptions:
  ;; any sequence of messages on a given opened websocket will use the same channel
  ;; any concurrent message scenarios (e.g. eval + interrupt) on a given opened websocket will use the same channel
  (swap! single-user assoc :ws-channel ch)
  ;; TODO we probably want to set an id on the message if not already set 
  (go
    (>! (:nrepl-channel @single-user) (json/read-str msg :key-fn keyword))))

(defn- on-close [_ status-code]
  (log/info "Websocket closed with status code" status-code)
  (swap! single-user assoc 
         :nrepl-session (promise)
         :nrepl-session-id nil
         :ws-channel nil))

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
      (let [msg (<! (:nrepl-channel @single-user))]
        (log/info "Sending message to nrepl server:" msg)
        (doseq [res-msg (nrepl/message @(:nrepl-session @single-user) msg)]
          (log/info "Received message from nrepl server:" res-msg)
          (when (:ns res-msg)
            (alter-var-root #'*ns* (constantly (create-ns (symbol (:ns res-msg))))))
          (>! (:nrepl-reply-channel @single-user) res-msg))
        (recur)))
    (go-loop []
      (let [res-msg (<! (:nrepl-reply-channel @single-user))]
        (log/info "Websocket sending reply:" res-msg)
        (http/send! (:ws-channel @single-user) (json/write-str res-msg))
        (recur)))
    (log/infof "Started websocket server at ws://localhost:%s" port)
    {:server server
     :port   port}))

(defn stop
  "Stop the WebSocket server"
  [server]
  (log/info "Stopping WebSocket server")
  ((:server server)))