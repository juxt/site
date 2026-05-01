# juxt-site Allium Specifications

A policy-driven HTTP application framework with dynamic resource management, rule-based authorization, and integrated GraphQL support.

## Project Overview

**juxt-site** is a declarative HTTP server that stores its entire configuration (resources, authorization rules, representations, users) in a distributed temporal database (XTDB). Rather than hardcoding request handlers, the framework evaluates dynamic resources and rules against each request, enabling runtime reconfiguration without restarts.

### Key Characteristics

- **Dynamic Resources**: All resources are persisted entities with metadata (content type, methods, acceptable formats)
- **Policy-Driven Authorization**: Rules are stored as data; authorization via datalog queries against request context
- **Representation Negotiation**: Content negotiation with Vary header support and dynamic variants
- **Temporal Persistence**: XTDB provides bitemporal querying and audit trails
- **Integrated GraphQL**: First-class GraphQL query support with schema validation
- **WebDAV Support**: PROPFIND, MKCOL methods for collection management
- **Event-Driven Actions**: Triggers fire post-request based on query evaluation
- **Ring Middleware**: Ring 2.0 adapter with 30+ middleware stages

## Specification Files

| File | Domain |
|------|--------|
| `core_domain.allium` | Entities, value objects, business rules |
| `data_model.allium` | Persistent data model and invariants |
| `api_behaviour.allium` | HTTP request/response behavior, content negotiation |
| `data_flows.allium` | End-to-end request flows and processing pipelines |
| `error_handling.allium` | Error conditions, failure modes, recovery strategies |
| `external_contracts.allium` | Expectations on external systems (XTDB, Ring, Jetty) |

## System Boundaries

**In-Scope**: The HTTP request handler, authorization engine, resource location, content negotiation, and response generation. All observable behavior of the framework when processing HTTP requests.

**Out-of-Scope**: 
- XTDB internals (treated as a black-box database)
- Ring/Jetty adapter internals (treated as HTTP abstraction)
- Specific GraphQL execution (covered at framework integration level only)
- Template engine internals (Selmer)

## Architecture at a Glance

```
HTTP Request
    ↓
[Ring 1→2 Adapter] → [Health Check] → [Initialize Request]
    ↓
[CORS/Security Headers] → [Error Handler] → [Method Validation]
    ↓
[Locate Resource] → [Check Redirects] → [Find Representations]
    ↓
[Content Negotiation] → [Authentication] → [Authorization (PDP)]
    ↓
[Method Check] → [Invoke Method Handler] → [Triggers]
    ↓
[Format Response] → [Cache/Store Request] → Ring 2 Response
```

The request carries state through all middleware as a single map (`req`), accumulating contextual information at each stage.
