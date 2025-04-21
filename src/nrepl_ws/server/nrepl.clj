(ns nrepl-ws.server.nrepl
  (:require
   [clojure.tools.logging :as log]
   [nrepl.server :as server]))

(defn start
  "Start an nREPL server on the specified port"
  [port]
  (let [server (server/start-server :port port)]
    (log/infof "Started nREPL server on port %s" port)
    {:server server
     :port port}))

(defn stop
  "Stop the nREPL server"
  [server]
  (log/info "Stopping nREPL server")
  (server/stop-server (:server server)))