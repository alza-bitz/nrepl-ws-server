(ns nrepl-ws.main
  (:require [nrepl-ws.server :as server]))

(defn -main
  [& args]
  (let [server (server/start-server :port 7888)] 
    (clojure.pprint/pprint server)))