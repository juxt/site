# juxt-site Specification

A resource-oriented HTTP web application framework with comprehensive authentication, authorization, content negotiation, and templating capabilities.

## Project Scope

juxt-site is a system for serving HTTP requests through a resource-based REST architecture. Key characteristics:

- **Resource-centric**: All data (resources, users, rules, configurations) stored as entities in an immutable, time-versioned database (XTDB)
- **HTTP standards compliant**: Full support for HTTP semantics including conditional requests (ETags, If-Match), content negotiation, CORS, and various HTTP methods
- **Declarative configuration**: Resources, authorization rules, and triggers declared as data in the database rather than code
- **Template-driven responses**: Support for Selmer templates and GraphQL for dynamic content generation
- **Event-driven**: Trigger system for executing actions in response to requests
- **Authentication & Authorization**: Subject-based authentication with rule-driven authorization policies
- **Extensible**: Custom functions for POST/PUT/PATCH handlers, template models, and resource locators

## Specification Files

- **core_domain.allium** - Core entities, value objects, and domain rules
- **data_model.allium** - XTDB data model schema and invariants
- **api_behaviour.allium** - HTTP request/response behavior, validation rules
- **data_flows.allium** - End-to-end request processing flows
- **error_handling.allium** - Error conditions, status codes, failure modes
- **external_contracts.allium** - Expectations on external systems (XTDB, Ring, dependencies)
