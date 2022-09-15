---
title: '@site Directive'
---

<Intro>
The @site directive controls how site turns a graphql query into an XTDB query or transcation.
</Intro>

## Parameters {/_parameters_/}

The site directive does nothing by itself, it must be passed at least one of the following parameters.

- [q](#q)
- [e](#e)
- [a](#a)
- [ref](#ref)
- [each](#each)
- [history](#history)
- [mutation](#mutation)
- [resolver](#resolver)
- [itemForId](#itemForId)

### q (query) {/_q_/}

Direct the server to use datalog query to fulfil a field.
See [XTDB DataLog Tutorial](https://nextjournal.com/try/learn-xtdb-datalog-today/learn-xtdb-datalog-today)

this directive can take one of two things:

- a graphql object like this, note that keywords need to be wrapped in objects

```graphql
type Movie {
  id: ID!
  cast: [Person] @site(q: {find: [p], where: [[_, {keyword: "movie/cast"}, p]]})
}
```

- an edn string, three double quotes in graphql represents a multiline string. This can be more convienient for copying and pasting queries from the repl.

```graphql
type Movie {
  id: ID!
  cast: [Person]
    @site(
      q: {
        edn: """
        {:find [p]
         :where [[_ :movie/cast p]]]}
        """
      }
    )
}
```

both are effectively equivilent and have pros and cons. the first way will have editor support for formatting and linting, the second is more concise, doesn't have commas and other JSON nonsense to deal with and can allow easy copy/pasting from the repl.

The edn format has the additional ability to insert various arguments using a selmer template syntax.

the current template options look like this:

```clojure
"object-id" ;; the id of the currrent object (type)

"args"      ;; a map representing the graphql arguments

"type-k"    ;; a keyword as passed into the 'type' directive

"user-id"   ;; a string representing the unique id of the current user
            ;; (parsed from the access token)
```

an example use case is using values passed by the graphql user in the datalog query:

Query made by the user

```graphql
searchMovies(query: "batman") {
  name
}
```

Schema

```graphql
type Movie {
  id: ID!
  name: String!
}

type Query {
  searchMovies(query: String!, limit: Int): Movie
    @site(
      q: {
        edn: """
        {:find [e]
         :where [
         [e {{type-k}} "Movie"]
         [(text-search :name {{args.query}})]]
         :limit {{args.limit}}}
        """
      }
    )
}
```

### e (entity) {/_e_/}

Does an entity lookup using the value of `e` as the xt/id.

For example the following type will resolve admin by returning the document in XTDB under "admin-user".

The value in the db doesn't need to exactly match the User type, but if User specifies required fields that are not present in the db, graphql will return null from the query.

```graphql
type SystemInfo {
  admin: User @site(e: "admin-user")
}
```

### a (attribute) {/_a_/}

Direct the server to relevant attribute used in XTDB. Useful for namespaced keys.

```graphql
type Person {
  id: ID!
  name: String @site(a: "person/name")
}
```

Not required when the field name matches the attrubue key.

### ref {/_ref_/}

One of the more commonly used directives, used when you need to join across documents in the db.

Given a db containing the following documents:

```json
{:xt/id "house1"
 :type "mansion"}

{:xt/id "owner1"
 :name "bob"
 :ownedHouseId "house1"}

```

You can define attributes that join onto the relevant XT document in both directions.

```graphql
type Owner {
  id: ID!
  name: String!
  house: House @site(ref: "ownedHouseId")
}

type House {
  id: ID!
  type: String!
  owner: Owner @site(ref: "ownedHouseId")
}
```

This only works for one to one relationships. For one to many use `each`.

### each {/_each_/}

Also for joining, but used when you want to make a one-to-many relationship

say our house can now have multiple owners, we do this instead.

Data in DB

```clojure
{:xt/id "house1"
 :type "mansion"}

{:xt/id "owner1"
 :name "bob"
 :ownedHouseId "house1"}

{:xt/id "owner2"
 :name "sally"
 :ownedHouseId "house1"}

```

note that owner becomes 'owners' which returns a list, so we use each instead of ref

```graphql
type Owner {
  id: ID!
  name: String!
  house: House @site(ref: "ownedHouseId")
}

type House {
  id: ID!
  type: String!
  owners: [Owner] @site(each: "ownedHouseId")
}
```

### history {/_history_/}

Requires an id argument and for the return value to be a list of the type of item for the given id.

Returns a list of the history of the given item sorted using the value of the history directive.
Possible values are 'desc' or 'asc'.

Example:

```graphql
progressHistory(id: ID!): [ProgressItem] @site(history: "desc")
```

### mutation {/_mutation_/}

This directive is optional for any mutation type and is used to declare the
mutation type site should perform.

It is possible to provide values directly in the arg definitions but its
excouraged to create an 'input' type instead. See the examples at the bottom of
this section.

The options are:

- 'put' - creates a new entity using the args given, generating an xt/id if an
  "id" key is not present in the arguments. This is the default if there is no
  directive.

- 'update' - same as 'put' except it first does an entity lookup for a document
  matching the id key in the arguments and merges that document with the
  provided one. If no existing entity found site creates a new one.

- 'delete' - must receive an id argument, and does an xt delete for that ID.

- 'evict-cascade' - must receive an id and a cascadeKey argument. It looks up
  all entity ids where the attribute 'cascadeKey' has a value of 'id' and
  evicts them.

Mutations can also handle multiple entities at once if a list is provided. See
examples below:

```graphql
type EntityMutations {
  addEntity(entity: EntityInput): Entity
  updateEntity(id: ID, entity: EntityInput): Entity @site(mutation: "update")
  deleteEntity(id: ID): Entity @site(mutation: "delete")
  addMultipleEntities(entities: [EntityInput]): [Entity]
}

type JoinedEntity {
  id: ID!
  name: String
}

type Entity {
  id: ID!
  img: String
  name: String
  joined: JoinedEntity @site(ref: "joinedEntityId")
}

input EntityInput {
  img: String
  name: String
  joinedEntityId: ID
}
```

### resolver {/_resolver_/}

Sometimes the default behaviour of site is not enough, in these cases you can write your own clojure function that determins how the graphql operation should be resolved.

The rules here are to:

1. Create a function somewhere on the classpath
2. reference that function in a resolver directive

As an example we will create a file in `src/company/events.clj` with the contents:

```clojure
(ns company.events
  (:require [juxt.site.alpha.graphql :as graphql]
            [clojure.tools.logging :as log]
            [company.emails :as emails]
            [xtdb.api :as xt]))

(defn mutate-and-email
  [{:keys [argument-values db] :as opts}]
  (let  [{:keys [xt/id] :as event}
         (try
           (graphql/perform-mutation! opts)
           (catch Exception e
             (log/error "Error creating event" e)))
         email-data (xt/entity db (get argument-values "emailDataId"))]
    (emails/send-email email-data id)
    event))
```

and in the schema:

```graphql
  addThing(thing: ThingInput): Thing @site(resolver: "company.events/mutate-and-email")
```

To see what opts contains its best to inspect it using something like [portal](https://github.com/djblue/portal)

### itemForId {/_itemForId_/}

Gets all entities with the given id e.g:

```graphql
;; find all Comment entities which have the field cardId: "foo"
  commentsForCard(id: ID!): [Comment] @site(itemForId: "cardId")

  query commentsForCard(id: "foo") {
    ...
  }
```
