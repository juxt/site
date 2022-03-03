---
title: '@site Directive'
---

<Intro>
The @site directive controls how site turns a graphql query into an XTDB query or transcation.
</Intro>

## Parameters {/_parameters_/}

The site directive does nothing by itself, it must be passed at least one  of the following parameters.

- [type](#type)
- [q](#q) 
- [ref](#ref)
- [each](#each)
- e
- [a](#a)
- history
- mutation
- resolver
- [itemForId](#itemForId)

### type {/_type_/}
The type directive tells Site what key identifies Graphql types in XTDB
```
schema @site(type: "movie-api/type") {
  query: Query
  mutation: Mutation
}
```
directs Site to use the `:movie-api/type` attribute when resolving Types

If not set the Site defaults to using the `:juxt.site.alpha/type` however it is recommended to define a type key to allow data for many APIs to coexist with avoid name collisions or query performance degradation.

### q (query) {/_q_/}
Directs the server to use datalog query to fulfil a field.
See [XTDB DataLog Tutorial](https://nextjournal.com/try/learn-xtdb-datalog-today/learn-xtdb-datalog-today)
- `object` represents an instance of the parent type.
- only documents containing a [type](#type) attribute matching the Graphql type will be returned  - TODO is this correct?
```
type Movie {
  id: ID!
  director: Person @site(q: {find: [p] where: [[object {keyword: "movie/director"} p]]})
}
```
will return documents where `:xt/id` matches values in `:movie/cast` 

### ref (reference) {/_ref_/}
Referencing a single entity (as in the query above) is a common senario.
The ref directive is syntac sugar . This is equvalent to the query above
```
director Person @site(ref: "movie/director")
```
Additionally, `ref` will perform a reverse lookup if no document is found looking forward.

### each  {/_each_/}
Each directs the server to reference a list of entities: 
```
cast [Person] @site(ref: "movie/cast")
```

### a (attribute) {/_a_/}

Direct the server to relevant attribute used in XTDB. Useful for namespaced keys.
```
type Person {
  id: ID!
  name: String @site(a: "person/name")
}
```
Not required when the field name matches the attrubue key.

### itemForId {/_itemForId_/}

Gets all entities with the given id e.g:

```
;; find all Comment entities which have the field cardId: "foo"
  commentsForCard(id: ID!): [Comment] @site(itemForId: "cardId")

  query commentsForCard(id: "foo") {
    ...
  }
```
