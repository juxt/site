---
title: 'Building a Kanban App API on Site'
---

Working with Site is a fast and straightforward way to build a fully functional and customisable back end that provides standardised APIs to connect to front end(s) of your choice. As Site is situated on an XTDB database, you can access [all the benefits](https://docs.xtdb.com/concepts/what-is-xtdb/) of flexible, immutable, bi-temporal data storage and manipulation within minutes just by uploading a simple GraphQL schema.

JUXT's InSite console (or indeed any preferred GraphQL visualiser) allows you to amend and test your data structures in real time, giving you the convenience and immediacy of existing no-code back end products, without their frustrating opacity or inevitable limits on customisation as your system becomes more complex.

All the most commonly used back end functionality happens in one place in Site - a GraphQL schema - and requires only a basic knowledge of GraphQL. For more complex requirements Site can integrate custom queries and external code files, and each iteration can be built on the previous with no need for system-wide refactoring.

I'm sure all of that is enough to whet your appetite, so lets crack on with a tutorial to build the back end for a simple kanban app. Hooking up a user interface is beyond the scope of this tutorial (for now...), but here is an example of a similar React based web-app that uses Site as its resource server:

<img src="/images/hiring.gif"/>

You can also [play with this kanban board](https://hire.juxt.site/) live.

In this tutorial we will be using Site's packaged InSite GraphQL console to visualise our API as it develops, but Site is designed to be compatible so feel free to use another explorer if you fancy.

The full schema for this tutorial is available [here](schema), but I would highly recommend leaving that tantalising box unopened for now in order to derive the most benefit (and understanding) from the step-by-step approach we'll take below.

## Preface

This tutorial is designed to be an entry point for exploring Site's capabilities and therefore has minimal prerequisite knowledge. However, if at any point you're finding schemas, entities, requests, and directives turning to soup in your brain, I would recommend [Johanna Antonelli's presentation](https://www.youtube.com/watch?v=PZVYVAxbzmE) for reClojure 2021 which demonstrates how to build a slightly simpler GraphQL service using Site.

We launch into our tutorial on the assumption that you already have Site running locally on your machine. Site set-up is not time consuming but it still deserves its [own instructions](/learn/installation).

Once you have the server running locally, head to [the InSite Console](https://tb-site-console.vercel.app/apis) and you are ready to begin.

If you prefer, you can [view all tutorial steps on one page](kanban-tutorials).
