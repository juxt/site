---
title: GraphQL in Site
---

<Intro>
All that is needed to create a fully functioning GraphQL server is to create a schema.

A single graphQL compliant schema file provides Site with all the data it needs to expose a GraphQL endpoint, transct or query data with XTDB and validate the incoming and outgoing data.
</Intro>

## Creating a schema {/_creating-a-schema_/}

The following is a simple example of a schema file.

<APIAnatomy title="schema.graphql">

<AnatomyStep title="Define a schema type">

This type declares a Query and Mutation type and also sets the 'Site Type' to `myapp/type`. The 'Site Type' is transcted on every document when you use a mutation defined in this schema.

</AnatomyStep>

<AnatomyStep title="Define your data model">

Any 'Type' definition not used in the above schema can be used as return types in your queries and mutations. Site has several built-in [Scalar Types](/reference/graphql/scalars) but you can also define your own.

</AnatomyStep>

<AnatomyStep title="Define Mutations">

A mutation takes arguments (in parentheses) and returns a Type which must be defined in this schema. By default mutations are 'Create' operations and will auto-generate a unique ID for the new document. You can specify other types of mutation with [Directives](/reference/graphql/site-directive).

</AnatomyStep>

<AnatomyStep title="Define Queries">

Queries are defined with the same syntax as mutations, though the possible directives are different. By default a query looks up all documents with a matching 'Site Type' to the return type of the query. You can specify other types of query with [Directives](/reference/graphql/site-directive).

</AnatomyStep>

```graphql [[1, 5], [6, 17], [18, 23], [24, 29]]
schema @site(type: "myapp/type") {
  query: Query
  mutation: Mutation
}

type Episode {
  id: ID!
  name: String!
}

type Character {
  id: ID!
  name: String!
  episodeId: ID!
  appearsIn: [Episode] @site(ref: "episodeId")
}

type Mutation {
  createCharacter(name: String!, episodeId: ID!): Character

  createEpisode(name: String!): Episode
}

type Query {
  character(id: ID!): Character @site(e: "id")
  characters: [Character]
  episodes: [Episode]
}
```

</APIAnatomy>
