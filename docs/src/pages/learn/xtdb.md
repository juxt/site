---
title: XTDB Primer
---


Looking at these example documents from the [fixtures](fixtures) you may notice a few things:

```clojure
(put! {:person/name "Arnold Schwarzenegger",
       :person/born #inst "1947-07-30T00:00:00.000-00:00",
       :juxt.site/type "Person"
       :xt/id "http://localhost:2021/movies/-101"}

      {:person/name "Joanne Samuel", :xt/id "http://localhost:2021/movies/-144"
       :juxt.site/type "Person"}

      {:movie/title "The Terminator",
       :movie/year 1984,
       :movie/director "http://localhost:2021/movies/-100"
       :movie/cast ["http://localhost:2021/movies/-101"],
       :movie/sequel "http://localhost:2021/movies/-207",
       :xt/id "http://localhost:2021/movies/-200"
       :juxt.site/type "Movie"}

      {:movie/title "Terminator 2: Judgment Day",
       :movie/year 1991,
       :movie/director "http://localhost:2021/movies/-100"
       :movie/cast ["http://localhost:2021/movies/-101"],
       :xt/id "http://localhost:2021/movies/-207"
       :juxt.site/type "Movie"})
```

- Every document has a key `:xt/id`
- Joanne's record does not have a `:person/born` attribute
- Arnold Schwarzenegger's `:xt/id` appears the Terminator `:movie/cast` list
- The movies are links by `:movie/sequel`

XTDB is schemaless and does not have tables.

The only required field is `:xt/id` that acts as a unique identifier for each document.

This flexibility allows data to evolve and outlive the applications we build on top of it.
In an open system like this data can be unified easily, and simply ignored when not pertinent.
Like the web, the documents can be linked in a graph and queried in novel ways.

Documents can have any attribute, XTDB won't stop you putting `:movie/year` and `:person/name` together.
