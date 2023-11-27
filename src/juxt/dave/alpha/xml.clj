;; Copyright © 2021, JUXT LTD.

(ns juxt.dave.alpha.xml
  (:require
   [clojure.tools.logging :as log]
   [xtdb.api :as xtdb]))

(defn write-doc
  ([doc output-stream]
   (write-doc doc output-stream {}))
  ([doc output-stream options]
   ;; See https://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407/load-save.html
   ;; for a full list of options
   (let [registry (org.w3c.dom.bootstrap.DOMImplementationRegistry/newInstance)
         ls (.getDOMImplementation registry "LS")
         serializer (.createLSSerializer ls)
         output (doto (.createLSOutput ls) (.setByteStream output-stream))
         dom-config (.getDomConfig serializer)]

     (doseq [[k v] options]
       (.setParameter dom-config (name k) v))

     (.write serializer doc output))))

(defn doc->str
  ([doc] (doc->str doc {}))
  ([doc opts]
   (let [baos (new java.io.ByteArrayOutputStream)]
     (write-doc doc baos opts)
     (String. (.toByteArray baos)))))

(defn dom-builder []
  (.newDocumentBuilder (javax.xml.parsers.DocumentBuilderFactory/newInstance)))

(comment
  (let [doc (.newDocument (dom-builder))
        el (.createElementNS doc "http://www.example.com/ns/" "foo")]
    (.appendChild doc el)
    (doc->str doc)))

(defn ->document [in]
  (let [registry (org.w3c.dom.bootstrap.DOMImplementationRegistry/newInstance)
        ls (.getDOMImplementation registry "LS")
        parser (.createLSParser
                ls
                org.w3c.dom.ls.DOMImplementationLS/MODE_SYNCHRONOUS
                nil)
        input (doto (.createLSInput ls) (.setByteStream in))]
    (.parse parser input)))


#_(defmacro dom [doc parent el]
  (let [docsym (gensym "doc")
        parentsym (gensym "parent")
        elsym (gensym "el")
        fst (first el)]
    `(let [~parentsym ~parent
           [enm# ens#] ~(cond (vector? fst) fst
                              (string? fst) [fst nil])
           ~docsym ~doc
           ~elsym (.createElementNS ~docsym ens# enm#)]
       ~@(concat
          (for [item (next el)]
            (cond
              ;; Elements
              (list? item) `(.appendChild ~elsym (dom ~docsym ~parentsym ~item))
              ;; Text nodes
              (or
               (string? item)
               (number? item))
              `(.appendChild ~elsym (.createTextNode ~docsym (str ~item)))))

          (list `(.appendChild ~parentsym ~elsym))))))

(defn ->dom-node [doc parent desc]
  (cond
    (map? desc)
    (let [el (.createElementNS doc (:ns desc) (:tag desc))]
      (doseq [child (:children desc)]
        (->dom-node doc el child))
      (.appendChild parent el))
    (string? desc)
    (let [node (.createTextNode doc desc)]
      (.appendChild parent node)))
  parent)

(comment
  (let [doc (.newDocument (dom-builder))
        input
        {:tag "multistatus" :ns "DAV:"
         :children
         [{:tag "response" :ns "DAV:"
           :children
           [{:tag "href" :ns "DAV:" :children ["file-1"]}
            {:tag "propstat" :ns "DAV:"
             :children
             [{:tag "prop" :ns "DAV:"
               :children
               [{:tag "getcontentlength" :ns "DAV:" :children ["200"]}]}]}]}]}
        ]
    (println (doc->str (->dom-node doc doc input) {:format-pretty-print true
                                                   :xml-declaration false}))))

(comment
  (println
   (doc->str
    (let [doc (.newDocument (dom-builder))]
      (dom
       doc doc
       (["multistatus" "DAV:"]
        (["response" "DAV:"]
         (["href" "DAV:"] "file-1")
         (["propstat" "DAV:"]
          (["prop" "DAV:"]
           (["getcontentlength" "DAV:"] "200")))))))

    {:format-pretty-print false
     :xml-declaration false})))

(comment
  (let [doc "<?xml version=\"1.0\" encoding=\"utf-8\"?>
<propfind xmlns=\"DAV:\"><prop>
<resourcetype xmlns=\"DAV:\"/>
<getcontentlength xmlns=\"DAV:\"/>
<getetag xmlns=\"DAV:\"/>
<getlastmodified xmlns=\"DAV:\"/>
<executable xmlns=\"http://apache.org/dav/props/\"/>
</prop></propfind>"]
    (->document (java.io.ByteArrayInputStream. (.getBytes doc "utf-8")))))



(comment
  (println
   (let [doc (.newDocument (dom-builder))
         multistatus (.createElementNS doc "DAV:" "multistatus")
         response (.createElementNS doc "DAV:" "multistatus")
         href (.createElementNS doc "DAV:" "multistatus")
         t (.createTextNode doc "http://www.example.com/file")
         propstat (.createElementNS doc "DAV:" "propstat")
         prop (.createElementNS doc "DAV:" "prop")
         ]
     (.appendChild href t)
     (.appendChild response href)
     (.appendChild propstat prop)
     (.appendChild response propstat)
     (.appendChild multistatus response)
     (.appendChild doc multistatus)

     (doc->str doc {:format-pretty-print true}))))

(comment
  (println
   (doc->str
    (let [doc (.newDocument (dom-builder))]
      (dom
       doc doc
       (["multistatus" "DAV:"]
        (["response" "DAV:"]
         (["href" "DAV:"] "http://www.example.com/file")
         (["propstat" "DAV:"]
          (["prop" "DAV:"]
           (["bigbox" "DAV:"])))))))
    {:format-pretty-print true})))



(comment
  "<?xml version=\"1.0\" encoding=\"utf-8\" ?>
     <D:multistatus xmlns:D=\"DAV:\">
       <D:response xmlns:R=\"http://ns.example.com/boxschema/\">
         <D:href>http://www.example.com/file</D:href>
         <D:propstat>
           <D:prop>
             <R:bigbox>
               <R:BoxType>Box type A</R:BoxType>
             </R:bigbox>
             <R:author>
               <R:Name>J.J. Johnson</R:Name>
             </R:author>
           </D:prop>
           <D:status>HTTP/1.1 200 OK</D:status>
         </D:propstat>
         <D:propstat>
           <D:prop><R:DingALing/><R:Random/></D:prop>
           <D:status>HTTP/1.1 403 Forbidden</D:status>
           <D:responsedescription> The user does not have access to the
      DingALing property.
           </D:responsedescription>
</D:propstat>

       </D:response>
       <D:responsedescription> There has been an access violation error.
       </D:responsedescription>
     </D:multistatus>")
