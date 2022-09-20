---
title: Installing Site
---

<Intro>

Currently installation is a little long winded, we hope to improve this in the near future.

</Intro>

## Prerequisites {/_prerequisites_/}

Before you start, you’ll need to have the following installed:

- Java 9+ (Site is not compatible with Java 8 and below!)

- [Clojure](https://clojure.org/guides/getting_started)

- [Babashka](https://github.com/babashka/babashka/releases) (or via brew on macOS)

- [ncat](https://nmap.org/book/inst-linux.html) (Linux users only, or can use `telnet` instead)

And optional (but recommended):

- [rlwrap](https://github.com/hanslub42/rlwrap) (included with Clojure on MacOS)
- [node](https://nodejs.dev/download/package-manager/#nvm) (Needed to build the graphiql console browser. LTS version is recommended - `nvm use --lts`)

Getting a fresh Site instance is as simple as running the following:

```bash
git clone https://github.com/juxt/site
mkdir -p $HOME/.config/site
cp site/etc/config.edn $HOME/.config/site/config.edn
cd site
git switch frontend-example
site/bin/site-dev
```

Run in a new terminal to connect a REPL for initialisation:

```bash
#can use ncat, telnet, netcat, depending on your system
rlwrap ncat localhost 50505
```

In the REPL:

```clojure Site REPL
 ;; init! takes a username and password and creates
 ;; a superuser using those credentials as well as
 ;; initialising the rest of Site's core modules.
 ;; Check src/juxt/site/alpha/repl.clj for details
(init!)
```

## Optional Site Modules {/_exports_/}

Site contains several modules in the `opt` directory. These modules are used to provide the following functionality:

- graphiql - an html template that can display [GraphiQL](/reference/graphiql). You will need node (version <17) installed to build this.
- swagger-ui - an html template that can display [Swagger UI](/reference/swagger-ui)
- insite-console - A Dashboard containing both the swagger and graphiql dashboards, as well as a tool for viewing a table of all requests (including error stack traces, response latency and more)

To install any of these modules, look in the README file inside the modules directory. A better way to install these is planned for a future release.
