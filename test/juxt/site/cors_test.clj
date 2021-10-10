;; Copyright © 2021, JUXT LTD.

(ns juxt.site.cors-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [juxt.site.alpha.handler :refer [access-control-match-origin]]))


(alias 'site (create-ns 'juxt.site.alpha))

(deftest access-control-origin-match-test
  (let [allow-origins
        {#"http://localhost:\p{Digit}+"
         {::site/access-control-allow-methods #{:post}
          ::site/access-control-allow-headers #{"authorization" "content-type"}}}]

    (is (= #{:post} (::site/access-control-allow-methods (access-control-match-origin allow-origins "http://localhost:8080"))))
    (is (nil? (access-control-match-origin allow-origins  "https://home.juxt.site")))))
