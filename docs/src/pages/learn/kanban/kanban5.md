At this point we have a schema that allows us to record, update, query, and view the history of, all the basic entities required to run a kanban app. We've seen how easy Site makes it to amend these structures simply by editing our graphql schema, and we have looked at some of Site's inbuilt directives that let us access the power of the underlying XTDB database with ease.

- Moving cards between columns on our kanban board in the back end is a case of updating the card documents with a new workflowStateId.
- Creating a new board or deleting unnecessary cards is handled by create and delete mutations, and if we make any mistakes we can view the entire history of our data with the @site(history) directive.
- Should you decide that you need to add fields to any types, the schemaless nature of XTDB as a database means that updating the graphql schema in site does not require a complete overhaul of your existing data.

If you have made it this far then congrats! You (should) have a ready to go back end and the required APIs for your fledgling kanban app. What to do next?

- Now that you are familiar with Site and the console, you can experiment with adding and modifying your kanban schema further, or striking out alone and creating something from scratch. If you do, please share it with us by tweeting **(TODO socials / strangeloop slack)** as JUXT would love to see what you come up with.
- Check out Alex's video [Making a Kanban App with Site](https://www.youtube.com/watch?v=L9CytxUMCaA&t=122s), where he walks through a similar project and connects it with a React front end, to add a GUI to your project.
- Take a look at JUXT's home-apps/hiring-kanban [public repo](https://github.com/juxt/home-apps/tree/main/apps/hiring-kanban) for inspiration on extending your kanban's functionality with custom Clojure code and building multiple apps on one Site instance.
