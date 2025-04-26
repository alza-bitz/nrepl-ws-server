(ns nrepl-ws.server.main
  (:require
   [clojure.pprint :as pprint]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [nrepl-ws.server.core :as ws]
   [nrepl-ws.server.nrepl :as nrepl]
   [scicloj.clay.v2.api :as clay]))

(def system-config
  {:server/ws {:port 7888
               :nrepl (ig/ref :server/nrepl)}
   :server/nrepl {:port 7889}
   :server/clay {:port 7890}})

(defmethod ig/init-key :server/nrepl [_ {:keys [port]}]
  (nrepl/start port))

(defmethod ig/halt-key! :server/nrepl [_ server]
  (nrepl/stop server))

(defmethod ig/init-key :server/ws [_ {:keys [port nrepl]}]
  (ws/start port nrepl))

(defmethod ig/halt-key! :server/ws [_ server]
  (ws/stop server))

(defmethod ig/init-key :server/clay [_ {:keys [port]}]
  (clay/start! {:browse false :port port})
  {:port port})

(defmethod ig/halt-key! :server/clay [_ _]
  (log/info "Stopping Clay server")
  (clay/stop!))

(defn -main
  [& args]
  (let [system (ig/init system-config)
        ports {:ws-port (get-in system [:server/ws :port])
               :nrepl-port (get-in system [:server/nrepl :port])
               :clay-port (get-in system [:server/clay :port])}]

    (log/infof "All started %s" ports)
    (log/info "Press Ctrl+C to stop.")

    ;; Add shutdown hook
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (log/info "Shutting down..")
                                 (ig/halt! system)
                                 (log/info "All stopped."))))))