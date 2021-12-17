---
id: home
title: Site - A new way to build websites
permalink: index.html
---

<HomepageHero />

## What is this site?

Site is a Resource Server, built on the XTDB database.

You can put things into Site with (HTTP) PUT requests. When you do this, Site will put (the representation of) your thing (document, image, video, data…) into the database. You can get these later with a (HTTP) GET request with the same URI. In this way, Site behaves like a web server, with an immutable bitemporal content store.

## APIs

If you PUT a JSON document with a `Content-Type` of
`application/vnd.oai.openapi+json;version=3.0.2`, Site will treat this as an
[OpenAPI](https://www.openapis.org/) API definition, and serve that API for
you. This OpenAPI API definition will contain the API endpoints, and provide
schemas for the data transferred by the API. This tells the server how to
validate data coming in to the API, and how to construct data on the way out.

APIs served from Site are good web citizens. They implement HTTP method
semantics properly, with support for content negotiation, conditional requests,
range requests and authentication.

APIs are also able to benefit from Site's authorization module, Pass, providing
Policy-Based Access Control, loosely based on XACML.
