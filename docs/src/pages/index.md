---
id: home
title: Site - A new way to build websites
permalink: index.html
---

<HomepageHero />

<Intro>

[JUXT's Approach to software](https://www.juxt.pro/) details four primary concerns: **Time**, **Data**, **Form** and **Code**.

[XTDB](https://xtdb.com/) provides **Data** management, independent of any particular application or use-case, built upon a common model of **Time**.

[Site](https://github.com/juxt/site) is a Resource Server, built on the XTDB database. It is a tool for adding **Form** to data, providing structure to application clients, relevent to a particular use-case.

The server behaviour is defined in a declarative manner using open-source formats for describing and documenting APIs.

API definitions are sent to Site via HTTP and re-read every request meaning changes are 'deployed' instantly.

API definitions are stored in XTDB which means a full audit history of changes is maintained with no extra code.

As XTDB has an open data model many APIs can coexist within a single system allowing efficent federation of data sources.

Site currently provides execution engines for [Open Api](https://spec.openapis.org/oas/v3.0.2) and [Graphql](https://graphql.org/)

APIs are also able to benefit from Site’s authorization module, [Pass](), providing Policy-Based Access Control, loosely based on XACML.

</Intro>

## Open API

The OpenAPI Specification (OAS) defines a standard, programming language-agnostic interface description for HTTP APIs.

If you PUT a JSON document with a Content-Type of `application/vnd.oai.openapi+json;version=3.0.2`, Site will treat this as an OpenAPI API definition, and serve that API for you. This OpenAPI API definition will contain the API endpoints, and provide schemas for the data transferred by the API. This tells the server how to validate data coming in to the API, and how to construct data on the way out.

APIs served from Site are good web citizens. They implement HTTP method semantics properly, with support for content negotiation, conditional requests, range requests and authentication.

## Graphql

All that is needed to create a fully functioning GraphQL server is to create a schema.
A single graphQL compliant schema file provides Site with all the data it needs to expose a GraphQL endpoint, transct or query data with XTDB and validate the incoming and outgoing data.