---
title: Graphql Tutorial
---

Presentation: [Schema Driven Development - Johanna Antonelli - reClojure 2021](https://www.youtube.com/watch?v=PZVYVAxbzmE)

Also check out the [XTDB Primer](xtdb)


First we will build and install graphiql - this requires node version smaller than `17`

```shell
node --version
```

[nvm](https://github.com/nvm-sh/nvm#installing-and-updating) is a tool to manage node js versions
```bash
 cd opt/graphiql
 nvm install 16
 nvm use 16
 ```

Build the tool - make sure you have [set up the Site cli](cli) first

```shell
 cd graphiql
 make build install
```

## Creating API resource within Site

```clojure
{:xt/id "{{base-uri}}/movies/graphql"
 :juxt.http.alpha/content-type "text/plain;charset=utf-8"
 :juxt.http.alpha/methods #{:get :head :post :put :options}
 :juxt.http.alpha/acceptable-on-put "application/graphql"
 :juxt.http.alpha/acceptable-on-post "application/json"

 :juxt.site.alpha/access-control-allow-origins
 {#regex "http://localhost:\\p{Digit}+"
  {:juxt.site.alpha/access-control-allow-methods #{:post}
   :juxt.site.alpha/access-control-allow-headers #{"authorization" "content-type"}
   :juxt.site.alpha/access-control-allow-credentials true}}

 ;; For handling the upsert the schema
 :juxt.site.alpha/put-fn juxt.site.alpha.graphql/put-handler
 :juxt.http.alpha/put-error-representations
 [{:ring.response/status 400
   :juxt.http.alpha/content-type "application/json"
   :juxt.site.alpha/body-fn juxt.site.alpha.graphql/put-error-json-body}
  {:ring.response/status 400
   :juxt.http.alpha/content-type "text/plain"
   :juxt.site.alpha/body-fn juxt.site.alpha.graphql/put-error-text-body}
  {:ring.response/status 400
   :juxt.http.alpha/content-type "text/html;charset=utf-8"
   :juxt.http.alpha/content "<h1>Error compiling schema</h1>"}]

 ;; For POSTing GraphQL queries
 :juxt.site.alpha/post-fn juxt.site.alpha.graphql/post-handler
 :juxt.http.alpha/post-error-representations
 [{:ring.response/status 400
   :juxt.http.alpha/content-type "text/plain"
   :juxt.site.alpha/body-fn juxt.site.alpha.graphql/post-error-text-body}
  {:ring.response/status 400
   :juxt.http.alpha/content-type "application/json"
   :juxt.site.alpha/body-fn juxt.site.alpha.graphql/post-error-json-body}]}
```

```shell
site post-resources --file movies.edn
```

Add some data in XTDB via the repl
```clojure
(put! {:person/name "Arnold Schwarzenegger",
       :person/born #inst "1947-07-30T00:00:00.000-00:00",
       :movies-api/type "Person"
       :xt/id "http://localhost:2021/movies/-101"}

      {:person/name "Linda Hamilton",
       :person/born #inst "1956-09-26T00:00:00.000-00:00",
       :movies-api/type "Person"
       :xt/id "http://localhost:2021/movies/-102"}

      {:person/name "James Cameron",
       :person/born #inst "1954-08-16T00:00:00.000-00:00",
       :movies-api/type "Person"
       :xt/id "http://localhost:2021/movies/-100"}

      {:movie/title "The Terminator",
       :movie/year 1984,
       :movie/director "http://localhost:2021/movies/-100"
       :movie/cast ["http://localhost:2021/movies/-101" "http://localhost:2021/movies/-102"],
       :movie/sequel "http://localhost:2021/movies/-207",
       :xt/id "http://localhost:2021/movies/-200"
       :movies-api/type "Movie"}

      {:movie/title "Terminator 2: Judgment Day",
       :movie/year 1991,
       :movie/director "http://localhost:2021/movies/-100"
       :movie/cast ["http://localhost:2021/movies/-101" "http://localhost:2021/movies/-102"],
       :xt/id "http://localhost:2021/movies/-207"
       :movies-api/type "Movie"})
```

Site expects `:xt/id` to be the URI of the resource.
It is recommended to use localhost as the server and configuring HTTP redirects rather than hardcode Domains in the databse.

## Define a graphql API

### Site Type Directive

The type directive tells Site what key identifies Graphql types in XTDB
```
schema @site(type: "movie-api/type") {
  query: Query
  mutation: Mutation
}
```
If not set the Site defaults to using the `:juxt.site.alpha/type` however it is recommended to define your own type key to allow data for many APIs to coexist with avoid name collisions or query performance degradation.
```
type Person {
  id: ID!
  name: String @site(a: "person/name")
}
```
Note that to match the `name` field with the key `:person/name` we used in xtdb we add an [attribute site directive](../reference/graphql/site-directive#a) 

In the repl you can list by type:

```clojure
(ls-type "Person")
```

## Query
```
type Query {
  people: [Person]
}
```
as no directive is provided Site defaults to returning all documents that contain `:juxt.site/type "Person"`
```shell
site put-graphql -f schema.graphql -p /movies/graphql
```

now you can explore with [Graphiql](http://localhost:2021/_site/graphiql/index.html?url=/movies/graphql)
Query all people and check the name works

Let's add some schema for Movies:

```
type Movie {
  id: ID!
  title: String @site(a: "movie/title")
  year: Int @site(a: "movie/year")
  director: Person @site(q: {find: [p] where: [[object {keyword: "movie/director"} p]]})
  cast: [Person] @site(q: {find: [p] where: [[object {keyword: "movie/cast"} p]]})
}

type Query {
  people: [Person]
  allMovies: [Movie]
}
```
note we use the [query site directive](../reference/graphql/site-directive#q) 

Alternatively we could use [ref](../reference/graphql/site-directive#ref) for director and [each](../reference/graphql/site-directive#each) for cast( exercise for the reader).

## Mutations
```
type Mutation {
  addPerson(
    id: ID @site(gen: { type: UUID pathPrefix: "{{base-uri}}/movies/" })
    name: String @site(a: "person/name")
  ): Person
  
  updatePerson(
    id: ID!
    name: String @site(a: "person/name")
  ): Person @site(mutation: "update")
  
  deletePerson(
    id: ID!
  ): Person @site(mutation: "delete")
}
```

Complete schema:
```
schema @site(type: "movie-api/type"){
  query: Query
  mutation: Mutation
}

type Person {
  id: ID!
  name: String @site(a: "person/name")
}

type Movie {
  id: ID!
  title: String @site(a: "movie/title")
  year: Int @site(a: "movie/year")
  director: Person @site(q: {find: [p] where: [[object {keyword: "movie/director"} p]]})
  cast: [Person] @site(q: {find: [p] where: [[object {keyword: "movie/cast"} p]]})
  sequel: Movie @site(ref: "movie/sequel")
}

type Query {
  people: [Person]
  allMovies: [Movie]
}

type Mutation {
  addPerson(
    id: ID @site(gen: { type: UUID pathPrefix: "{{base-uri}}/movies/" })
    name: String @site(a: "person/name")
  ): Person
  
  updatePerson(
    id: ID!
    name: String @site(a: "person/name")
  ): Person @site(mutation: "update")
  
  deletePerson(
    id: ID!
  ): Person @site(mutation: "delete")
}
```