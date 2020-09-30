(ns juxt.site.holidays-test
  (:require [crux.api :as crux])
  )

(with-open
  [node (crux/start-node {})]

  (crux/submit-tx
   node
   [[:crux.tx/put {:crux.db/id :our-holiday
                   :beginning #inst "2020-10-01"
                   :end #inst "2020-10-08"}]])


  )
