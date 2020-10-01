(ns juxt.site.db
  (:require [crux.api :as crux]
            [integrant.core :as ig]))

(defmethod ig/init-key ::crux [_ _]
  (println "Starting Crux node")
  (let [node (crux/start-node {})]
    (crux/submit-tx
     node
     [[:crux.tx/put
       {:crux.db/id :holiday

        :crux.schema/description "Your holidays!"

        :crux.schema/attributes
        {:beginning {:crux.schema/type :crux.schema.type/date
                     :crux.schema/label "Beginning"}
         :end {:crux.schema/type :crux.schema.type/date
               :crux.schema/label "End"}
         :description {:crux.schema/type :crux.schema.type/string
                       :crux.schema/label "Description"}}

        :juxt.site/url "http://localhost:8888/holidays"}]])

    node))

(defmethod ig/halt-key! ::crux [_ node]
  (.close node)
  (println "Closed Crux node"))
