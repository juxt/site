;; Copyright © 2020-2021, JUXT LTD.

(ns juxt.spin.alpha.representation
  (:require
   [clojure.tools.logging :as log]
   [juxt.pick.alpha.core :refer [rate-representation]]
   [juxt.pick.alpha.ring :refer [decode-maybe]]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.reap.alpha.decoders :as reap]
   [juxt.reap.alpha.ring :refer [headers->decoded-preferences]]
   [juxt.spin.alpha :as spin]))

(alias 'http (create-ns 'juxt.http.alpha))

(defn receive-representation
  "Check and load the representation enclosed in the request message payload."
  [request resource date]

  (let [content-length
        (try
          (some->
           (get-in request [:headers "content-length"])
           (Long/parseLong))
          (catch NumberFormatException e
            (throw
             (ex-info
              "Bad content length"
              {::spin/response
               {:status 400
                :body "Bad Request\r\n"}}
              e))))]

    (log/debug "content-length received is" content-length)

    (when (nil? content-length)
      (throw
       (ex-info
        "No Content-Length header found"
        {::spin/response
         {:status 411
          :body "Length Required\r\n"}})))

    ;; Spin protects resources from PUTs that are too large. If you need to
    ;; exceed this limitation, explicitly declare ::spin/max-content-length in
    ;; your resource.
    (when-let [max-content-length (get resource ::http/max-content-length (Math/pow 2 16))]
      (when (> content-length max-content-length)
        (throw
         (ex-info
          "Payload too large"
          {::spin/response
           {:status 413
            :body "Payload Too Large\r\n"}}))))

    (when-not (:body request)
      (throw
       (ex-info
        "No body in request"
        {::spin/response
         {:status 400
          :body "Bad Request\r\n"}})))

    (let [decoded-representation
          (decode-maybe

           ;; See Section 3.1.1.5, RFC 7231 as to why content-type defaults
           ;; to application/octet-stream
           (cond-> {::http/content-type "application/octet-stream"}
             (contains? (:headers request) "content-type")
             (assoc ::http/content-type (get-in request [:headers "content-type"]))

             (contains? (:headers request) "content-encoding")
             (assoc ::http/content-encoding (get-in request [:headers "content-encoding"]))

             (contains? (:headers request) "content-language")
             (assoc ::http/content-language (get-in request [:headers "content-language"]))))]

      (when-let [acceptable (::http/acceptable resource)]

        (let [prefs (headers->decoded-preferences acceptable)
              request-rep (rate-representation prefs decoded-representation)]

          (when (or (get prefs "accept") (get prefs "accept-charset"))
            (cond
              (= (:juxt.pick.alpha/content-type-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-type of the request payload is not supported by the resource"
                {::request request
                 ::resource resource
                 ::acceptable acceptable
                 ::content-type (get request-rep "content-type")
                 ::spin/response
                 {:status 415
                  :body "Unsupported Media Type\r\n"}}))

              (and
               (= "text" (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/type]))
               (get prefs "accept-charset")
               (not (contains? (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map]) "charset")))
              (throw
               (ex-info
                "The Content-Type header in the request is a text type and is required to specify its charset as a media-type parameter"
                {::request request
                 ::resource resource
                 ::acceptable acceptable
                 ::content-type (get request-rep "content-type")
                 ::spin/response
                 {:status 415
                  :body "Unsupported Media Type\r\n"}}))

              (= (:juxt.pick.alpha/charset-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The charset of the Content-Type header in the request is not supported by the resource"
                {::request request
                 ::resource resource
                 ::acceptable acceptable
                 ::content-type (get request-rep "content-type")
                 ::spin/response
                 {:status 415
                  :body "Unsupported Media Type\r\n"}}))))

          (when (get prefs "accept-encoding")
            (cond
              (= (:juxt.pick.alpha/content-encoding-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-encoding in the request is not supported by the resource"
                {::request request
                 ::resource resource
                 ::acceptable acceptable
                 ::content-language (get-in request [:headers "content-encoding"] "identity")
                 ::spin/response
                 {:status 409
                  :body "Conflict\r\n"}}))))

          (when (get prefs "accept-language")
            (cond
              (not (contains? (:headers request) "content-language"))
              (throw
               (ex-info
                "Request must contain Content-Language header"
                {::spin/response
                 {:status 409
                  :body "Conflict\r\n"}}))

              (= (:juxt.pick.alpha/content-language-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-language in the request is not supported by the resource"
                {::request request
                 ::resource resource
                 ::acceptable acceptable
                 ::content-language (get-in request [:headers "content-language"])
                 ::spin/response
                 {:status 415
                  :body "Unsupported Media Type\r\n"}}))))))

      (when (get-in request [:headers "content-range"])
        (throw
         (ex-info
          "Content-Range header not allowed on a PUT request"
          {::spin/response
           {:status 400
            :body "Bad Request\r\n"}})))

      (with-open [in (:body request)]
        (let [bytes (.readNBytes in content-length)
              content-type (:juxt.reap.alpha.rfc7231/content-type decoded-representation)]

          (cond
            (= (:juxt.reap.alpha.rfc7231/type content-type) "text")
            (let [charset (get-in decoded-representation [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map "charset"])]
              (merge decoded-representation
                     {::http/last-modified (format-http-date date)
                      ::http/charset charset
                      ::http/content (new String bytes (or charset "utf-8"))}))
            :else
            (merge decoded-representation
                   {::http/last-modified (format-http-date date)
                    ::http/body bytes})))))))
