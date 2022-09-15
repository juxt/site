```graphql
schema @site(type: "kanban/v1") {
  query: Query
  mutation: Mutation
}

type Query {
  allWorkflows: [Workflow]
  workflow(id: ID!): Workflow
  allWorkflowStates: [WorkflowState]
  workflowState(id: ID!): WorkflowState
  allCards: [Card]
  card(id: ID!): Card
  cardsByStateId(id: ID!): [Card] @site(itemForId: "workflowStateId")
  cardHistory(id: ID!): [Card] @site(history: "desc")
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

  createCard(
    id: ID
    name: String!
    description: String
    workflowId: ID!
    workflowStateId: ID!
    user: String
  ): Card

  updateWorkflow(
    id: ID!
    name: String
    description: String
    workflowStateIds: [ID]
  ): Workflow @site(mutation: "update")

  updateCard(
    id: ID!
    name: String
    description: String
    workflowId: ID
    workflowStateId: ID
    _siteValidTime: String
    user: String
  ): Card @site(mutation: "update")

  deleteWorkflow(id: ID!): Workflow @site(mutation: "delete")
  deleteWorkflowState(id: ID!): WorkflowState @site(mutation: "delete")
  deleteCard(id: ID!): Card @site(mutation: "delete")
}

type Workflow {
  id: ID!
  name: String!
  description: String
  workflowStates: [WorkflowState] @site(each: "workflowStateIds")
}

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
  _siteValidTime: String!
  _siteCreatedAt: String!
  user: String
}
```
