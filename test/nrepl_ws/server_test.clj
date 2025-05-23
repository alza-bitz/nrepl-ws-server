(ns nrepl-ws.server-test
  (:require
   [clojure.core.async :refer [alts!! timeout]]
   [clojure.test :refer :all]
   [nrepl-ws.client :as client]
   [nrepl-ws.server.core :as ws]
   [nrepl-ws.server.nrepl :as nrepl]))

;; (def ^:dynamic *server* nil)
(def ^:dynamic *client* nil)

(defn server-fixture [f]
  (let [nrepl-server (nrepl/start 1235)
        ws-server (ws/start 1234 nrepl-server)]
    (try
      (f)
      (finally
        (ws/stop ws-server)
        (nrepl/stop nrepl-server)))))

(defn client-fixture [f]
  (let [client (client/create-client "ws://localhost:1234")]
    (try
      (binding [*client* client]
        (f))
      (finally
        (client/close! client)))))

(use-fixtures :once server-fixture)
(use-fixtures :each client-fixture)

(deftest basic-evaluation-test
  (testing "basic evaluation"
    (client/send! *client* {:op "eval"
                            :code "(+ 2 3)"
                            :id "1"})
    (let [msg-ch (:msg-ch *client*)
          [msgs ch] (alts!! [msg-ch (timeout 1000)])]
      (if (not (identical? ch msg-ch))
        (throw (ex-info "timed out" {:ch ch}))
        (do
          (is (= 2 (count msgs)))
          (let [msg1 (first msgs)
                msg2 (second msgs)]
            (is (= "1" (:id msg1)))
            (is (= "5" (:value msg1)))
            (is (= ["done"] (:status msg2)))))))))

(deftest error-handling-test
  (testing "error handling"
    (client/send! *client* {:op "eval"
                            :code "(/ 1 0)"
                            :id "2"})
    (let [msg-ch (:msg-ch *client*)
          [msgs ch] (alts!! [msg-ch (timeout 1000)])]
      (if (not (identical? ch msg-ch))
        (throw (ex-info "timed out" {:ch ch}))
        (do
          (is (= 3 (count msgs)))
          (let [msg1 (first msgs)
                msg2 (second msgs)]
            (is (= "2" (:id msg1)))
            (is (contains? msg1 :err))
            (is (= "2" (:id msg2)))
            (is (= ["eval-error"] (:status msg2)))
            (is (= ["done"] (:status (get msgs 2))))))))))

(deftest session-management-test
  (testing "session management"
    (client/send! *client* {:op "clone"
                            :id "3"})
    (let [msg-ch (:msg-ch *client*)
          [msgs ch] (alts!! [msg-ch (timeout 1000)])]
      (if (not (identical? ch msg-ch))
        (throw (ex-info "timed out" {:ch ch}))
        (do
          (is (= 1 (count msgs)))
          (let [msg1 (first msgs)]
            (is (= "3" (:id msg1)))
            (is (contains? msg1 :new-session))
            (is (= ["done"] (:status msg1)))))))))

(deftest namespace-test
  (testing "namespace create"
    (client/send! *client* {:op "eval"
                            :code "(ns my-new-ns)"
                            :id "4"})
    (let [msg-ch (:msg-ch *client*)
          [msgs ch] (alts!! [msg-ch (timeout 1000)])]
      (if (not (identical? ch msg-ch))
        (throw (ex-info "timed out" {:ch ch}))
        (do
          (is (= 2 (count msgs)))
          (let [msg1 (first msgs)
                msg2 (second msgs)]
            (is (= "4" (:id msg1)))
            (is (= "my-new-ns" (:ns msg1)))
            (is (= "4" (:id msg2)))
            (is (= ["done"] (:status msg2))))))))

  (testing "namespace persist"
    (client/send! *client* {:op "eval"
                            :code "*ns*"
                            :id "5"})
    (let [msg-ch (:msg-ch *client*)
          [msgs ch] (alts!! [msg-ch (timeout 1000)])]
      (if (not (identical? ch msg-ch))
        (throw (ex-info "timed out" {:ch ch}))
        (do
          (is (= 2 (count msgs)))
          (let [msg1 (first msgs)
                msg2 (second msgs)]
            (is (= "5" (:id msg1)))
            (is (= "my-new-ns" (:ns msg1)))
            (is (= "5" (:id msg2)))
            (is (= ["done"] (:status msg2)))))))))

(deftest repeated-evaluation-test
  (doseq [counter (range 100)]
    (let [_ (client/send! *client* {:op "eval"
                                    :code (str counter)})
          msg-ch (:msg-ch *client*)
          [msgs ch] (alts!! [msg-ch (timeout 1000)])]
      (if (not (identical? ch msg-ch))
        (throw (ex-info "timed out" {:ch ch
                                     :code (str counter)}))
        (is (= 2 (count msgs)))))))

(deftest repeated-evaluation-test-with-new-client-per-batch
  (doseq [batch (partition 10 (range 100))]
    (let [client (client/create-client "ws://localhost:1234")]
      (try
        (doseq [counter batch]
          (let [_ (client/send! client {:op "eval"
                                        :code (str counter)})
                msg-ch (:msg-ch client)
                [msgs ch] (alts!! [msg-ch (timeout 1000)])]
            (if (not (identical? ch msg-ch))
              (throw (ex-info "timed out" {:ch ch
                                           :code (str counter)}))
              (is (= 2 (count msgs))))))
        (finally
          (client/close! client))))))

