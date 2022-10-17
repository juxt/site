We've seen Site directives for mutations and for linking database documents together, now you will implement some of the inbuilt query mutations.

To see all the cards associated with a particular WorkflowState we can use the **itemForId** directive, which gets all entities of the return type that reference the given id. Here, all cards with a certain workflowId will be fetched:

```
      type Query {
        ...
        cardsByStateId(id: ID!): [Card] @site(itemForId: "workflowStateId")
      }
```

When our query is passed the "In Progress" WorkflowState ID, all cards currently In Progress are returned regardless of their parent workflow:

<img src="/images/ss8.png"/>

- This enables you as the user to see all your current tasks, and is possible because our WorkflowStates exist independent of any one Workflow.

Feel free to add more itemForId operations to query the data in ways you find useful.

Site is based on an XTDB database system and therefore the **entire history** of your data is available to you (including, I'm afraid, the typos you thought you deleted earlier...) Updates to a document do not destroy its previous data, so auditing the history of your data or reviewing it at a certain point in time is possible (and with Site it's easy!)

The [**history**](../../reference/graphql/site-directive#history) directive allows you to see all the changes to a given document. Say for example you updated the name of a card at some point, you can add the query:

```graphql
cardHistory(id: ID!): [Card] @site(history: "desc")
```

When this query is run, Site returns a list of Cards which represent the document at every iteration:

  <img src="/images/ss9.png"/>

A list like this is all well and good, but you need to know _when_ a cat walked across your keyboard and turned "Deliver Prototype" into "cknkalielmkl.nm,cne". Without a timestamp to cross reference with your home security cameras, how will you be able to tell _which_ pet is the culprit?

We can add the fields `_siteCreatedAt: String` and `_siteValidTime: String` to our Card type to easily access the temporal power of XTDB via Site's inbuilt \_site fields.

<img src="/images/ss10.png"/>

- By selecting \_siteCreatedAt as a return field in the query our history suddenly becomes much more useful!

The history directive also helps us see XTDB's flexibility as a [**schemaless database**](https://docs.xtdb.com/concepts/what-is-xtdb/#schemaless). Inevitably, after you've adopted your kanban app as your one source of truth for your To-Dos, your team will see it and want to use it at work. Adding a user field to Card (or Workflow) does not break data previously entered for that type.

<img src="/images/ss11.png"/>

- If 'JXT' denies any ownership of the Market Research task, Site's history directive shows you exactly when they were assigned

As well as Site's inbuilt query directives, you have the flexibility to write custom queries. Site's q directive can take a GraphQL object, or an edn string - three double quotes in GraphQL represents a multiline string. This can be more convenient for copying and pasting queries from the repl for example.

This example returns all the cards in a given workflow when searched for by **name**, as opposed to ID, which is a more likely direct interaction a user might have with the kanban app.

For a more in-depth tutorial on queries in XTDB check out [XTDB Datalog Queries](https://docs.xtdb.com/language-reference/1.21.0/datalog-queries/), but in brief:

<APIAnatomy title="q.directive">

<AnatomyStep title="">

We want to find an entity e...

</AnatomyStep>
<AnatomyStep >

...and the [Card] return type tells Site what to expect e to be.

</AnatomyStep>
<AnatomyStep >

First we need to find an entity of type Workflow, which has the name we passed to the query as an argument.

</AnatomyStep>
<AnatomyStep>

Once we have found this workflow w, we look for an entity (Card) that references w in its workflowId.
</AnatomyStep>

```graphql [[6,7], [2,3], [7,9], [9,10]]
type Query {
  cardsByWorkflowName(name: String!): [Card]
    @site(
      q: {
        edn: """
        {:find [e]
          :where [[w {{type-k}} "Workflow"]
                  [w :name "{{args.name}}"]
                [e :workflowId w]]}
        """
      }
    )
}
```

</APIAnatomy>

All cards that satisfy e will be returned as a list.

  <img src="/images/ss12.png"/>
