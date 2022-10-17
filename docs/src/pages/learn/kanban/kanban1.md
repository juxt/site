We've landed in the InSite console, and if your screen looks something like the one below (perhaps plus or minus some links) then you've arrived successfully too. If not, head back to [setup](../installation) and check you've completed all the steps. Still stuck? Check out JUXT's [Building Site](https://www.youtube.com/playlist?list=PLrCB9bq0iVIoCCV7SGJH1bXTrDfp2mP4i) YouTube playlist, or Tweet us [@juxtpro](https://twitter.com/juxtpro).

<img src="/images/ss1.png"/>

In this tutorial you are going to be setting up a fully functional back-end for your kanban app using a local Site server instance that will generate custom APIs that can be used by your app **and** any future productivity apps you turn your hand to.

We are going to be using a GraphQL schema as the structure for our application, so go ahead and click on /\_site/graphql to check out your first API.
<img src="/images/ss2.png"/>

On the left you should be able to see the operations used by Site itself (<em>how meta</em>)
<img src="/images/ss3.1.png"/>

- Click on the >apis query, select type and id to return, hit play to run the query and ta-dah! You should see the same APIs listed that were visible on the landing page.
- Queries can also be passed arguments: click >request on the left to add it to your query and you can see where you will be able to input a request ID to return its details. To find an ID to test, run the requests query returning summaries -> id. However, you might not get any IDs returned yet as your Site instance is fresh as a daisy.

At the moment your query results may be short and relatively uninteresting, but in the next section we will start creating our own schema for storing, mutating, and querying data beyond your wildest (productivity-app-based) dreams...