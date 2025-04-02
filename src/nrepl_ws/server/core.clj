(ns nrepl-ws.server.core
  (:require
   [clojure.core.async :refer [<! >! chan close! go]]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [nrepl.core :as nrepl]
   [nrepl.server :as server] ;; [lambdaisland.nrepl-proxy :as proxy]
   [org.httpkit.server :as http]))

;; TODO why httpkit? why not ring+jetty? why not aleph+manifold?
;; further, is there a core async (non-blocking) http server for clojure?
;; https://github.com/ring-clojure/ring-websocket-async

;; TODO single user mode! that is fine if we have a docker container for each user
;; TODO otherwise, consider a pool of nrepl server backends (they will all have the same classpath)
(def state (atom {:nrepl-session (promise)
                  :nrepl-channel (chan)}))

(defn- on-open-handler [transport]
  (fn [ch]
    (log/info "websocket opened!" (pr-str ch))
    (let [nrepl-client (nrepl/client transport 5000)
          session-id (nrepl/new-session nrepl-client)
          nrepl-session (nrepl/client-session nrepl-client :session session-id)]
      (log/info "nrepl session created with id:" session-id)
      (swap! state assoc :session-id session-id)
      (deliver (:nrepl-session @state) nrepl-session))))

(defn- on-receive [ch msg]
  (log/info "received message:" msg)
  ;; TODO we probably want to set an id on the message if not already set 
  (go
    (>! (:nrepl-channel @state) (-> (json/read-str msg :key-fn keyword)
                                    (assoc :ws-channel ch)))))

(defn- on-close [ch status-code]
  (log/info "websocket closed with status code" status-code)
  (swap! state assoc :nrepl-session (promise) :nrepl-session-id nil))

(defn ws-handler [transport]
  (fn [request]
    (if-not (:websocket? request)
      {:status 400
       :body "websocket connection required"}
      (http/as-channel request
                       {:on-open (on-open-handler transport)
                        :on-receive on-receive
                        :on-close on-close}))))

(defn start-server [& {:keys [port]
                       :or {port 7888}}]
  (let [server (server/start-server :port (inc port))
        transport (nrepl/connect :port (inc port))
        ws-server (http/run-server (ws-handler transport) {:port port})]
    (go
      ;; (binding [*ns* *ns*]) 
      (loop []
        (let [msg (<! (:nrepl-channel @state))
              ch (:ws-channel msg)]
          (log/info "sending message to nrepl server:" (dissoc msg :ws-channel))
          (doseq [res (nrepl/message @(:nrepl-session @state) (dissoc msg :ws-channel))]
            (log/info "received response from nrepl server:" res)
            (when (:ns res)
              ;; TODO this is a hack, it wouldn't work if we had per-session nrepl server backends
              (alter-var-root #'*ns* (constantly (create-ns (symbol (:ns res)))))
              ;; (set! *ns* (create-ns (symbol (:ns res))))
              )
            (log/info "replying to client:" res)
            ;; TODO ch might be nil here, we should handle that (or use a reply channel)
            (http/send! ch (json/write-str res)))
          (recur))))
    {:nrepl-server server
     :ws-server ws-server
     :port port}))

(defn stop-server [{:keys [nrepl-server ws-server]}]
  (log/info "Server stopping!")
  (server/stop-server nrepl-server)
  (ws-server)
  (log/info "Server stopped!"))