;; Copyright © 2021, JUXT LTD.

(ns juxt.site.alpha.home
  (:require
   [clojure.java.io :as io]
   [juxt.site.alpha.tailwind :as tw]
   [clojure.tools.logging :as log]
   [crux.api :as crux]
   [hiccup.page :as hp]
   [clojure.pprint :refer [pprint]]
   [integrant.core :as ig]
   [juxt.spin.alpha :as spin]
   [juxt.site.alpha.payload :as payload]))

(alias 'apex (create-ns 'juxt.apex.alpha))
(alias 'http (create-ns 'juxt.http.alpha))
(alias 'pass (create-ns 'juxt.pass.alpha))
(alias 'site (create-ns 'juxt.site.alpha))

;; This ns is definitely optional

(defmethod payload/generate-representation-body ::user-home-page
  [request resource representation db authorization subject]

  (log/trace (::pass/username subject) (::user resource))

  (let [owner (crux/entity db (::owner resource))]

    (case (::http/content-type representation)
      "text/html;charset=utf-8"

      (let [owner? (= (::pass/user subject) (::owner resource))
            prefix (if owner? "My" (str (:name owner) "'s"))]
        (hp/html5

         [:html
          [:head
           [:link {:rel "stylesheet" :href "/css/tailwind/styles.css"}]]

          [:body
           [:div {:class "flex flex-col p-4"}
            (if owner?
              [:h1 {:class "text-xl"} "Console"]
              [:h1 (str (:name owner) "'s" " page")])

            (when-let [bookings
                       (seq (map first (crux/q db '{:find [(eql/project e [*])]
                                                   :where [[e ::site/type "WorkBooking"]]})))]
              [:div {:class "p-4"}
               [:h2 (str prefix " timesheets")]
               [:table {:style "border: 1px solid black; border-collapse: collapse"}
                [:tbody
                 (for [booking bookings]
                   [:tr
                    [:td (:title booking)]
                    [:td (:state booking)]])]]])

            (when-let [items
                       (seq (map first (crux/q db '{:find [(eql/project e [*])]
                                                   :where [[e ::site/type "OpenAPI"]]})))]
              [:div {:class "py-4"}
               [:div {:class "py-2 text-xl"}
                [:h2 "APIs"]]
               [:div {:class "py-2"}
                [:table {:style "border: 1px solid black; border-collapse: collapse"}
                 [:tbody
                  (for [item items
                        :let [{:strs [info servers]} (::apex/openapi item)]]
                    [:tr
                     [:td
                      (tw/link
                       (format "https://home.juxt.site/_site/swagger-ui/index.html?url=%s"
                               (tw/relativize (:crux.db/id item)))
                       (get info "title"))]
                     [:td (get info "description")]
                     [:td (get info "version")]
                     [:td (let [[_ tlc]
                                (re-matches #"(.*)@juxt.pro" (get-in info ["contact" "email"]))]
                            (cond->>
                                (get-in info ["contact" "name"])
                              tlc (tw/link (format "/~%s/" tlc))))]

                     [:td (get-in servers [0 "url"])]])]]]])]

           [:footer {:class "bg-gray-400 p-4"}
            [:form {:action "/_site/logout" :method "POST"}
             [:input {:class "px-4" :type "submit" :value "Logout"}]]]
           ]])))))

(defn create-user-home-page [request crux-node subject]
  (crux/submit-tx
   crux-node
   [[:crux.tx/put
     {:crux.db/id (str "https://home.juxt.site" (:uri request))
      ::pass/classification "PUBLIC"
      ::owner (::pass/user subject)
      ::http/methods #{:get :head :options}
      ::http/representations
      [{::http/content-type "text/html;charset=utf-8"
        ::http/bytes-generator ::user-home-page}]}]]))

