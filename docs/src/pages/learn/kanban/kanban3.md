To complete the functionality of our kanban schema, we need to add additional operations and of course a Card type for recording our tasks.

- Return to the schema and create a mutation called deleteWorkflow. To delete an entity you only need to pass its ID as an argument. By default, Site mutations are ‘Create’ operations and will auto-generate a unique ID for the new document, but you can specify other types of mutation with directives - used for example in @site(mutation: "update") in updateWorkflow.
- deleteWorkflow uses the same mutation directive and should look like this:
  ```
        deleteWorkflow(id: ID!): Workflow @site(mutation: "delete")
  ```
- Repeat these steps to write deleteWorkflowState, and while you're at it you might as well get ahead and write deleteCard.

Save the schema in site/apis, refresh the console page, and you should now be able to use your delete mutations in the InSite console. (Now is an excellent time to clean up your data and remove those typoed practice entities you inevitably made earlier.)

Site responds immediately to changes in the schema with no need to stop and restart the server between updates.

To record individual tasks on the board you need to create a Card type. Use the existing types as a template and amend your schema - the Card will need to have a field for the Workflow it belongs to, connected with an @site(ref: ) directive.

**ref** and **each** are directives used to join documents - Site's [docs](../../reference/graphql/site-directive) explain further:

### ref

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

This only works for one to one relationships. For one to many use [`each`](../../reference/graphql/site-directive#each).

Using Site directives ref and each you should already have connections between Workflow <-> WorkflowState, Card -> Workflow, and Card -> WorkflowState. You can see that we don't have a reference to cards inside the workflow, instead we are going to reference cards in the workflow state.

<img src="/images/diagram.png"/>

- This diagram demonstrates the connection between different documents in a simplified way. We do not want to attach a single workflow ID to each workflowState, as a single state can be used by many different workflows. Eg a party planning board and a hiring board will both have a To Do state, but instead of creating two separate To Do WorkflowState documents, one connected to PartyPlanning and one connected to Hiring, we create a single ToDo state that is shared. This means we can query all our ToDos by looking up that single WorkflowState id, which will return all upcoming tasks regardless of the Workflow they belong to.

In order for this to be possible we need to create a field in WorkflowState that contains all the Cards currently in that state.

```graphql
type WorkflowState {
  id: ID!
  name: String!
  description: String
  workflow: Workflow! @site(ref: "workflowStateIds")
  cards: [Card] @site(ref: "workflowStateId")
}

type Card {
  id: ID!
  name: String!
  description: String!
  workflow: Workflow! @site(ref: "workflowId")
  workflowState: WorkflowState! @site(ref: "workflowStateId")
}
```

- Line 6 is essentially saying 'return all the cards that have this WorkflowState in their workflowStateId field'. An alternative would be to use:

  ```graphql
  type WorkflowState {
    ...
    cards: [Card] @site(each: "cardIds")
  }
  ```

  - However, this would mean that all the cardIds would have to be put directly into the WorkflowState when it's created, and then the state document updated after you make any changes to state within the Card documents. It makes more sense to connect the state and its cards via a query that uses up to date information already available in the card documents.

- The string value in ref refers to a field name in the create mutation for this document type:

  ```graphql
  type Mutation {
    createCard(
      id: ID
      name: String!
      description: String
      workflowId: ID!
      workflowStateId: ID!
    ): Card
  }
  ```

Save and return to InSite. Play around with your new type and operations to check that everything is running smoothly and you don't feel that any basic queries or mutations are missing.

You may find more update operations helpul while you get the hang of things.
When querying for a type, experiment with returning the details of other document types linked to yours:
<img src="/images/ss7.png"/>

- From one simple schema we are able to create and visualise a range of relationships between documents in our database.

If you are having trouble or want to double check your code, remember that the final schema for this project is available [here](schema).

So far we have queried our data using Site's default queries - return all of a type, or lookup a type by id. To build a more sophisticated picture, in the next part we will be writing (marginally) more complex graphql queries and implementing Site directives that turn these queries into XTDB legible requests ( #wording).
