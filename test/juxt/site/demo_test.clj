;; Copyright © 2022, JUXT LTD.

(ns juxt.site.demo-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [juxt.site.alpha.repl :as repl]
   [clojure.test :refer [deftest is are testing use-fixtures] :as t]
   [juxt.demo :as demo]
   [juxt.test.util :refer [with-system-xt with-db submit-and-await! *xt-node* *db*]]
   [xtdb.api :as xt]))

(defn with-initial-setup [f]
  (demo/demo-install-do-action-fn!)
  (demo/demo-put-user!)
  (f))

(use-fixtures :each with-system-xt with-initial-setup with-db)

((t/join-fixtures [with-system-xt with-initial-setup with-db])
 (fn []
   ;;(:xtdb.kv/estimate-num-keys (xt/status *xt-node*))
   ;;(xt/entity (xt/db *xt-node*) "https://site.test/users/mal")
   *db*
   (repl/ls)
   ))

(deftest graphql-test
  (is (xt/entity *db* "https://site.test/users/mal"))
  )
