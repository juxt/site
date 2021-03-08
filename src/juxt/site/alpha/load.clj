;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.load

  (:import
   [java.net URI]
   [java.net.http HttpClient HttpClient$Version HttpRequest HttpResponse$BodyHandlers HttpRequest$BodyPublishers HttpResponse$BodySubscribers]))

(defn new-client
  ([]
   (new-client {}))
  ([{:keys [connect-timeout]
     :or {connect-timeout (java.time.Duration/ofSeconds 20)}
     :as opts}]
   (.. (HttpClient/newBuilder)
       (version HttpClient$Version/HTTP_1_1)
       (connectTimeout connect-timeout)
       (build))))

(defn new-request
  ([method url]
   (new-request method url {}))
  ([method url {:keys [request-body-publisher headers] :as opts}]
   (.build
    (cond-> (HttpRequest/newBuilder)
      (= method :get) (.GET)
      (= method :delete) (.DELETE)
      headers ((fn [builder]
                 (doseq [[k v] headers]
                   (.setHeader builder k v))
                 builder))
      request-body-publisher (.method (.toUpperCase (name method)) request-body-publisher)
      true (.uri (URI/create url))))))

(defn request
  ([client method url]
   (request client method url {}))
  ([client
    method
    url
    {:keys [response-body-handler async on-success on-error]
     :or {response-body-handler (HttpResponse$BodyHandlers/ofString)}
     :as opts}]
   (let [request (new-request method url opts)]
     (if async
       (cond->
           (.sendAsync client request response-body-handler)
           on-error (. (exceptionally
                        (reify java.util.function.Function
                          (apply [_ error]
                            (println "Error:" error)
                            (on-error error)))))
           on-success (. (thenAccept
                          (reify java.util.function.Consumer
                            (accept [_ result]
                              (on-success result))))))
       (try
         (let [result (.send client request response-body-handler)]
           (if on-success (on-success result) result))
         (catch Exception e
           (if on-error (on-error e) (throw e))))))))

(defn get-zip-enties [zis]
  (when (pos? (.available zis))
    (when-let [ze (.getNextEntry zis)]
      (lazy-seq (cons ze (get-zip-enties zis))))))

#_(doseq [ze (->>
                  (get-zip-enties zis)
                  (filter #(.startsWith (.getName %) "swagger-ui-3.44.1/dist/")))])


(defn zip-entry [zis pred]
  (println "zip-entry")
  (when (.available zis)
    (when-let [ze (.getNextEntry zis)]
      (try
        (when (pred ze)
          (println "ZipEntry:" (.getName ze))
          (let [sz (.getSize ze)]
            (when (pos? sz)
              (let [ba (byte-array sz)]
                (loop [offset 0]
                  (let [rd (.read zis ba offset (- sz offset))]
                    (println "Read" (+ offset rd) "of" sz "of" (.getName ze))
                    (when (pos? rd) (recur (+ offset rd)))))))))
        (println [(.getName ze) (.getSize ze) ])
        ze
        (finally (.closeEntry zis))))))


#_(let [_ (println "START")
      client (new-client)
      body-handler (HttpResponse$BodyHandlers/ofInputStream)
      response (request client
                        :get
                        "https://codeload.github.com/swagger-api/swagger-ui/zip/v3.44.1"
                        {:response-body-handler body-handler})
      headers (into {} (.. response headers map))
      ;;      content-length (Long/parseLong (first (get headers "content-length")))
      content-type (first (get headers "content-type"))]

  (println "content-type" content-type)

  (case content-type
    "application/zip"
    (let [zis (java.util.zip.ZipInputStream. (.body response))]
      (loop []
        (when-let [ze (zip-entry
                       zis
                       (fn [ze] (.startsWith (.getName ze)
                                             "swagger-ui-3.44.1/dist/")))]
          (recur)))))


  ;;headers
  ;;(Long/parseLong (first (get headers "content-length")))
  ;;(type (.body response))
  ;;(get-in headers ["content-length" 0])


  ;;(.headers response)

  ;; file:///usr/share/doc/java11-openjdk/api/java.net.http/java/net/http/HttpResponse.BodySubscribers.html#ofInputStream()
  )
