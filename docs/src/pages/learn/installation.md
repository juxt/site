---
title: Installing Site
---

The following steps have been tested on an ARM/M1 Mac and various linux machines. Windows is not officially supported.

## Prerequisites {/_prerequisites_/}

Before you start, you’ll need to have the following installed:

- Java 9+ (Site is not compatible with Java 8 and below!)

- [Clojure](https://clojure.org/guides/getting_started)

- [Babashka](https://github.com/babashka/babashka/releases) (or via brew on macOS)

- [ncat](https://nmap.org/book/inst-linux.html) (already included with macOS)

And optional (but recommended):

- [rlwrap](https://github.com/hanslub42/rlwrap) (included with Clojure on MacOS)

Getting a fresh Site instance is as simple as running the following:

```bash
git clone https://github.com/juxt/site
cd site
git switch frontend-example
bin/site-dev
```

Run in a new terminal to connect a REPL for initialisation:

```bash
#can use ncat, telnet, netcat, depending on your system
rlwrap ncat localhost 50505
```

In the REPL:

```clojure Site REPL
 ;; Check src/juxt/site/alpha/repl.clj for details about
 ;; this function (and other helpers available from the repl)
(init!)
```

And that's it! To test things are up and running you can visit [here](http://localhost:5509/_site/graphql) in the browser to see the built in GraphQL schema, and POST to the same url with a GraphQL query to explore the DB.
