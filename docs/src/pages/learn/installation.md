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

Getting a fresh Site instance is as simple as running the following:

<PackageImport>

```bash
git clone https://github.com/juxt/site
mkdir -p $HOME/.config/site
cp site/etc/config.edn $HOME/.config/site/config.edn
site/bin/site-server
```

```bash
rlwrap nc localhost 50505
```

```clojure Site REPL
(put-site-api!)
(put-auth-resources!)
(put-superuser-role!)
(put-superuser! "admin" "Administrator" "admin")
```

</PackageImport>

## Site Modules {/_exports_/}

Site contains several modules in the `opt` directory. These modules are used to provide the following functionality:

- debug-request - an html template that can display [Site debug pages](/reference/debug-request)
- graphiql - an html template that can display [GraphiQL](/reference/graphiql)
- graphql - a module that provides GraphQL functionality
- login-form - a basic login page and API for authenticating users
- swagger-ui - an html template that can display [Swagger UI](/reference/swagger-ui)

To install any of these modules, look in the README file inside the modules directory. A better way to install these is planned for a future release.gg
