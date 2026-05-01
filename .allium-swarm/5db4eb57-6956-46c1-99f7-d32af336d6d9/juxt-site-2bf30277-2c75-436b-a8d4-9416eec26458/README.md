# Site 1.0 - Allium Specification

## Overview

Site is a Resource Server built on XTDB, a bitemporal database. It provides immutable, versioned storage of web content and APIs. Site implements HTTP semantics properly with support for content negotiation, conditional requests, and provides policy-based access control through its Pass authorization module.

### Key Capabilities

- **Resource Storage**: Store documents, images, data via HTTP PUT/POST requests
- **HTTP APIs**: Publish OpenAPI definitions; Site serves those APIs with automatic validation
- **Content Management**: Versioned, bitemporal storage of all content representations
- **Authentication & Authorization**: User authentication and policy-based access control (PBAC)
- **Content Negotiation**: Automatic selection of representation based on client preferences
- **Conditional Requests**: Support for If-Match, If-None-Match, If-Modified-Since, If-Unmodified-Since headers

## Domain Structure

### Core Concepts
- **Resources**: Target URIs that can have multiple representations
- **Representations**: Concrete variants of a resource (e.g., JSON, XML, HTML)
- **Users & Roles**: Authentication identity and authorization grouping
- **Rules & Triggers**: Authorization policies and event-driven actions
- **OpenAPI Definitions**: API schemas that drive request/response validation

## Specification Files

- **core_domain.allium** - Core domain entities, value objects, and business rules
- **data_model.allium** - XTDB persistent data model and invariants
- **api_behaviour.allium** - HTTP API contracts, validation, and error responses
- **data_flows.allium** - End-to-end request processing flows
- **error_handling.allium** - Error conditions, failure modes, and recovery
- **external_contracts.allium** - Expectations on external dependencies (XTDB, authentication)

