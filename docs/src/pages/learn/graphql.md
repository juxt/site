---
title: Graphql Tutorial
---

Presentation: [Schema Driven Development - Johanna Antonelli - reClojure 2021](https://www.youtube.com/watch?v=PZVYVAxbzmE)

Also check out the [XTDB Primer](xtdb)

## Inserting/reading data through the REPL

You can insert data directly into XTDB via the REPL using the `put!` helper function.

```clojure
(put! {:person/name "Arnold Schwarzenegger",
       :person/born #inst "1947-07-30T00:00:00.000-00:00",
       :movies/local "Person"
       :xt/id "/movies/-101"}
      {:person/name "Linda Hamilton",
       :person/born #inst "1956-09-26T00:00:00.000-00:00",
       :movies/local "Person"
       :xt/id "/movies/-102"}
      {:person/name "James Cameron",
       :person/born #inst "1954-08-16T00:00:00.000-00:00",
       :movies/local "Person"
       :xt/id "/movies/-100"}
      {:movie/title "The Terminator",
       :movie/year 1984,
       :movie/director "/movies/-100"
       :movie/cast ["/movies/-101" "/movies/-102"],
       :movie/sequel "/movies/-207",
       :xt/id "/movies/-200"
       :movies/local "Movie"}
      {:movie/title "Terminator 2: Judgment Day",
       :movie/year 1991,
       :movie/director "/movies/-100"
       :movie/cast ["/movies/-101" "/movies/-102"],
       :xt/id "/movies/-207"
       :movies/local "Movie"})
```

You can query the raw data from XTDB with the `e` helper, this does an entity lookup given a valid :xt/id value:

```clojure
(e "/movies/-100")
```

You can also do a full query:

```clojure
(q '{:find [e] :where [[e :movies/local "Movie]]})
```

Or to pull all attributes rather than returning the ids:

```clojure
(q '{:find [(pull e *)] :where [[e :movies/local "Movie]]})
```

You can learn more about the powerful Datalog query language used by XTDB [here](https://docs.xtdb.com/language-reference/1.22.0/datalog-queries/).

## Creating a GraphQL API

To create a new GraphQL schema simply add it to the `apis/graphql` directory. Try adding the following to a new file called `apis/graphql/movies.graphql`.

```graphql
schema @site(type: "movies/local") {
  query: Query
}

type Query {
  allMovies: [Movie]
}
```

### Site Type

You may have noticed all of the documents transacted from the REPL have a `:movies/local` attribute on them. And that this matches the value of the `type` directive in the above schema.

Because XTDB is schemaless, we need a way to infer the correct XT query to perform given a GraphQL query, so we use a 'type' attribute to apply a structure to our documents that makes this easier.

This isn't actually required as you can query any document in any shape using the `q` directive, but it does make structuring and maintaining your schemas much easier.

The `type` directive does two things:

1. Every entity inserted into XTDB has an attribute named the value of the `type` directive with a value of the return type of the mutation. So given the following mutation:

```graphql
putUser(id: ID name: String): User @site(type: "baz")
```

The following document will be inserted into XTDB:

```clojure
{:xt/id "foo"
 :name "bar"
 :baz "User"}
```

2. If a query is inferred from the schema (i.e there is no `q` directive), the following where clause will be added to the query `[e :type-key "ReturnType"]`. So for the following GraphQL:

```graphql
allUsers: [User] @site(type: "foo")
```

This is the datalog which will be generated:

```clojure
{:find [e]
 :where [[e :foo "User"]]}
```

The type directive can be anywhere in the schema, but in most cases you will just want it in the top of the file on the schema declaraton. It will then be applied to every query and mutation.

## Query

```
type Query {
  people: [Person]
}
```

As no directive is provided Site defaults to returning all documents that contain `:juxt.site/type "Person"`

```shell
site put-graphql -f schema.graphql -p /movies/graphql
```

Now you can explore with [Graphiql](https://graphiql-online.com), using the endpoint <http://localhost:5509/movies/graphql>

Query all people and check the name works.

Let's add some schema for Movies:

```
type Movie {
  id: ID!
  title: String
  year: Int
  director: Person @site(ref: "directorId")
  cast: [Person] @site(ref: "castIds")
}
type Query {
  people: [Person]
  allMovies: [Movie]
}
```

Note we use the [ref site directive](../reference/graphql/site-directive#q) to join from the Movie entity to the director and cast. The actual document in XT has 'directorId' and 'castIds' attributes and ref will join those to their referenced documents and use the Person type to resolve it.

## Mutations

```
type Mutation {
  addPerson(
    id: ID @site(gen: { type: UUID pathPrefix: "/movies/" })
    name: String
  ): Person

  updatePerson(
    id: ID!
    name: String
  ): Person @site(mutation: "update")

  deletePerson(
    id: ID!
  ): Person @site(mutation: "delete")
}
```

Complete schema:

```graphql
schema {
  query: Query
  mutation: Mutation
}
type Person {
  id: ID!
  name: String
}
type Movie {
  id: ID!
  title: String
  year: Int
  director: Person @site(ref: "directorId")
  cast: [Person] @site(ref: "castIds")
}
type Query {
  people: [Person]
  allMovies: [Movie]
}
type Mutation {
  addPerson(
    id: ID @site(gen: {type: UUID, pathPrefix: "/movies/"})
    name: String
  ): Person

  updatePerson(id: ID!, name: String): Person @site(mutation: "update")

  deletePerson(id: ID!): Person @site(mutation: "delete")
}
```
