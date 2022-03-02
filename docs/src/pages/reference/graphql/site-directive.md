---
title: '@site Directive'
---

<Intro>
The @site directive controls how site turns a graphql query into an XTDB query or transcation.
</Intro>

## Parameters {/_parameters_/}

The site directive does nothing by itself, it must be passed at least one  of the following parameters.

- [q](#q) 
- e
- [a](#a)
- ref
- history
- mutation
- resolver
- each
- [itemForId](#itemForId)

### q (query) {/_q_/}
Direct the server to use datalog query to fulfil a field.
See [XTDB DataLog Tutorial](https://nextjournal.com/try/learn-xtdb-datalog-today/learn-xtdb-datalog-today)
- `object` represents an instance of the parent type.
- The documents returned from the query needs to have correct `:juxt.site/type` matching the type in graphql schema
```
type Movie {
  id: ID!
  cast: [Person] @site(q: {find: [p] where: [[object {keyword: "movie/cast"} p]]})
}
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
