;; Copyright © 2021, JUXT LTD.

(ns juxt.site.template-test
  (:require
   [xtdb.api :as x]
   [clojure.test :refer [deftest is are testing] :as t]
   [juxt.test.util :refer [with-xt with-handler submit-and-await!
                           *xt-node* *handler*
                           access-all-areas access-all-apis]]))

;; Site templates can be defined as a resource which references a Template
;; resource. The Template resource provides defaults for the representation
;; metadata of the resource's selected representation. The Template resource
;; also references TemplateModel resource, which specifies the query used to
;; extract the template model from the database. In this way, a template can be
;; shared by multiple instances. Instances may provide data, which is accessible
;; via the query using the 'resource' symbol. This implementation could be
;; extended to support content negotiation, whereby the resource would be the
;; resource of the URL, rather than the negotiated representation.

(alias 'http (create-ns 'juxt.http.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

(t/use-fixtures :each with-xt with-handler)

(comment
  (deftest simple-template-test
    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "Fruits"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::site/template "https://example.org/templates/fruits.html"}]])

    (let [r (*handler*
             {:ring.request/method :get
              :ring.request/path "/fruits.html"})]
      (is (= "Fruits" (:ring.response/body r)))))

  (deftest single-template-model-test
    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "Fruits:{% for f in fruits %} * {{f}}{% endfor %}"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/template-models/fruits"
        :fruits ["apple" "orange" "banana"]}]

      [:xtdb.api/put
       {:xt/id "https://example.org/fruits.html"
        ::http/methods {:get {} :head {} :options {} }
        ::site/template "https://example.org/templates/fruits.html"
        ::site/template-model "https://example.org/template-models/fruits"}]])

    (let [r (*handler*
             {:ring.request/method :get
              :ring.request/path "/fruits.html"})]
      (is (= "Fruits: * apple * orange * banana"
             (:ring.response/body r)))))

  ;; Multiple template models are supported but should encourage a single GraphQL
  ;; query over a single schema (perhaps created via schema-stitching)
  (deftest multiple-template-models-test
    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "Fruits:{% for f in fruits %} * {{f}}{% endfor %}"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/template-models/fruits-a"
        :fruits ["apple" "orange" "banana"]}]

      [:xtdb.api/put
       {:xt/id "https://example.org/template-models/fruits-b"
        :fruits ["strawberry" "kiwi" "pineapple"]}]

      [:xtdb.api/put
       {:xt/id "https://example.org/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::site/template "https://example.org/templates/fruits.html"
        ::site/template-model ["https://example.org/template-models/fruits-a"
                               "https://example.org/template-models/fruits-b"]}]])

    (let [r (*handler*
             {:ring.request/method :get
              :ring.request/path "/fruits.html"})]
      (is (= "Fruits: * apple * orange * banana * strawberry * kiwi * pineapple"
             (:ring.response/body r)))))

  (deftest template-model-as-query-test
    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "Fruits:{% for f in fruits %} * {{f}}{% endfor %}"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/apple"
        :type "Fruit"
        :name "apple"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/orange"
        :type "Fruit"
        :name "orange"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/banana"
        :type "Fruit"
        :name "banana"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/template-models/fruits-a-query"
        :fruits {::site/query '{:find [nm]
                                :where [[e :type "Fruit"]
                                        [e :name nm]]}
                 ::site/extract-first-projection? true}}]

      [:xtdb.api/put
       {:xt/id "https://example.org/template-models/fruits-b"
        :fruits ["strawberry" "kiwi" "pineapple"]}]

      [:xtdb.api/put
       {:xt/id "https://example.org/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::site/template "https://example.org/templates/fruits.html"
        ::site/template-model ["https://example.org/template-models/fruits-a-query"
                               "https://example.org/template-models/fruits-b"]}]])

    (let [r (*handler*
             {:ring.request/method :get
              :ring.request/path "/fruits.html"})]

      (is (= {:fruits ["apple" "banana" "orange" "strawberry" "kiwi" "pineapple"]}
             (:juxt.site.alpha/template-model r)))))

  (deftest template-inclusion-test
    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/outer.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "outer {% include \"https://example.org/templates/inner.html\" %}"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/inner.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "inner"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/index.html"
        ::http/methods {:get {} :head {} :options {}}
        ::site/template "https://example.org/templates/outer.html"}]])

    (let [r (*handler*
             {:ring.request/method :get
              :ring.request/path "/index.html"})]

      (is (= "outer inner"
             (:ring.response/body r)))))

  ;; The include in template-inclusion-test is unwieldy because it requires the
  ;; entire URL of the included template. Often, the absolute URL would not be
  ;; possible to know in advance, and the template would use relative URLs. We can
  ;; support this case by adding :selmer.util/custom-resource-path
  (deftest template-inclusion-with-custom-resource-path-test
    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/outer.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "outer {% include \"inner.html\" %}"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/inner.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "inner"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/index.html"
        ::http/methods {:get {} :head {} :options {}}
        ::site/template "https://example.org/templates/outer.html"
        :selmer.util/custom-resource-path "https://example.org/templates/"}]])

    (let [r (*handler*
             {:ring.request/method :get
              :ring.request/path "/index.html"})]

      (is (= "outer inner" (:ring.response/body r)))))

  ;; Putting it all together into a combined complex case, to test everything
  ;; works together.
  (deftest combination-test
    (submit-and-await!
     [[:xtdb.api/put access-all-areas]

      [:xtdb.api/put
       {:xt/id "https://example.org/apple"
        :type "Fruit"
        :name "apple"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/orange"
        :type "Fruit"
        :name "orange"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/banana"
        :type "Fruit"
        :name "banana"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/template-models/fruits-a-query"
        :fruits {::site/query '{:find [nm] :where [[e :type "Fruit"][e :name nm]]}
                 ::site/extract-first-projection? true}}]

      [:xtdb.api/put
       {:xt/id "https://example.org/template-models/fruits-b"
        :fruits ["strawberry" "kiwi" "pineapple"]}]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/inner.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "{% for f in fruits %} * {{f}}{% endfor %}"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/templates/outer.html"
        ::http/methods {:get {} :head {} :options {}}
        ::http/content-type "text/plain"
        ::site/template-dialect "selmer"
        ::http/content "outer{% include \"inner.html\" %}"}]

      [:xtdb.api/put
       {:xt/id "https://example.org/fruits.html"
        ::http/methods {:get {} :head {} :options {}}
        ::site/template "https://example.org/templates/outer.html"
        ::site/template-model ["https://example.org/template-models/fruits-a-query"
                               "https://example.org/template-models/fruits-b"]
        :selmer.util/custom-resource-path "https://example.org/templates/"}]])

    (let [r (*handler*
             {:ring.request/method :get
              :ring.request/path "/fruits.html"})]

      (is (= "outer * apple * banana * orange * strawberry * kiwi * pineapple" (:ring.response/body r))))))
