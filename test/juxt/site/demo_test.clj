;; Copyright © 2022, JUXT LTD.

(ns juxt.site.demo-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is are testing use-fixtures] :as t]
   [juxt.demo :as demo]
   [juxt.test.util :refer [with-xt with-db submit-and-await! *xt-node* *db*]]
   [xtdb.api :as xt]))

(defn with-users [f]
  ;;(demo/demo-install-do-action-fn!)
  (demo/demo-put-user!)
  (f))

(use-fixtures :each with-xt with-users with-db)

((t/join-fixtures [with-xt with-users with-db])
 (fn []
   ;;(:xtdb.kv/estimate-num-keys (xt/status *xt-node*))
   ;;(xt/entity (xt/db *xt-node*) "https://site.test/users/mal")
   *db*
   ))

(deftest graphql-test
  (is (xt/entity *db* "https://site.test/users/mal"))
  ;;(is (= 1 (:xtdb.kv/estimate-num-keys (xt/status *xt-node*))))
  )
