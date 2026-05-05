# juxt-site-full-2013 System Summary

## What this system does

**juxt-site-full-2013** is a comprehensive HTTP server framework built on Clojure that provides a flexible, declarative system for serving resources with strong content negotiation, conditional request handling, authentication, and authorization. It uses XTDB (a bitemporal database) as the primary data store and Ring/Jetty for HTTP handling.

The system's core responsibility is to:
- Store and serve HTTP resources with full RFC 7232 conditional request support (If-Match, If-None-Match, If-Modified-Since, If-Unmodified-Since)
- Authenticate users via password-based login and OAuth2/OpenID Connect
- Enforce access control through rules-based authorization policies
- Negotiate content representations based on Accept headers (media-type, charset, language, encoding)
- Support dynamic content generation via templating and GraphQL
- Trigger side-effect actions when data conditions are met

## Major components

### 1. **Core Domain Model** (`core_domain.allium`)
- **Resource**: HTTP resource stored in database with URI, HTTP methods, and handler functions
- **Representation**: Content variant of a resource (specific content-type, language, encoding) with caching metadata (ETag, Last-Modified)
- **User**: Authenticated subject with optional classification levels
- **Password**: Bcrypt-hashed credential for user login
- **Role**: Named permission group assignable to users
- **UserRoleMapping**: Assignment of user to role
- **Rule**: Authorization policy (Datalog query + allow/deny effect)
- **Trigger**: Event-driven action fired when Datalog conditions match
- **ResourceLocator**: Dynamic URL pattern matching for resource lookup
- **Session**: Time-bounded authentication session with bearer token

### 2. **Data Model & Storage** (`data_model.allium`)
- XTDB bitemporal immutable database with full ACID guarantees
- Structured storage for all entities with invariants (uniqueness, foreign keys, format validation)
- Transactions for atomic writes and speculative evaluation
- Authorization checks via temporary in-memory database states
- Session management through in-memory token store with expiry

### 3. **HTTP API Contracts** (`api_behaviour.allium`)
- **GET/HEAD**: Retrieve representations with full precondition support
- **PUT**: Create/replace resources with content negotiation validation
- **POST**: Process data via resource-defined handler functions
- **DELETE**: Remove resources
- **Content Negotiation**: juxt/pick-based media-type rating with Vary headers
- **Authorization**: Default-deny rules evaluation
- **Status Codes**: Comprehensive HTTP status mapping (200, 201, 204, 304, 400, 401, 403, 404, 406, 411, 412, 413, 415, 500)

### 4. **Data Flows** (`data_flows.allium`)
- **Inbound HTTP Request**: Entry point with resource location, session lookup, authorization
- **HTTP GET/HEAD Processing**: Precondition evaluation, representation selection, content negotiation, response building
- **HTTP PUT Processing**: Payload validation, content-type/charset/encoding verification, handler invocation, database write
- **HTTP POST Processing**: Similar to PUT but handler-determined response
- **HTTP DELETE Processing**: Authorization check and resource removal
- **Authentication Flow**: Credential parsing, user lookup, bcrypt verification, session token generation (24 random bytes, base64-encoded)
- **Authorization Rule Evaluation**: Speculative database with request context, Datalog query matching, allow/deny decision
- **Trigger Action Execution**: Datalog-based trigger matching with side-effect dispatching
- **Session Expiry**: In-memory cleanup of expired sessions

### 5. **Error Handling** (`error_handling.allium`)
30+ error conditions covering:
- **Precondition failures** (412): If-Match, If-Unmodified-Since, If-None-Match evaluation
- **Conditional responses** (304): Not Modified via If-None-Match / If-Modified-Since
- **Input validation** (400, 411, 413, 415): Content-Length, payload size, content-type, charset
- **Authentication/Authorization** (401, 403): Unauthorized access, forbidden operations
- **Not Found** (404): Resource or representation missing
- **Not Acceptable** (406): No representation matches Accept constraints
- **Server errors** (500): Unresolvable handler symbols, template failures, database errors

Recovery strategies: retry-with-backoff, client-correction, immediate rejection, configuration fixes, graceful degradation

### 6. **External Dependencies** (`external_contracts.allium`)
- **XTDB**: Bitemporal database with Datalog querying and transaction management
- **Ring/Jetty**: HTTP server abstraction and request/response handling
- **Aero**: Configuration file reading with profile and environment support
- **Integrant**: Dependency injection and system lifecycle
- **juxt/pick**: Content negotiation with RFC 7231 media-type matching
- **juxt/grab**: GraphQL schema validation and execution
- **juxt/reap**: RFC 7230/7231/7232/7235 HTTP header parsing
- **Malli**: Schema validation for payloads and GraphQL
- **Crypto-Password (bcrypt)**: Timing-safe password hashing and verification
- **Selmer**: Jinja2-like template rendering with custom XTDB template loader
- **java-jwt + jwks-rsa**: OpenID Connect token verification
- **Selmer extensions, Hiccup, json-html, clj-yaml, Jsonista**: Output formatting

External systems: OpenID Connect provider, optional email service, RocksDB-based file storage

## Open questions

- **Session persistence**: Sessions stored in in-memory atom—does this survive server restart? Is there a disk-backed fallback?
- **Post/Put handlers**: Spec shows framework for custom handlers but doesn't detail what handlers are actually implemented
- **OpenID Connect flow**: Integration is declared but detailed flow (authorization code exchange, JWK verification) not fully specified
- **Trigger action implementations**: Framework mentions multimethod dispatch on action types but concrete actions not enumerated
- **Performance constraints**: No mention of throughput, latency expectations, or scaling limits
- **GraphQL implementation status**: Contract exists but unclear if full GraphQL query language is supported or subset
- **Acceptable-on-put configuration**: How are acceptable content-types, charsets, encodings configured on resources? Inline in resource definition or lookup table?
- **Concurrent request handling**: Atomicity model for concurrent rule evaluation, session updates, database transactions unclear
- **Template filter/tag ecosystem**: What custom Selmer filters/tags are provided beyond standard library?

## Areas of uncertainty

- **Session storage atomicity**: In-memory sessions-by-access-token uses `atom swap` for expiry but concurrent access patterns during expiry not detailed
- **Speculative database overhead**: Authorization via temporary entities with random IDs—performance implications of db/with-tx per request not addressed
- **Rule scope isolation**: Rules evaluate against temporary context but can they query real database state? Unclear if rules have access to full historical bitemporal data
- **Content negotiation edge cases**: Spec requires charset declaration for text/* types but recovery path for missing charset not fully specified (415 vs 400)
- **ETag generation strategy**: Last-Modified and ETag both required for cacheability but mechanism for generating ETags not detailed (hash-based? timestamp?)
- **Classification enforcement**: Resources and users have optional classification field but enforcement (PUBLIC, RESTRICTED) rules only partially specified
- **Representation variant selection**: When multiple representations exist, conflict resolution via pick/qvalue is clear, but tie-breaking not specified
- **Error logging context**: Error handling specifies "log error" but log level, context format, correlation with request-id not detailed
- **Resource deletion semantics**: DELETE removes resource but interaction with existing representations unclear—cascade delete or orphan?
- **Trigger side effects**: Trigger execution doesn't block request—if trigger fails, is the originating request still successful?
