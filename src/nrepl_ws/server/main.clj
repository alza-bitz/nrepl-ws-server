(ns nrepl-ws.server.main
  (:require [nrepl-ws.server.core :as server]))

(defn -main
  [& args]
  (let [server (server/start-server :port 7888)] 
    (clojure.pprint/pprint server)))