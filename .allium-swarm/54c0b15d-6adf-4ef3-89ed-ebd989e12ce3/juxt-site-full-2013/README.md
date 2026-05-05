# juxt-site-full-2013 Specification

## Scope

juxt-site-full-2013 is a comprehensive HTTP server framework built on Clojure that provides a flexible, permission-based system for:

- Storing and retrieving HTTP resources using XTDB (a bitemporal database)
- Handling HTTP requests with full RFC 7232 conditional request support
- Managing user authentication (password-based, OAuth2, OpenID Connect) and role-based authorization
- Supporting multiple content representations through content negotiation
- Templating and GraphQL query support
- Rules-based policy evaluation for access control
- Trigger-based actions in response to data changes

The system is built on **Integrant** for dependency injection, **Ring/Jetty** for HTTP handling, **XTDB** for data persistence, and various libraries for content negotiation, templating, and GraphQL support.

## Index

- **core_domain.allium** — Core entities (resources, rules, users, roles, sessions), business rules, and value objects
- **data_model.allium** — Persistent data model structures stored in XTDB and their invariants
- **api_behaviour.allium** — HTTP request/response handling, content negotiation, status codes, and validation
- **data_flows.allium** — End-to-end request flows through authentication, authorization, and response generation
- **error_handling.allium** — Error conditions, failure modes, and recovery strategies
- **external_contracts.allium** — Dependencies and external system contracts
