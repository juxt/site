;; Copyright © 2021, JUXT LTD.

(ns juxt.dave.alpha.methods
  (:require
   [clojure.tools.logging :as log]
   [xtdb.api :as xt]
   [juxt.dave.alpha :as dave]
   [juxt.dave.alpha.xml :as xml]

   [juxt.reap.alpha.encoders :refer [format-http-date]]))

(alias 'http (create-ns 'juxt.http.alpha))

(defn gen-response-body [request members db authorization subject]
  (let [baos (new java.io.ByteArrayOutputStream)
        doc (.newDocument (xml/dom-builder))]
    (xml/write-doc
     (xml/->dom-node
      doc doc
      {:tag "multistatus" :ns "DAV:"
       :children
       (for [resource members
             :let [body "TODO" ;; (payload/generate-representation-body request resource representation db authorization subject)
                   ]]
         {:tag "response" :ns "DAV:"
          :children
          [{:tag "href" :ns "DAV:" :children [(:crux.db/id resource)]}
           {:tag "propstat" :ns "DAV:"
            :children
            [{:tag "prop" :ns "DAV:"
              :children
              [{:tag "resourcetype" :ns "DAV:"}
               {:tag "getcontentlength" :ns "DAV:" :children [(str (count body))]}
               {:tag "getetag" :ns "DAV:" :children ["\"1234\""]}
               {:tag "getlastmodified" :ns "DAV:" :children [(some-> resource ::http/last-modified format-http-date)]}]}]}]})})

     baos
     {:format-pretty-print false
      :xml-declaration true})
    (.toByteArray baos)))

(defn propfind [request resource date xtdb-node authorization subject]
  (let [db (crux/db xtdb-node)
         ;; See https://tools.ietf.org/html/rfc4918#section-9.1
        depth (get-in request [:headers "depth"] "infinity")
        doc (xml/->document (:body request))
        members (map first (crux/q db {:find '[(pull e [*])]
                                       :where (::dave/query resource)}))]
    {:status 207
     :headers {"content-type" "application/xml"}
     :body (java.io.ByteArrayInputStream. (gen-response-body request members db authorization subject))}))
