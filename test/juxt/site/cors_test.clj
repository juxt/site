;; Copyright © 2021, JUXT LTD.

(ns juxt.site.cors-test
  (:require
   [clojure.test :refer [deftest is]]
   [juxt.site.handler :refer [access-control-match-origin]]))

(deftest access-control-origin-match-test
  (let [allow-origins
        {#"http://localhost:\p{Digit}+"
         {:juxt.site/access-control-allow-methods #{:post}
          :juxt.site/access-control-allow-headers #{"authorization" "content-type"}}}]

    (is (= #{:post} (:juxt.site/access-control-allow-methods (access-control-match-origin allow-origins "http://localhost:8080"))))
    (is (nil? (access-control-match-origin allow-origins  "https://home.juxt.site")))))
