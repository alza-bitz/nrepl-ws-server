(ns nrepl-ws.client
  (:require
   [clojure.core.async :as async]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [hato.websocket :as ws]
   [nrepl-ws.transducers :refer [join-when partition-when]]))

(comment
  (json/read-str "{\"id\":\"f3e"))

(defn create-client [uri]
  (let [msg-ch (async/chan 10
                           (comp
                            (join-when #(true? (:last? %)) #(:msg %))
                            (map #(json/read-str % :key-fn keyword))
                            (partition-when #(= ["done"] (:status %))))
                           #(log/error % "problem processing received messages"))
        client (ws/websocket uri
                             {:on-open (fn [ws]
                                         (log/info "websocket opened!"))
                              :on-message (fn [ws buf last?]
                                            (let [msg (str buf)]
                                              (log/info "received message:" msg "last?" last?)
                                              (try
                                                (async/put! msg-ch {:msg msg :last? last?})
                                                (log/info "message put in channel:" msg)
                                                (catch Exception e
                                                  (log/error "error parsing message:" e)))))
                              :on-close (fn [ws status reason]
                                          (log/info "websocket closed!")
                                          (async/close! msg-ch))
                              :on-connect (fn [ws]
                                            (log/info "websocked connected to:" uri))
                              :on-error (fn [ws err]
                                          (log/error "error:" err))})]
    {:socket @client
     :msg-ch msg-ch}))

(defn send! [{:keys [socket]} msg]
  (log/info "sending message:" msg)
  (ws/send! socket (json/write-str msg))
  (log/info "message sent!"))

(defn close! [{:keys [socket msg-ch]}]
  (log/info "closing down!")
  (ws/close! socket)
  (async/close! msg-ch))