(ns io.pedestal.interceptor-chain-benchmark
  "Basic benchmarks for the executing the interceptor chain."
  (:require [criterium.core :as c]
            [net.lewisship.trace :refer [trace]]
            [cheshire.core :as cheshire]
            [io.pedestal.http :as service]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]))

;; Going to create a simple, default application with two cases:
;; 1. A simple POST request to a handler that succeeds
;; 2. A simple request to a handler that throws an exception

(defn echo-handler [request]
  #_(trace :request request)
  {:status 200
   :headers {"content-type" "application/json"}
   :body (-> request :json-params cheshire/generate-string)})

(def eat-exception
  (interceptor
    {:name ::eat-exception
     :error (fn [context _]
              (assoc context :response {:status 500
                                        :body (str "exception in "
                                                   (get-in context [:request :uri]))}))}))

(defn fail-handler [_]
  (throw (Exception. "Failure inside fail-handler.")))

(def ^:private routes
  #{["/echo" :post [(body-params) echo-handler] :route-name ::echo]
    ["/fail" :get [eat-exception fail-handler] :route-name ::fail]})

(def service-fn
  (-> {::http/routes routes}
      service/default-interceptors
      service/service-fn
      ::service/service-fn))

(defn json-response-for
  [url body-data]
  (response-for service-fn :post url
                :headers {"Content-Type" "application/json"}
                :body (cheshire/generate-string body-data)))

(comment
  (c/with-progress-reporting
    (c/bench
      (do (json-response-for "/echo" {:foo 1 :bar 2})
          nil)))

  (c/with-progress-reporting
    (c/bench
      (do (response-for service-fn :get "/fail")
          nil)))
  )

;; Results

;; Baseline:
;; /echo    94.08 µs
;; /fail    149.7 µs

;; Simple opts (only push to stack if :leave or :error)

;; /echo  89.3 µs
;; /fail  141.8 µs

;; After interceptor.chain rewrite:

;; /echo 53.8 µs
;; /fail 90.3 µs