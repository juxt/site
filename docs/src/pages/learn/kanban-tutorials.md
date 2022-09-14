- Quick summary of Site
- Example of kanban board (embedded or a video)
- link final schema (don't look yet)

## Preface

This tutorial is designed to be an entry point for exploring Site's capabilities and therefore has minimal prerequisite knowledge. However, if at any point you're finding schemas, entities, requests, and directives turning to soup in your brain, I would recommend [Johanna Antonelli's presentation](https://www.youtube.com/watch?v=PZVYVAxbzmE) for reClojure 2021 which demonstrates how to build a slightly simpler GraphQL service using Site.
We launch into our tutorial on the assumption that you already have Site running locally on your machine. Site set-up is not time consuming but it still deserves its [own instructions](installation)
Once you have the server running locally, head to (TODO permanent link to insite apis) and you are ready to begin

## Part 1: Exploring the InSite Console

We've landed in the InSite console, and if your screen looks something like the one below (perhaps plus or minus some links) then you've arrived successfully too. If not, head back to [setup](installation) and check you've completed all the steps, then on to Troubleshooting (TODO?) if you've still got troubles to shoot.

<img src="/images/ss1.png"/>

In this tutorial you are going to be setting up a fully functional back-end for your kanban app using a local Site server instance that will generate ( #Wording host? manage?) custom APIs that can be used by your app **and** any future productivity apps you turn your hand to.
We are going to be using a GraphQL schema as the structure for our application, so go ahead and click on /\_site/graphql to check out your first ( #Wording schema? set of apis? implementation of schema? )
<img src="/images/ss2.png"/>

- On the left you should be able to see the operations used by Site itself (<em>how meta</em>)

  - Click on the >allUsers query, select id and username to return, hit play to run the query and ta-dah! You should see your own user details appear on the right.
  - Queries can also be passed arguments: click >user on the left to add it to your query and input your username in the pink quotes. Choose name to return and run the query as before. You've just queried Site for a list of all users and for a specific user's full name based on their username. At the moment your results may be short and relatively uninteresting, but in the next section we will start creating our own schema for storing, mutating, and querying data beyond your wildest (productivity-app-based) dreams...
  - <img src="/images/ss3.png"/>

## Part 2: Uploading A Schema

A kanban board (or Workflow) has columns (or WorkflowStates eg To Do, In Progress, Done), and Cards containing individual tasks that can be moved from one column to the next. In the schema for our app we will treat each of these entities as a **Type** and use the Site **ref** and **each** directives to define the relationships between them.

- Open the site repo in your favourite IDE and navigate to site/apis/graphql

  - create a new file called mykanban.graphql and copy into it the following:
  - ```graphql
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
- <img src="/images/ss4.png"/>
  - As before we can see the queries on the left, but querying for all workflows won't give you much back!
  - To add a workflow we need to use a mutation: select mutation from the dropdown in the lower left and add one. The left panel will now show the mutations available from our uploaded schema.
  - <img src="/images/ss5.png"/>
    - createWorkflow -> enter a name and return the name and ID (we will need this ID later so use the Query Variables panel to record it).
    - Now when you query allWorkflows you should see your shiny new board!
  - Repeat these steps and create a WorkflowState named "To Do" (or whatever you fancy)
  - We can use updateWorkflow to add our state to our workflow using the IDs we saved previously
  - <img src="/images/ss6.png"/>
    - <em>Note that workflowStateIds need to be in an array</em>
- With that we have created, read and updated, and...ah crud, we need a way to delete.

## Part 3: Extending the Schema

To complete the functionality of our kanban schema, we need to add additional operations and of course a Card type for recording our tasks.

- Return to the schema and create a mutation called deleteWorkflow. To delete an entity you only need to pass its ID as an argument. By default, Site mutations are ‘Create’ operations and will auto-generate a unique ID for the new document, but you can specify other types of mutation with directives - used for example in @site(mutation: "update") in updateWorkflow.
- deleteWorkflow uses the same mutation directive and should look like this:
  ```
        deleteWorkflow(id: ID!): Workflow @site(mutation: "delete")
  ```
- Repeat these steps to write deleteWorkflowState, and while you're at it you might as well get ahead and write deleteCard.
  - Save the schema in site/apis, refresh the console page, and you should now be able to use your delete mutations in the InSite console. (Now is an excellent time to clean up your data and remove those typoed practice entities you inevitably made earlier.)
  - Site responds immediately to changes in the schema with no need to stop and restart the server between updates.
- To record individual tasks on the board you need to create a Card type. Use the existing types as a template and amend your schema - the Card will need to have a field for the Workflow it belongs to, connected with an @site(ref: ) directive.

  - ref and each are directives used to join documents - Site's docs explain further (TODO permanent link for directives):
    - <img src="/images/docs_directives.png"/>
  - Using Site directives ref and each you should already have connections between Workflow <-> WorkflowState, Card -> Workflow, and Card -> WorkflowState. You can see that we don't have a reference to cards inside the workflow, instead we are going to reference cards in the workflow state.
  - <img src="/images/diagram.png"/>

    - This diagram demonstrates the connection between different documents in a simplified way. We do not want to attach a single workflow ID to each workflowState, as a single state can be used by many different workflows. Eg a party planning board and a hiring board will both have a To Do state, but instead of creating two separate To Do WorkflowState documents, one connected to PartyPlanning and one connected to Hiring, we create a single ToDo state that is shared. This means we can query all our ToDos by looking up that single WorkflowState id, which will return all upcoming tasks regardless of the workflow they belong to.
    - In order for this to be possible we need to create a field in WorkflowState that contains all the Cards currently in that state.

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

      ```
                type WorkflowState {
                  ...
                  cards: [Card] @site(each: "cardIds")
                }
      ```

      - However, this means that all the cardIds would have to be put directly into the WorkflowState when it's created, and then the state document updated after you make any changes to state within the Card document. It makes more sense to connect the state and its cards via a query using up to date information from the card documents.

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

  - Save and return to InSite. Play around with your new type and operations to check that everything is running smoothly and you don't feel that any basic queries or mutations are missing.
    - You may find more update operations helpul while you get the hang of things.
    - When querying for a type, experiment with returning the details of other document types linked to yours:
      - <img src="/images/ss7.png"/>
      - From one simple schema we are able to create and visualise a range of relationships between documents in our database.
    - If you are having trouble or want to double check your code, remember that the final schema for this project is available here (TODO link).

- So far we have queried our data using Site's default queries - return all of a type, or lookup a type by id. To build a more sophisticated picture, in the next part we will be writing (marginally) more complex graphql queries and implementing Site directives that turn these queries into XTDB legible requests ( #wording).

## Part 4: Querying the Data

- We've seen site directives for mutations and for linking database documents together, now you will implement some of the inbuilt query mutations.
- To see all the cards associated with a particular WorkflowState we can use the itemForId directive:
  ```
        type Query {
          ...
          cardsByStateId(id: ID!): [Card] @site(itemForId: "workflowStateId")
        }
  ```
  - itemForId gets all entities with the given id. Here all cards with the given workflow id will be fetched
    - <img src="/images/ss8.png"/>
    - When our query is passed the In Progress WorkflowState ID, all cards In Progress are returned regardless of their parent workflow. This enables you as the user to see all your current tasks, and is possible because our WorkflowStates exist independent of any one Workflow.
  - Feel free to add more itemForId operations to query the data in ways you find useful.
- Site is based on an XTDB database system and therefore the **entire history** of your data is available to you (including, I'm afraid, the typos you thought you deleted earlier...) Updates to documents do not destroy its previous data, so auditing the history of your data or reviewing it at a certain time in history is possible (and with Site it's easy!).

  - the history directive allows you to see all the changes to a given document. Say for example you updated the name of a card:

  - <img src="/images/ss9.png"/>
  - A list like this is all well and good but you need to know _when_ a cat walked across your keyboard and turned "Deliver Prototype" into "cknkalielmkl.nm,cne". Without a timestamp to cross reference with your home security cameras, how will you be able to tell _which_ pet is the culprit?
  - We can add the inbuilt fields \_siteCreatedAt and \_siteValidTime to our Card type to easily access the temporal power of XTDB.

    - <img src="/images/ss10.png"/>
    - By selecting \_siteCreatedAt as a return field in the query our history suddenly becomes much more useful!

    - The history directive also lets us see XTDB's flexibility as a **schemaless database**

  - Inevitably, after your personal kanban app is in daily use your team will see it and want to adopt it at work. Adding a user field to Card (or Workflow) does not break data previously entered for that type.
    - #Questions only breaks if it isnt mandatory - can you add a new mandatory field??
    - <img src="/images/ss11.png"/>
      - If 'JXT' denies any ownership of the Market Research task, Site's history directive shows you exactly when they were assigned

  <!-- - #Questions TODO valid time doesnt hold / change? These temporal fields are also useful when we need to backdate changes to our data. You log in on a Monday and realise with horror that your final completed task from Friday is still sitting in 'In Progress'! By amending your updateCard method to include valid time you can luckily backdate a change and your personal productivity streak will not be lost:  -->

  ```graphql
    updateCard(
        id: ID!
        name: String!
        description: String
        workflowId: ID
        _siteValidTime: String!
      ): Card @site(mutation: "update")

  ```

- Querying at a certain point in the past TODO
- More complex queries with q directive TODO
- Static example of resolver TODO

## Part 5: Summary & Next Steps...

At this point we have a schema that allows us to record, update, query, and view the history of, all the basic entities required to run a kanban app. We've seen how easy Site makes it to amend these structures simply by editing our graphql schema, and we have looked at some of Site's inbuilt directives that let us access the power of the underlying XTDB database with ease.

- Moving cards between columns on our kanban board in the back end is a case of updating the card documents with a new workflowStateId.
- Creating a new board or deleting unnecessary cards is handled by create and delete mutations, and if we make any mistakes we can view the entire history of our data with @site(history) directive.
- Should you decide that you need to add fields to any types, the schemaless nature of XTDB as a database means that updating the graphql schema in site does not require a complete overhaul of your existing data.

If you have made it this far then congrats! You (should) have a ready to go back end and the required APIs for your fledgling kanban app. What to do next?

- Now that you are familiar with Site and the console, you can experiment with adding and modifying your kanban schema further, or striking out alone and creating something from scratch. If you do, please share it with us by tweeting (TODO socials / strangeloop slack) as Juxt would love to see what you come up with.
- Check out Alex's video (TODO link), where he walks through a similar kanban built on Site and connects it with a React front end, to add a GUI to your project.
- Take a look at Juxt's home-apps/hiring-kanban repo (TODO link to github) for inspiration on extending your kanban's functionality with custom Clojure code and building multiple apps on one Site instance.
