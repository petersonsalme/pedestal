(ns io.pedestal.http.websocket-test
  (:require
    [clojure.test :refer :all]
    [hato.websocket :as ws]
    [io.pedestal.http :as http]
    [clojure.core.async :refer [chan put!] :as async]
    [io.pedestal.http.jetty.websockets :as websockets]))

(def server-status-chan nil)

(defn server-status-chan-fixture [f]
  (with-redefs [server-status-chan (chan 10)]
    (f)))

(defn <status!!
  []
  (async/alt!!
    server-status-chan ([status-value] status-value)

    (async/timeout 3000) [::timed-out]))

(defn simple-server-fixture [f]
  (let [ws-map {"/ws" {:on-connect #(put! server-status-chan [:connect %])
                       :on-close (fn [status-code reason]
                                   (put! server-status-chan [:close status-code reason]))
                       :on-error #(put! server-status-chan [:error %])
                       :on-text #(put! server-status-chan [:text %])
                       :on-binary (fn [payload offset length]
                                    (put! server-status-chan [:binary payload offset length]))}}
        server (http/create-server {::http/type :jetty
                                    ::http/join? false
                                    ::http/port 8080
                                    ::http/routes []
                                    ::http/container-options
                                    {:context-configurator #(websockets/add-ws-endpoints % ws-map)}})]
    (try
      (http/start server)
      (f)
      (finally
        (http/stop server)))))

(use-fixtures :each
              server-status-chan-fixture
              simple-server-fixture)

;; TODO: test text round-trip
(deftest client-sends-text-test
  (let [session @(ws/websocket "ws://localhost:8080/ws" {})]
    (is (= :connect
           (first (<status!!))))
    (ws/send! session "hello")

    (is (= [:text "hello"]
           (<status!!)))

    ;; Note: the status code value is tricky, must be one of a few preset values, or in the
    ;; range 3000 to 4999.
    @(ws/close! session 4000 "A valid reason")

    (is (=[:close 4000 "A valid reason"]
           (<status!!)))))

;; TODO: test binary round-trip
;; TODO: test when server closes, client sees it
;; TODO: test when client closes, server sees it

