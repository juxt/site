;; Copyright © 2021, JUXT LTD.

(ns juxt.site.handler-test
  (:require
   [crux.api :as x]
   [juxt.site.alpha.handler :as h]
   [clojure.test :refer [deftest is are testing] :as t])
  (:import crux.api.ICruxAPI))

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))

(def ^:dynamic *opts* {})
(def ^:dynamic ^ICruxAPI *crux-node*)
(def ^:dynamic *handler*)
(def ^:dynamic *db*)

(defn with-crux [f]
  (with-open [node (x/start-node *opts*)]
    (binding [*crux-node* node]
      (f))))

(defn with-site-data [f]
  (->>
   (x/submit-tx
    *crux-node*
    [[:crux.tx/put {:crux.db/id "https://example.com/index.html"
                    ::http/content-type "text/html;charset=utf-8"
                    ::http/etag "\"123\""}]])
   (x/await-tx *crux-node*))
  (f))

(defn with-handler [f]
  (binding [*handler* (h/make-handler *crux-node* {})]
    (f)))

(defn with-db [f]
  (binding [*db* (x/db *crux-node*)]
    (f)))

(t/use-fixtures :once with-crux with-site-data with-handler)

(deftest fixture-test
  (is (= 4 (+ 2 2)))

  (is (= :dummy (*handler* {:ring.request/method :get
                           :ring.request/path "/index.html"})))
  )

#_(deftest get-with-if-none-match-test
    (let [{status1 :status
           {:strs [etag] :as headers1} :headers}
          (demo/handler
           {:uri "/en/index.html"
            :request-method :get})
          {status2 :status headers2 :headers}
          (demo/handler
           {:uri "/en/index.html"
            :request-method :get
            :headers {"if-none-match" etag}})]
      (is (= 200 status1))
      (is (= #{"date" "content-type" "etag" "last-modified" "content-language" "content-length"}
             (set (keys headers1))))
      (is (= 304 status2))
      ;; "The server generating a 304 response MUST generate any of the following
      ;; header fields that would have been sent in a 200 (OK) response to the
      ;; same request: Cache-Control, Content-Location, Date, ETag, Expires, and
      ;; Vary." -- Section 4.1, RFC 7232
      (is (= #{"date" "etag"} (set (keys headers2)))))

    ;; Check that content-location and vary are also generated, when they would be
    ;; for a 200.
    (let [{status1 :status
           headers1 :headers}
          (demo/handler
           {:uri "/index.html"
            :request-method :get})
          {status2 :status
           headers2 :headers}
          (demo/handler
           {:uri "/index.html"
            :request-method :get
            :headers {"if-none-match" "\"1465419893\""}})]

      (is (= 200 status1))
      (is (= #{"date" "etag" "vary" "content-location"
               "content-type" "content-length" "last-modified" "content-language"}
             (set (keys headers1))))
      (is (= 304 status2))
      (is (= #{"date" "etag" "vary" "content-location"} (set (keys headers2))))
      ;; TODO: test for cache-control & expires
      ))
