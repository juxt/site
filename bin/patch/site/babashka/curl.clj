(ns site.babashka.curl
  (:refer-clojure :exclude [get read-line])
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net URLEncoder]
           [java.net URI]
           [java.io File SequenceInputStream ByteArrayInputStream]))

(set! *warn-on-reflection* true)

(def windows? (-> (System/getProperty "os.name")
                  (str/lower-case)
                  (str/includes? "win")))

(def ^:dynamic *defaults*
  {:escape (if windows? #(str/replace % "\"" "\\\"") identity)
   :compressed true
   :throw true})

;;;; Utils

(defn- shell-command
  [args opts]
  (let [args (if windows?
               (mapv (:escape opts) args)
               args)
        pb (ProcessBuilder. ^java.util.List args)
        proc (.start pb)
        _ (when-let [is (:in-stream opts)]
            (with-open [stdin (.getOutputStream proc)]
              (io/copy is stdin)))
        out (.getInputStream proc)
        err (.getErrorStream proc)]
    {:out out
     :err err
     :proc proc}))

(defn- exec-curl [args opts]
  (let [res (shell-command args opts)
        out (:out res)
        err (:err res)
        proc (:proc res)]
    (assoc opts :out out :err err :proc proc)))

(defn- file? [f]
  (when (instance? File f)
    (let [f ^File f]
      (and (.exists f)
           (.isFile f)))))

(defn- input-stream? [x]
  (instance? java.io.InputStream x))

(defn- accept-header [opts]
  (when-let [accept (:accept opts)]
    ["-H" (str "Accept: " (case accept
                            :json "application/json"
                            accept))]))

(defn- url-encode
  "Returns an UTF-8 URL encoded version of the given string."
  [^String unencoded]
  (URLEncoder/encode unencoded "UTF-8"))

(defn- curl-command [opts]
  (let [body (:body opts)
        opts (if body
               (cond-> opts
                 (string? body) (assoc :data-raw body)
                 (file? body) (assoc :in-file body)
                 (input-stream? body) (assoc :in-stream body))
               opts)
        method (when-let [method (:method opts)]
                 (case method
                   :head ["--head"]
                   ["--request" (-> method name str/upper-case)]))
        headers (into [] (mapcat (fn [[k v]] ["-H" (str (name k) ": " v)])) (:headers opts))
        accept-header (accept-header opts)
        form-params (when-let [form-params (:form-params opts)]
                      (loop [params* (transient [])
                             kvs (seq form-params)]
                        (if kvs
                          (let [[k v] (first kvs)
                                v (url-encode v)
                                param ["--data" (str (url-encode k) "=" v)]]
                            (recur (reduce conj! params* param) (next kvs)))
                          (persistent! params*))))
        query-params (when-let [qp (:query-params opts)]
                       (loop [params* (transient [])
                              kvs (seq qp)]
                         (if kvs
                           (let [[k v] (first kvs)]
                             (recur (conj! params* (str (url-encode k) "=" (url-encode v))) (next kvs)))
                           (str/join "&" (persistent! params*)))))
        data-raw (:data-raw opts)
        data-raw (when data-raw
                   ["--data-binary" data-raw])
        url (let [url* (:url opts)]
              (cond
                (string? url*)
                url*

                (map? url*)
                (str (URI. ^String (:scheme url*)
                           ^String (:user url*)
                           ^String (:host url*)
                           ^Integer (:port url*)
                           ^String (:path url*)
                           ^String (:query url*)
                           ^String (:fragment url*)))))
        in-file (:in-file opts)
        in-file (when in-file ["--data-binary" (str "@" (.getCanonicalPath ^java.io.File in-file))])
        in-stream (:in-stream opts)
        in-stream (when in-stream ["--data-binary" "@-"])
        basic-auth (:basic-auth opts)
        basic-auth (if (sequential? basic-auth)
                     (str/join ":" basic-auth)
                     basic-auth)
        basic-auth (when basic-auth
                     ["--user" basic-auth])
        header-file (.getPath ^File (:header-file opts))
        stream? (identical? :stream (:as opts))]
    [(conj (reduce into (cond-> ["curl" "--silent" "--show-error" "--location" "--dump-header" header-file]
                          (not (false? (:compressed opts))) (conj "--compressed")
                          ;; tested with SSE server, e.g. https://github.com/enkot/SSE-Fake-Server
                          stream? (conj "-N"))
                   [method headers accept-header data-raw in-file in-stream basic-auth
                    form-params #_multipart-params
                    (:raw-args opts)])
           (str url
                (when query-params
                  (str "?" query-params))))
     opts]))

;;;; End utils

;;;; Response Parsing

(defn- read-headers
  [^File header-file]
  (with-open [^java.io.BufferedReader rdr (io/reader header-file)]
    (loop [lines []]
      (if-let [line (.readLine rdr)]
        (recur (conj lines line))
        lines))))

(defn- read-then-unread
  [^java.io.InputStream is]
  (let [c    (.read is)
        eof? (= -1 c)]
    (if eof?
      is
      (-> (byte-array [c])
          (ByteArrayInputStream.)
          (SequenceInputStream. is)))))

(defn- curl-response->map
  "Parses a curl response input stream into a map"
  [opts]
  (let [is ^java.io.InputStream (:out opts)
        ;; curl does not write to :header-file until stdout is read from once.
        ;; This ensures :status and :headers are parsed when option `:as :stream` is set.
        is (read-then-unread is)
        err (:err opts)
        stream? (identical? :stream (:as opts))
        process (:proc opts)
        [body err exit] (if stream?
                          [is err (delay (.waitFor ^java.lang.Process process))]
                          [(slurp is) (slurp err) (.waitFor ^java.lang.Process process)])
        headers (read-headers (:header-file opts))
        [status headers]
        (reduce (fn [[status parsed-headers :as acc] header-line]
                  (if (str/starts-with? header-line "HTTP/")
                    [(Integer/parseInt (second (str/split header-line  #" "))) parsed-headers]
                    (let [[k v] (str/split header-line #":" 2)]
                      (if (and k v)
                        [status (assoc parsed-headers (str/lower-case k) (str/trim v))]
                        acc))))
                [nil {}]
                headers)
        response {:status status
                  :headers headers
                  :body body
                  :err err
                  :process process
                  :exit exit}]
    response))

;;;; End Response Parsing

(def unexceptional-statuses
  #{200 201 202 203 204 205 206 207 300 301 302 303 304 307})

(defn- should-throw? [response opts]
  (and (:throw opts)
       ;; when streaming, we don't know the exit code yet, so it's too early
       ;; to say if we should throw
       (or (and (not (identical? :stream (:as opts)))
                (not (zero? (:exit response))))
           (let [status (:status response)]
             (and status
                  (not (contains? unexceptional-statuses status)))))))

(defn- build-ex-msg [response]
  (cond
    (:status response)
    (str "babashka.curl: status " (:status response))

    (not (str/blank? (:err response)))
    (:err response)

    :else
    "babashka.curl: error"))

(defn request [opts]
  (let [header-file (File/createTempFile "babashka.curl" ".headers")
        opts (assoc opts :header-file header-file)
        default-opts *defaults*
        opts (merge default-opts opts)
        [args opts] (curl-command opts)
        response (let [response (-> (exec-curl args opts)
                                    (curl-response->map))]
                   (.delete header-file)
                   response)
        response (if (:debug opts)
                   (assoc response
                          :command args
                          :options opts)
                   response)]
    (if (should-throw? response opts)
      (throw (ex-info (build-ex-msg response) response))
      response)))

(defn delete
  ([url] (delete url nil))
  ([url opts]
   (let [opts (assoc opts :url url
                     :method :delete)]
     (request opts))))

(defn head
  ([url] (head url nil))
  ([url opts]
   (let [opts (assoc opts :url url
                     :method :head)]
     (request opts))))

(defn get
  ([url] (get url nil))
  ([url opts]
   (let [opts (assoc opts :url url)]
     (request opts))))

(defn post
  ([url] (post url nil))
  ([url opts]
   (let [opts (assoc opts :url url
                     :method :post)]
     (request opts))))

(defn put
  ([url] (put url nil))
  ([url opts]
   (let [opts (assoc opts :url url
                     :method :put)]
     (request opts))))

(defn patch
  ([url] (patch url nil))
  ([url opts]
   (let [opts (assoc opts :url url
                     :method :patch)]
     (request opts))))

(comment
  ;; after running a python server in the source repo with `python3 -m http.server`
  (request {:url      {:host   "localhost"
                       :scheme "http"
                       :port   8000
                       :path   "/src/babashka"}
            :raw-args ["-L"]})

  (request {:url      {:host   "localhost"
                       :scheme "http"
                       :port   8000
                       :path   "/src/babashka"}
            :response true})

  )
