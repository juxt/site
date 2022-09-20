A kanban board (or Workflow) has columns (or WorkflowStates eg To Do, In Progress, Done), and Cards containing individual tasks that can be moved from one column to the next. In the schema for our app we will treat each of these entities as a **Type** and use the Site **ref** and **each** directives to define the relationships between them.

- Open the site repo in your favourite IDE and navigate to site/apis/graphql

  - create a new file called mykanban.graphql and copy into it the following:

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

- To add a Workflow we need to use a mutation: select mutation from the dropdown in the lower left and add one. The left panel will now show the mutations available from our uploaded schema.

<img src="/images/ss5.png"/>

- createWorkflow -> enter a name and return the name and ID (we will need this ID later so use the Query Variables panel to record it).
- Query allWorkflows you should see your shiny new Workflow!

- Repeat these steps and create a WorkflowState named "To Do" (or whatever you fancy)
- We can use updateWorkflow to add our state to our Workflow using the IDs we saved previously
  <img src="/images/ss6.png"/>
  <em>Note that workflowStateIds need to be in an array</em>

With that we have created, read and updated, and...ah crud, we need a way to delete.