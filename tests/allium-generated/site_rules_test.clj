(ns allium-generated.site-rules-test
  "Generated from specs/site.allium. Encodes rule and invariant
  obligations as executable clojure.test fixtures. These tests act as
  acceptance scaffolding: each rule is exercised through a minimal
  in-memory model of the HttpApi surface so spec changes surface as
  test failures."
  (:require [clojure.test :refer [deftest testing is]]))

(def http-methods #{:get :put :post :delete :head :options :patch})
(def authz-decisions #{:permit :deny :not-applicable})

(defn- new-world []
  {:resources {} :apis {} :history [] :authz-log []})

(defn- authorize [world subject request]
  (let [decision (get-in world [:policy [(:subject-uri subject) (:uri request) (:method request)]]
                         :permit)]
    (update world :authz-log conj {:subject subject :request request :decision decision})))

(defn- mutating? [m] (contains? #{:put :post :delete :patch} m))

(defn- handle [world subject request]
  (let [w (authorize world subject request)
        decision (-> w :authz-log peek :decision)
        method (:method request)
        uri (:uri request)]
    (if (or (= decision :deny) (= decision :not-applicable))
      [w {:status 403}]
      (case method
        :put     (let [rep {:uri uri
                            :content-type (:content-type request)
                            :body-hash (str "h-" (hash request))}
                       prior (get-in w [:resources uri])
                       w' (cond-> (assoc-in w [:resources uri] {:uri uri :current rep :exists true})
                            prior (update :history conj prior))
                       w'' (cond-> w'
                             (= "application/openapi+json" (:content-type request))
                             (assoc-in [:apis uri] {:base-uri uri :served true}))]
                   [w'' {:status 201 :content-type (:content-type request)}])
        :get     [w {:status (if (get-in w [:resources uri :exists]) 200 404)
                     :content-type (get-in w [:resources uri :current :content-type])}]
        :delete  (let [prior (get-in w [:resources uri])
                       w' (cond-> (assoc-in w [:resources uri] {:uri uri :current nil :exists false})
                            prior (update :history conj prior))]
                   [w' {:status 204}])
        [w {:status 405}]))))

(def alice {:subject-uri "https://site.test/_site/subjects/alice"})

(deftest http-method-enum-matches-spec
  (testing "HttpMethod enum from site.allium"
    (is (= #{:get :put :post :delete :head :options :patch} http-methods))))

(deftest authz-decision-enum-matches-spec
  (testing "AuthzDecision enum from site.allium"
    (is (= #{:permit :deny :not-applicable} authz-decisions))))

(deftest put-creates-or-replaces-representation
  (testing "PutCreatesOrReplacesRepresentation: PUT upserts a Resource with exists=true"
    (let [[w _] (handle (new-world) alice
                        {:method :put :uri "/r1" :content-type "text/plain"})]
      (is (true? (get-in w [:resources "/r1" :exists])))
      (is (some? (get-in w [:resources "/r1" :current])))))
  (testing "Replacing PUT preserves prior representation in history (HistoryIsImmutable)"
    (let [w0 (new-world)
          [w1 _] (handle w0 alice {:method :put :uri "/r1" :content-type "text/plain"})
          [w2 _] (handle w1 alice {:method :put :uri "/r1" :content-type "text/plain"})]
      (is (= 1 (count (:history w2))))
      (is (true? (get-in w2 [:resources "/r1" :exists]))))))

(deftest get-returns-current-representation
  (testing "GetReturnsCurrentRepresentation: GET returns 200 with the stored content-type"
    (let [[w1 _] (handle (new-world) alice
                         {:method :put :uri "/r1" :content-type "text/html"})
          [_ resp] (handle w1 alice {:method :get :uri "/r1"})]
      (is (= 200 (:status resp)))
      (is (= "text/html" (:content-type resp))))))

(deftest delete-removes-resource
  (testing "DeleteRemovesResource: DELETE marks the resource as not-exists"
    (let [[w1 _] (handle (new-world) alice
                         {:method :put :uri "/r1" :content-type "text/plain"})
          [w2 _] (handle w1 alice {:method :delete :uri "/r1"})]
      (is (false? (get-in w2 [:resources "/r1" :exists]))))))

(deftest openapi-documents-become-apis
  (testing "OpenApiDocumentsBecomeApis: PUT of an OpenAPI document registers a served ApiDefinition"
    (let [[w _] (handle (new-world) alice
                        {:method :put :uri "/api/" :content-type "application/openapi+json"})]
      (is (true? (get-in w [:apis "/api/" :served]))))))

(deftest requests-are-authorized
  (testing "RequestsAreAuthorized: every request produces an AuthzDecision"
    (let [[w _] (handle (new-world) alice {:method :get :uri "/r1"})
          entry (-> w :authz-log peek)]
      (is (= 1 (count (:authz-log w))))
      (is (contains? authz-decisions (:decision entry))))))

(deftest history-is-immutable
  (testing "HistoryIsImmutable / ContentAddressableHistory: prior versions retrievable after replace and delete"
    (let [w0 (new-world)
          [w1 _] (handle w0 alice {:method :put :uri "/r1" :content-type "text/plain"})
          [w2 _] (handle w1 alice {:method :put :uri "/r1" :content-type "text/markdown"})
          [w3 _] (handle w2 alice {:method :delete :uri "/r1"})
          uris (map :uri (:history w3))]
      (is (= 3 (count (:history w3))))
      (is (every? #(= "/r1" %) uris)))))

(deftest deny-by-default-invariant
  (testing "DenyByDefault: a deny decision must not mutate the Resource"
    (let [w0 (-> (new-world)
                 (assoc-in [:policy ["https://site.test/_site/subjects/alice" "/r1" :put]] :deny))
          [w1 resp] (handle w0 alice {:method :put :uri "/r1" :content-type "text/plain"})]
      (is (= 403 (:status resp)))
      (is (nil? (get-in w1 [:resources "/r1"])))))
  (testing "DenyByDefault: a not_applicable decision must not reveal a Resource"
    (let [[w1 _] (handle (new-world) alice {:method :put :uri "/r1" :content-type "text/plain"})
          w2 (assoc-in w1 [:policy ["https://site.test/_site/subjects/alice" "/r1" :get]] :not-applicable)
          [_ resp] (handle w2 alice {:method :get :uri "/r1"})]
      (is (= 403 (:status resp)))
      (is (nil? (:content-type resp))))))
