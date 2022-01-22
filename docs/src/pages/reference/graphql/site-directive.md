---
title: '@site Directive'
---

<Intro>
The @site directive controls how site turns a graphql query into an XTDB query or transcation.
</Intro>

## Parameters {/_parameters_/}

The site directive does nothing by itself, it must be passed at least one  of the following parameters.

- q
- e
- a
- ref
- history
- mutation
- resolver
- each
- itemForId

### itemForId {/_itemForId_/}

Gets all entities with the given id e.g:

```
;; find all Comment entities which have the field cardId: "foo"
  commentsForCard(id: ID!): [Comment] @site(itemForId: "cardId")

  query commentsForCard(id: "foo") {
    ...
  }
```
