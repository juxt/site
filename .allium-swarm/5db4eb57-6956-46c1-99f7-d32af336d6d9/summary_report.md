# Site 1.0 System Summary

## What This System Does

Site is a bitemporal Resource Server built on XTDB that provides HTTP-based content and API management. It stores versioned, immutable representations of resources (documents, images, data) and serves them with proper HTTP semantics including content negotiation, conditional requests, and policy-based access control. The system can publish OpenAPI definitions and serve APIs with automatic request/response validation.

**Core Value**: Immutable, versioned storage of web content with bitemporal history and fine-grained authorization policies.

## Major Components

### Data Layer
- **XTDB Database**: Schemaless, bitemporal storage with valid-time and transaction-time dimensions. All entities identified by `:xt/id` (URI). Supports atomic transactions, Datalog queries, and speculative evaluation.
- **Persistent Entities**: Resources (URIs), Representations (content variants with media type/encoding), Users, Passwords (bcrypt-encrypted), Roles, Rules (authorization policies), OpenAPI definitions, Triggers (event-driven actions), Templates, and Redirects.

### HTTP API Layer
- **Request Handling**: Ring/Jetty framework processes HTTP requests and routes to handlers. All standard HTTP methods supported: GET, HEAD, PUT, POST, DELETE, PATCH, OPTIONS, plus WebDAV methods (MKCOL, PROPFIND).
- **Content Negotiation**: Automatic representation selection based on Accept, Accept-Encoding, Accept-Language, Accept-Charset headers. Falls back to defaults if no match.
- **Conditional Requests**: RFC 7232 support for If-Match, If-None-Match, If-Modified-Since, If-Unmodified-Since with ETags and last-modified timestamps.
- **Request Validation**: Content-Type and Content-Length required for PUT/POST. Payload size limits (default 16MB). Charset validation for text content.

### Security & Authorization
- **Authentication**: Supports Bearer tokens, Basic auth, and session cookies. Credentials extracted from Authorization header or cookies.
- **Password Storage**: Bcrypt-encrypted hashes with configurable cost factor.
- **Policy-Based Access Control (PBAC)**: Rules evaluated as Datalog patterns against request context (subject, resource, request, representation, environment). First matching allow/deny rule determines access; default-deny.

### Response Generation
- **Content Sources**: Representation body stored as binary or text, dynamic generation via body-fn, or template rendering.
- **Template Engine**: Selmer template dialect with variable substitution, conditionals, iteration, and custom filters. Templates loaded from XTDB.
- **Response Headers**: Standard HTTP metadata (Content-Length, Content-Type, ETag, Last-Modified, Vary, Cache-Control) included automatically.

### Advanced Features
- **OpenAPI Integration**: Publish OpenAPI schemas; Site validates requests/responses against declared schemas.
- **Resource Locators**: Pattern-based regex matching for dynamic URI patterns with custom locator functions.
- **Triggers**: Event-driven actions fired when Datalog query conditions match; can perform side effects.
- **Redirects**: HTTP redirects from one URI to another with configurable status codes.

## Open Questions

1. **OpenAPI Request/Response Validation**: Specification describes OpenAPI validation flow but doesn't detail what happens when validation fails — is the error response schema-aware or generic?

2. **Trigger Execution Model**: Triggers execute after step 11 in the main request flow. Is execution synchronous or asynchronous? Are multiple matched triggers executed in sequence or parallel? How are errors handled?

3. **Resource Locator Precedence**: When multiple locators match the same URI, how is the winner selected? Is it first-match, most-specific pattern, or an error?

4. **POST Handler Return Format**: POST can invoke custom post-fn. Does the handler return a full Ring response or a partial response that gets merged with defaults?

5. **Representation Variants Strategy**: The specification mentions "variants" but doesn't clearly define when/how variants differ from independent representations. Is it a content-negotiation optimization or a business concept?

6. **Bitemporal Query Semantics**: Can end users query resources at arbitrary valid-times, or is this an internal capability? How does time-travel affect authorization evaluation?

7. **Session Management**: Cookies are supported for authentication, but session lifecycle (creation, invalidation, expiration) is not detailed.

8. **GraphQL Integration**: Specification mentions GraphQL schema and query execution but provides no integration details with Site's core request flow.

## Areas of Uncertainty

### Authorization Context Building
The specification states that authorization context is built by dissociating resource and representation bodies before rule evaluation ("dissoc resource :body :content"). Why is this necessary? Are rules pattern-matching on content, or is this a security measure to prevent leaked metadata?

### Concurrent Representation Updates
XTDB transactions are atomic, but the data model allows multiple representations per resource. What happens if two concurrent PUT requests arrive for the same resource with different Content-Types? Does the system maintain consistency or could race conditions occur?

### Error Recovery in Transactions
The error_handling spec describes retry-with-backoff strategy but doesn't specify which operations are retryable. Are all transaction failures retryable, or only specific types (e.g., conflicts vs. disk-full)?

### Template Model Resolution
Representations can have template-model as symbol, string URI, or map. How are symbols resolved (namespace/function lookup)? What's the scope for function binding?

### Configuration & Service Initialization
External contracts describe expectations on XTDB and authentication services, but don't specify how these are initialized, discovered, or configured. Is everything in a centralized config file or dynamic discovery?

### Datalog Query Expressiveness
Authorization rules use Datalog patterns. What constructs are supported? (projections, aggregations, negation, etc.) Are there performance considerations for complex patterns?

### Default Behavior for Missing Resources
The locate-resource step (step 2) tries alternatives: exact URI match, OpenAPI definitions, resource locators, redirects, then "default empty resource." What is a default empty resource? Does it allow all methods or block everything?

### CORS Header Handling
OPTIONS method mentions "cors-headers-included :when origin header present" but doesn't specify which headers (Access-Control-Allow-Origin, Access-Control-Allow-Methods, etc.) or how origins are validated.

### Precondition Evaluation Order
RFC 7232 defines a strict precedence (If-Match → If-Unmodified-Since → If-None-Match → If-Modified-Since). The spec correctly documents this, but implementation ambiguity: does early failure exit immediately, or are all conditions always evaluated?