(defmethod payload/generate-representation-body ::user-empty-home-page
  [request resource representation db authorization subject]
  ;;(log/trace (::pass/username subject) (::user resource))
  (case (::spin/content-type representation)
    "text/html;charset=utf-8"
    (if (= (::pass/user subject) (::owner resource))
      ;; TODO: Perhaps better to look up the entity at authentication and put into subject
      (let [user (crux/entity db (::pass/user subject))]
        (hp/html5
         [:h1 (format "Hello %s!" (:name user))]

         [:p "Welcome to Site"]

         [:p "Click on the button below to create your home area"]

         [:p "But first, read this legal stuff and provide your consent, etc."]

         [:small " Lorem ipsum dolor sit amet, consectetur adipiscing
         elit. Morbi cursus sem libero, in viverra magna tincidunt a. Lorem
         ipsum dolor sit amet, consectetur adipiscing elit. Ut lacus quam,
         sagittis id nisl tristique, volutpat consequat lectus. Nunc arcu dui,
         ullamcorper consectetur ornare nec, lacinia vitae nibh. Suspendisse
         fermentum malesuada ante, sed placerat lorem lobortis sed. Nullam
         bibendum interdum arcu, eu commodo ligula pulvinar nec. Lorem ipsum
         dolor sit amet, consectetur adipiscing elit. Etiam sit amet semper
         ligula, imperdiet egestas ipsum. Duis aliquet ex id nisi ultrices, id
         aliquam leo tincidunt. Aenean interdum id leo eget tempor. Cras massa
         felis, sodales ac iaculis nec, aliquam ut mauris. Pellentesque aliquet
         mattis ex, at semper est condimentum nec."]

         [:p "Now let's create your page!"]

         [:form {:method "POST"}
          [:input {:type "submit" :value "Create my home page"}]]))

      (throw
       (ex-info
        "User's page isn't yet created"
        {::spin/response {:status 404 :body "Not Found\r\n\r\n(but coming soon!)\r\n"}})))))

(defn locate-resource [db request]
  ;; Add a trailing slash if necessary
  (when-let [[_ _] (re-matches #"/~(\p{Alpha}[\p{Alnum}_-]*)$" (:uri request))]
    (throw ;; TODO: Promote this to a spin function
     (ex-info
      "Add trailing space"
      {::spin/response {:status 302 :headers {"location" (str (:uri request) "/")}}})))

  (when-let [[_ owner] (re-matches #"/~(\p{Alpha}[\p{Alnum}_-]*)/" (:uri request))]
    (when-let [user (crux/entity db (format "/_site/pass/users/%s" owner))]
      {::site/resource-provider ::empty-personal-home-page
       ::http/methods #{:get :head :options :post}
       ::pass/classification "PUBLIC"
       ::owner (:crux.db/id user)
       ::http/representations
       [{::http/content-type "text/html;charset=utf-8"
         ::site/bytes-generator ::user-empty-home-page}]})))

(defmethod payload/generate-representation-body ::home-page
  [request resource representation db authorization subject]
  ;; A default page (if one doesn't exist)
  (hp/html5

   (if-let [username (::pass/username subject)]
     (hp/html5
      [:html
       [:head
        [:link {:rel "stylesheet" :href "/css/tailwind/styles.css"}]]
       [:body
        (let [user (crux/entity db (::pass/user subject))]
          [:div {:class "bg-gray-100"}
           [:h2 "Welcome to site"]
           [:div {:class "flex flex-col"}
            [:p "You are logged in as " username]

            [:p "TODO: Show auth method, if cookie, then allow to logout"]

            [:p [:a {:href (format "/~%s/" username)} "My page"]]]])
        [:footer
         [:form {:action "/_site/logout" :method "POST"} [:input {:type "submit" :value "Logout"}]]]]])

     ;; Otherwise let them login - this should be some static html or template
     ;; in the database
     (throw
      (ex-info
       "You must login"
       {::spin/response
        {:status 302
         :headers {"location" "/_site/login"}}})))))
