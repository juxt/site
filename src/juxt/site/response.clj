;; Copyright © 2021, JUXT LTD.

(ns juxt.site.response)

(defn add-payload [{:juxt.site/keys [resource]
                    :ring.request/keys [method]
                    :as req}]
  ;; Should not be called if method is HEAD
  (assert (not= method :head))

  (let [{:juxt.http/keys [body content]} resource]
    (cond
      ;; TODO: Fish out the charset from the content-type of the resource and
      ;; use when converting to bytes.

      ;; Note: Although :ring.response/body supports anything that satisfies
      ;; ring.core.protocols.StreamableResponseBody, Ring will extract the
      ;; charset of a String by looking at the Content-Type header for the
      ;; response and using regex. Given we already know the charset, we should
      ;; avoid triggering this functionality in Ring for performance reasons.
      content (assoc req :ring.response/body (.getBytes content))

      body (assoc req :ring.response/body body)
      :else req)))

(defn add-error-payload
  [{:juxt.site/keys [resource]
    :ring.request/keys [method]
    :as req}]
  ;; Should not be called if method is HEAD
  (assert (not= method :head))

  (let [{:juxt.http/keys [body content]} resource]
    (cond
      content (assoc req :ring.response/body content)
      body (assoc req :ring.response/body body)
      :else req)))

(defn redirect [req status location]
  (-> req
      (assoc :ring.response/status status)
      (update :ring.response/headers assoc "location" location)))
