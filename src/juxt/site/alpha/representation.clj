;; Copyright © 2020-2021, JUXT LTD.

(ns juxt.site.alpha.representation
  (:require
   [clojure.tools.logging :as log]
   [juxt.pick.alpha.core :refer [rate-representation]]
   [juxt.pick.alpha.ring :refer [decode-maybe]]
   [juxt.reap.alpha.encoders :refer [format-http-date]]
   [juxt.reap.alpha.ring :refer [headers->decoded-preferences]]))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(defn receive-representation
  "Check and load the representation enclosed in the request message payload."
  [{::site/keys [resource start-date] :as req}]

  (let [content-length
        (try
          (some->
           (get-in req [:ring.request/headers "content-length"])
           (Long/parseLong))
          (catch NumberFormatException e
            (throw
             (ex-info
              "Bad content length"
              (into req {:ring.response/status 400
                         :ring.response/body "Bad Request\r\n"})
              e))))]

    (log/tracef "content-length received for %s is %s" (::site/uri req) content-length)

    (when (nil? content-length)
      (throw
       (ex-info
        "No Content-Length header found"
        (into req {:ring.response/status 411
                   :ring.response/body "Length Required\r\n"}))))

    ;; Spin protects resources from PUTs that are too large. If you need to
    ;; exceed this limitation, explicitly declare ::spin/max-content-length in
    ;; your resource.
    (when-let [max-content-length (get resource ::http/max-content-length (Math/pow 2 16))]
      (when (> content-length max-content-length)
        (throw
         (ex-info
          "Payload too large"
          (into req
                {:ring.response/status 413
                 :ring.response/body "Payload Too Large\r\n"})))))

    (when-not (:ring.request/body req)
      (throw
       (ex-info
        "No body in request"
        (into req {:ring.response/status 400
                   :ring.response/body "Bad Request\r\n"}))))

    (let [decoded-representation
          (decode-maybe

           ;; See Section 3.1.1.5, RFC 7231 as to why content-type defaults
           ;; to application/octet-stream
           (cond-> {::http/content-type "application/octet-stream"}
             (contains? (:ring.request/headers req) "content-type")
             (assoc ::http/content-type (get-in req [:ring.request/headers "content-type"]))

             (contains? (:ring.request/headers req) "content-encoding")
             (assoc ::http/content-encoding (get-in req [:ring.request/headers "content-encoding"]))

             (contains? (:ring.request/headers req) "content-language")
             (assoc ::http/content-language (get-in req [:ring.request/headers "content-language"]))))]

      (when-let [acceptable (::http/acceptable resource)]

        (let [prefs (headers->decoded-preferences acceptable)
              request-rep (rate-representation prefs decoded-representation)]

          (when (or (get prefs "accept") (get prefs "accept-charset"))
            (cond
              (= (:juxt.pick.alpha/content-type-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-type of the request payload is not supported by the resource"
                (into req
                      {:ring.response/status 415
                       :ring.response/body "Unsupported Media Type\r\n"
                       ::acceptable acceptable
                       ::content-type (get request-rep "content-type")})))

              (and
               (= "text" (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/type]))
               (get prefs "accept-charset")
               (not (contains? (get-in request-rep [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map]) "charset")))
              (throw
               (ex-info
                "The Content-Type header in the request is a text type and is required to specify its charset as a media-type parameter"
                (into req {:ring.response/status 415
                           :ring.response/body "Unsupported Media Type\r\n"
                           ::acceptable acceptable
                           ::content-type (get request-rep "content-type")})))

              (= (:juxt.pick.alpha/charset-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The charset of the Content-Type header in the request is not supported by the resource"
                (into req {:ring.response/status 415
                           :ring.response/body "Unsupported Media Type\r\n"
                           ::acceptable acceptable
                           ::content-type (get request-rep "content-type")})))))

          (when (get prefs "accept-encoding")
            (cond
              (= (:juxt.pick.alpha/content-encoding-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-encoding in the request is not supported by the resource"
                (into req {:ring.response/status 409
                           :ring.response/body "Conflict\r\n"
                           ::acceptable acceptable
                           ::content-encoding (get-in req [:ring.request/headers "content-encoding"] "identity")})))))

          (when (get prefs "accept-language")
            (cond
              (not (contains? (:ring.response/headers req) "content-language"))
              (throw
               (ex-info
                "Request must contain Content-Language header"
                (into req {:ring.response/status 409
                           :ring.response/body "Conflict\r\n"
                           ::acceptable acceptable
                           ::content-language (get-in req [:ring.request/headers "content-language"])})))

              (= (:juxt.pick.alpha/content-language-qvalue request-rep) 0.0)
              (throw
               (ex-info
                "The content-language in the request is not supported by the resource"
                (into req {:ring.response/status 415
                           :ring.response/body "Unsupported Media Type\r\n"
                           ::acceptable acceptable
                           ::content-language (get-in req [:ring.request/headers "content-language"])})))))))

      (when (get-in req [:ring.request/headers "content-range"])
        (throw
         (ex-info
          "Content-Range header not allowed on a PUT request"
          (into req
                {:ring.response/status 400
                 :ring.response/body "Bad Request\r\n"}))))

      (with-open [in (:ring.request/body req)]
        (let [body (.readNBytes in content-length)
              content-type (:juxt.reap.alpha.rfc7231/content-type decoded-representation)]

          (merge
           decoded-representation
           {::http/content-length content-length
            ::http/last-modified start-date}

           (if (and
                (= (:juxt.reap.alpha.rfc7231/type content-type) "text")
                (nil? (get decoded-representation ::http/content-encoding)))
             (let [charset
                   (get-in decoded-representation
                           [:juxt.reap.alpha.rfc7231/content-type :juxt.reap.alpha.rfc7231/parameter-map "charset"])]
               (merge
                {::http/content (new String body (or charset "utf-8"))}
                (when charset {::http/charset charset})))

             {::http/body body})))))))
