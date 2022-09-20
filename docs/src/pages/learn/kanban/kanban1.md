We've landed in the InSite console, and if your screen looks something like the one below (perhaps plus or minus some links) then you've arrived successfully too. If not, head back to [setup](../installation) and check you've completed all the steps, then on to Troubleshooting (**TODO?**) if you've still got troubles to shoot.

<img src="/images/ss1.png"/>

In this tutorial you are going to be setting up a fully functional back-end for your kanban app using a local Site server instance that will generate ( #Wording host? manage?) custom APIs that can be used by your app **and** any future productivity apps you turn your hand to.

We are going to be using a GraphQL schema as the structure for our application, so go ahead and click on /\_site/graphql to check out your first ( #Wording schema? set of apis? implementation of schema? )
<img src="/images/ss2.png"/>

On the left you should be able to see the operations used by Site itself (<em>how meta</em>)
<img src="/images/ss3.png"/>

- Click on the >allUsers query, select id and username to return, hit play to run the query and ta-dah! You should see your own user details appear on the right.
- Queries can also be passed arguments: click >user on the left to add it to your query and input your username in the pink quotes. Choose name to return and run the query as before. You've just queried Site for a list of all users and for a specific user's full name based on their username. At the moment your results may be short and relatively uninteresting, but in the next section we will start creating our own schema for storing, mutating, and querying data beyond your wildest (productivity-app-based) dreams...