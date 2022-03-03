---
title: Open Api Tutorial
---

Make sure you have [set up the Site cli](cli)

Also check out the [XTDB Primer](xtdb) to understand the data for this tutorial

First we will build and install swager-ui

```shell
cd opt/swager-ui
./install
```

if you have installed the [Login](login)
then you can [Open Swagger-UI](http://localhost:2021/swagger-ui/index.html?url=/_site/apis/site/openapi.json#/APIs/)


## Minimal Schema
open-api.edn
```clojure
{:openapi "3.0.2"
 :info
 {:version "1.0.0"
  :title "Movies"
  :description "Site Tutorial"}

 :servers [{:url "/openmovies"}]

 :paths
 {"/movies/"
  {:get
   {:responses
    {200
     {:juxt.site.alpha/query
      #juxt.site.alpha/as-str
              {:find [(pull e [*])]
               :keys []
               :where [[e :juxt.site/type "Movie"]]}
      :content {"application/json" {}}}}}}}

 :components
 {:schemas
  {"Movie"
   {:type "object"
    :properties
    {"movie/title" {:type "string"}
     "movie/year" {:type "number"}
     }}}}}
```

````shell
site put-api -n openmovies -f open-api.edn
````

[Open the API in Swagger-UI](http://localhost:2021/swagger-ui/index.html?url=/_site/apis/openmovies/openapi.json)





