A kanban board (or Workflow) has columns (or WorkflowStates eg To Do, In Progress, Done), and Cards containing individual tasks that can be moved from one column to the next. In the schema for our app we will treat each of these entities as a **Type** and use the Site **ref** and **each** directives to define the relationships between them.

- Open the site repo in your favourite IDE and navigate to site/apis/graphql

- Create a new file called mykanban.graphql and copy into it the following:

  ```graphql
  schema @site(type: "kanban/v1") {
    query: Query
    mutation: Mutation
  }

  type Query {
    allWorkflows: [Workflow]
    workflow(id: ID!): Workflow
  }

  type Mutation {
    createWorkflowState(
      id: ID
      name: String!
      description: String
      workflowId: ID
    ): WorkflowState

    createWorkflow(
      id: ID
      name: String!
      description: String
      workflowStateIds: [ID]
    ): Workflow

    updateWorkflow(
      id: ID!
      name: String
      description: String
      workflowStateIds: [ID]
    ): Workflow @site(mutation: "update")
  }

  type Workflow {
    id: ID!
    name: String!
    description: String
    workflowStates: [WorkflowState]! @site(each: "workflowStateIds")
  }

  type WorkflowState {
    id: ID!
    name: String!
    description: String
    workflow: Workflow! @site(ref: "workflowStateIds")
  }

  #TODO
  #type Card {
  #
  #}
  ```

- Save this file and navigate back to the APIs tab in InSite - you should now be able to select mykanban as an option
  <img src="/images/ss4.png"/>

As before we can see the queries on the left, but querying for allWorkflows won't give you much back...

- To add a Workflow we need to use a mutation: select mutation from the dropdown in the lower left and hit the plus. The left panel will now show the mutations available from our uploaded schema.

<img src="/images/ss5.png"/>

- createWorkflow -> enter a name and select the name and ID to return, then run the mutation. Tip: We will need the Workflow's ID later so note it down somewhere.
- Query allWorkflows and you should see your shiny new Workflow! (Here's where you can grab that ID if you ignored the tip above...)

- Repeat these steps for createWorkflowState to create a WorkflowState named "To Do" (or whatever you fancy)
- We can use updateWorkflow to add our state to our Workflow using the IDs we noted down previously
  <img src="/images/ss6.png"/>
  <em>Note that workflowStateIds need to be in an array</em>

With that we have created, read, updated, and...ah crud, we need a way to delete.
