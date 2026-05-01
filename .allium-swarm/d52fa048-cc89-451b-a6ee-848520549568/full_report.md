# juxt-site Comprehensive Scan Report

**Generated**: 2026-05-01  
**Repository**: juxt/site  
**Scan Type**: Allium Specification Analysis + NVD Vulnerability Assessment  
**Language**: Clojure  
**Scanner**: clj-watson  

---

## Executive Summary

**juxt-site** is a declarative HTTP application framework with policy-driven authorization, dynamic resource management, and integrated GraphQL support. The system architecture is built on XTDB (bitemporal database), Ring/Jetty (HTTP abstraction), and Selmer (templating).

- **Direct Dependencies Analyzed**: See external_contracts.allium (XTDB, Ring, Jetty, Selmer, GraphQL support)
- **Known CVEs**: 0 (as of scan date)
- **Specification Coverage**: 6 Allium specs with 120+ invariants and contracts
- **Baseline CI Status**: ✅ All recent runs passing

---

## Open Questions

These represent gaps between specification and implementation clarity that should be resolved to ensure correctness and security:

### 1. Ring 1 vs Ring 2 Contract Details
- **Issue**: Spec mentions `wrap-ring-1-adapter` conversion but doesn't specify exact key namespacing differences
- **Impact**: Ambiguity in request/response map structure could lead to middleware incompatibilities
- **Resolution**: Document exact key mapping (Ring 1 `:request-method` → Ring 2 `:ring.request/method`, etc.)

### 2. Dynamic Resource Locators
- **Issue**: `site/locator-fn` is defined as a custom function but no examples or invocation context given
- **Impact**: Runtime resource resolution could behave unexpectedly; custom locators lack contract definition
- **Resolution**: Specify function signature, argument context, return type contract, and error semantics

### 3. Rule Evaluation and Datalog Syntax
- **Issue**: Datalog patterns in `rule.target` described as "valid Datalog" without syntax/semantics specification
- **Impact**: Rules may be misconfigured; negation, aggregation, and join semantics unclear
- **Resolution**: Provide BNF grammar for rule patterns, specify which Datalog features are supported

### 4. Trigger Action Dispatch Table
- **Issue**: `site/action` is a keyword dispatch target but legal action keywords not documented
- **Impact**: Deployed systems may attempt non-existent actions, failing silently
- **Resolution**: Enumerate all supported trigger actions; define dispatch mechanism; document error handling

### 5. GraphQL Resolver Interface
- **Issue**: "Custom resolvers for datalog queries" mentioned but interface/context not specified
- **Impact**: Custom resolver implementations may omit required context or error handling
- **Resolution**: Define resolver function signature, full context passed, error contract

### 6. Selmer xt-Loader Path Resolution
- **Issue**: Custom loader for `{{path/to/resource}}` interpolation lacks resolution algorithm
- **Impact**: Template failures due to unclear path→resource mapping
- **Resolution**: Document loader semantics (relative vs absolute URIs, fallback behavior, error codes)

### 7. Error Representation Precedence
- **Issue**: "Try method-specific error rep, then ErrorResource, then default" precedence unclear
- **Impact**: Error responses may not respect configured handlers; client error handling fragile
- **Resolution**: Specify exact precedence logic and tie-breaking rules

### 8. Limiting-Clauses Application
- **Issue**: When `pass/limiting-clauses` granted by rules, application mechanism not defined
- **Impact**: Authorization may grant excessive access if limiting clauses not enforced
- **Resolution**: Specify whether limiting-clauses are applied to all subsequent queries, enforced at response time, or both

### 9. Conditional Request Precedence
- **Issue**: Spec says "conditionals evaluated before authorization" but full RFC 4918 precedence undefined
- **Impact**: Request ordering may violate HTTP semantics or security expectations
- **Resolution**: Specify exact evaluation order for all precondition headers (If-Match, If-Modified-Since, etc.)

### 10. Session Expiration Mechanism
- **Issue**: Sessions stored in `sessions-by-access-token[token]` with expiry, but cleanup mechanism unspecified
- **Impact**: Memory leak potential if expired sessions not garbage-collected
- **Resolution**: Document expiration cleanup: (a) lazy removal on lookup, (b) background task, (c) both

### 11. Performance and Scalability Metrics
- **Issue**: "May be seconds for normal workloads" is vague; no load-testing data provided
- **Impact**: Operators cannot predict latency or capacity
- **Resolution**: Provide benchmarks: rules/request, max XTDB size, p50/p99 latency under load

### 12. with-tx Semantics (Copy-on-Write vs In-Place)
- **Issue**: "`with-tx` speculatively applies transaction" but doesn't clarify if new snapshot or modified original
- **Impact**: Concurrent authorization queries may observe inconsistent state
- **Resolution**: Clarify transaction isolation semantics and snapshot independence

### 13. Rule Matching Semantics (Order/Priority)
- **Issue**: "If any :deny → denied, else if any :allow → approved, else → denied" — is this order-independent?
- **Impact**: Rule evaluation order could affect authorization if semantics unclear
- **Resolution**: Confirm order-independent evaluation; specify tie-breaking if multiple rules match

### 14. Content-Length vs HTTP/2 Stream Uploading
- **Issue**: Spec requires Content-Length on PUT/POST; HTTP/2 may use streaming without Content-Length
- **Impact**: Modern clients using streaming uploads may be rejected
- **Resolution**: Support chunked/streaming uploads or document Content-Length requirement for all clients

### 15. ETag Generation Algorithm
- **Issue**: ETag format defined (`"..."` or `*`) but generation algorithm not specified
- **Impact**: Clients cannot predict ETag values; weak vs strong matching semantics unclear
- **Resolution**: Specify if ETag = hash(content), last-modified, version ID; define weak ETag policy

### 16. Custom Handler Function Signatures
- **Issue**: `site/post-fn`, `site/put-fn`, `site/patch-fn` signatures and error semantics undefined
- **Impact**: Handler implementations may crash due to missing arguments or incompatible error handling
- **Resolution**: Document function signature (args), full context available, expected return type, error contract

### 17. Weak vs Strong ETag Matching
- **Issue**: RFC 7232 defines weak matching for If-None-Match vs strong for If-Match
- **Impact**: Precondition evaluation may not comply with HTTP spec
- **Resolution**: Verify implementation distinguishes weak ETags (`W/"..."`) and applies correct matching rules

### 18. CORS Origin Pattern Matching
- **Issue**: `site/access-control-allow-origins` is a map, but matching semantics (exact, regex, wildcard) undefined
- **Impact**: CORS headers may allow unintended origins or deny legitimate ones
- **Resolution**: Document matching algorithm (string match, regex, glob); clarify wildcard semantics

### 19. Bearer Token Design (Opaque ID vs JWT)
- **Issue**: "24 random bytes, base64" — are these session IDs (opaque references) or JWTs (signed tokens)?
- **Impact**: Token forgery/tampering prevention mechanism unclear; implications for distributed systems
- **Resolution**: Clarify token design; if session ID, specify server-side validation; if JWT, document signing

### 20. Template Model Resolution Namespace
- **Issue**: When `site/template-model` is a symbol, what namespace used to resolve it?
- **Impact**: Template rendering may fail if namespace resolution differs from expectation
- **Resolution**: Specify: current namespace, configured namespace, or fully qualified symbol required

### 21. Error Logging Redaction Rules
- **Issue**: "Redacted if untrusted" mentioned but exact rules (what exposed, what hidden) not fully detailed
- **Impact**: Sensitive information may leak to untrusted clients or operators may lack debugging data
- **Resolution**: Document redaction policy: stack trace, headers, response body, query parameters, etc.

### 22. Break-Glass Error Handling
- **Issue**: If `wrap-error-handling` itself throws or error-response throws, undefined behavior
- **Impact**: Request could fail without proper response; may hang or crash server
- **Resolution**: Document fatal error handling; ensure wrap-check-error-handling is truly terminal

### 23. Datalog Query Limits
- **Issue**: Can rules reference each other? Implicit limits on rule size or query time?
- **Impact**: Rules could cause denial-of-service via expensive queries
- **Resolution**: Document: (a) rule composition allowed, (b) max query time, (c) query complexity limits

### 24. Concurrent Update Conflict Resolution
- **Issue**: Two requests concurrently POST to create resources; how does XTDB optimistic locking interact with error handling?
- **Impact**: Lost updates, race conditions, or unexpected 409 responses
- **Resolution**: Specify: are retries automatic? What is client's responsibility? Document conflict semantics

### 25. Feature Completeness Status
- **Issue**: "RFC 7233 Range requests: NOT YET IMPLEMENTED; Content-Range rejected"
- **Impact**: Clients expecting range request support may fail; large file downloads inefficient
- **Resolution**: Document roadmap for range request support; clarify rejection behavior

---

## Concerns

### Architectural & Design Issues

#### 1. **Authorization Happens Late in Pipeline**
- **Finding**: Authorization is checked after resource location but before method invocation, per RFC 4918
- **Risk**: If resource location is expensive (datalog query), unauthenticated users pay cost for denied requests
- **Mitigation**: Consider early authentication check (before resource location) for known user-specific resources
- **Status**: Accepted by spec; documented trade-off for RFC compliance

#### 2. **Session Storage In-Memory Without Persistence**
- **Finding**: Sessions stored in `sessions-by-access-token` map in process memory only
- **Risk**: Server restart loses all active sessions; no session replication in multi-instance deployments
- **Impact**: High**: Users cannot maintain sessions across deployments
- **Mitigation**: Consider XTDB persistence for session data, or document single-instance deployment requirement

#### 3. **Rule Evaluation is Untrusted Datalog**
- **Finding**: Authorization rules execute arbitrary Datalog patterns from database
- **Risk**: Misconfigured rules (e.g., negation loops, expensive joins) could cause denial-of-service
- **Impact**: Admin privilege required to update rules; no query validation or complexity limits documented
- **Mitigation**: Document rule review process; add query timeout guards; consider rule complexity analyzer

#### 4. **Password Hashing Cost Not Configurable at Runtime**
- **Finding**: `cryptography/crypto-password` uses default cost=11 (≈100ms per login)
- **Risk**: Cannot adjust cost in response to attacker hardware advances without code change
- **Impact**: Bcrypt cost hardcoded in library; password hashing speed fixed at deployment time
- **Mitigation**: Document cost assumption; plan migration path if cost needs to increase

#### 5. **Template Model Queries Unbounded**
- **Finding**: `site/template-model` can specify arbitrary maps of datalog queries
- **Risk**: Complex nested queries in templates could cause slow response times
- **Impact**: Template performance degradation not isolated; difficult to profile
- **Mitigation**: Document template performance best practices; consider query cost estimation

#### 6. **No Authentication Encryption for Bearer Tokens**
- **Finding**: Bearer tokens are "24 random bytes, base64" — opaque but unencrypted
- **Risk**: If token stored unencrypted (browser localStorage), client-side theft is possible
- **Impact**: No built-in protection for tokens at rest; relies on HTTPS in-transit
- **Mitigation**: Document token security: (a) HTTPS required, (b) recommend httpOnly cookies, (c) short expiry times

#### 7. **Error Response Redaction Inconsistent**
- **Finding**: Stack traces hidden from untrusted clients but visible to authenticated users
- **Risk**: Authenticated attacker with limited privileges could use error messages for reconnaissance
- **Impact**: Information disclosure risk if auth system compromised
- **Mitigation**: Consider additional redaction for privileged info; log full errors server-side only

#### 8. **XTDB Dependency on RocksDB**
- **Finding**: Persistence depends on RocksDB local filesystem
- **Risk**: No distributed consensus; single-instance deployment has single point of failure
- **Impact**: Data loss if disk corrupted; no high-availability story
- **Mitigation**: Document backup strategy; consider multi-instance with shared storage (not yet supported)

#### 9. **Ring Middleware Composition Order Critical**
- **Finding**: 30+ middleware stages; execution order determines behavior
- **Risk**: Middleware ordering mistakes could violate HTTP semantics (e.g., auth before error handling)
- **Impact**: Complex to verify correct order; difficult to add new middleware without breaking invariants
- **Mitigation**: Document middleware dependency graph; consider declarative composition

#### 10. **Triggers Execute After Response Sent**
- **Finding**: Post-request triggers fire after successful response, failures logged but not propagated
- **Impact**: Side effects may fail silently; clients unaware of trigger failures
- **Mitigation**: Spec correctly documents this trade-off (graceful degradation); monitoring required for trigger failures

#### 11. **Unconditional Error Request Caching**
- **Finding**: All error requests cached in in-memory FIFO (1000 entries) with soft references
- **Risk**: Soft references may be cleared unexpectedly under GC pressure; cache not queryable
- **Impact**: Debugging error chains difficult; cache not accessible via API
- **Mitigation**: Document cache behavior; consider persistent audit log for errors

#### 12. **Content Negotiation Fails Open**
- **Finding**: If no representation matches Accept headers, respond 406 (Not Acceptable)
- **Risk**: Strict client preferences could cause 406 errors; servers must be very careful with Vary header
- **Impact**: Must maintain representation coverage for all declared Accept axes
- **Mitigation**: Spec correct per RFC; document best practices for declaration

#### 13. **Datalog Concurrency Semantics Unclear**
- **Finding**: Spec mentions "with-tx speculatively applies transaction" but isolation level undefined
- **Risk**: Concurrent rule evaluation against same resource state might observe dirty reads
- **Impact**: Authorization decisions may be based on stale data
- **Mitigation**: Confirm XTDB snapshot isolation; document as feature, not bug

#### 14. **Precondition Evaluation Order Vulnerable to Timing**
- **Finding**: If-Modified-Since evaluated before authorization
- **Risk**: If-None-Match matches before auth check; client learns resource exists
- **Impact**: Information disclosure if existence should be hidden from unauthenticated users
- **Mitigation**: Consider moving conditional evaluation after authorization check

#### 15. **GraphQL Executor Has No Depth Limit**
- **Finding**: GraphQL query execution mentioned but depth limits, complexity analysis not documented
- **Risk**: Nested GraphQL queries could cause denial-of-service
- **Impact**: Attackers could craft expensive queries
- **Mitigation**: Document query complexity limits; implement depth/breadth guards

---

## Security Vulnerabilities

### Direct Dependencies
**Status**: ✅ **No Known CVEs** as of 2026-05-01

The NVD scan (clj-watson) reported **0 vulnerabilities** for the primary Clojure project. However, transitive dependencies should be verified.

**Key Dependencies** (from external_contracts.allium):
- **XTDB 1.21.0**: Checked, no known critical CVEs
- **Ring 2.0**: Checked, no known critical CVEs  
- **Selmer 1.12.50**: Checked, no known critical CVEs
- **cryptography/crypto-password 0.3.0**: Stable, no known vulnerabilities
- **Jetty 9.x**: Check via Ring's underlying dependencies

### Transitive Dependencies
No transitive dependency data provided in scan output. **Recommendation**: Run `clj-watson` with full dependency tree analysis.

### Attack Surface Analysis

#### 1. **Authorization Rule Injection**
- **Vector**: Admin updates rules with malicious datalog patterns
- **Impact**: Denial-of-service via expensive queries; unauthorized access if rules misconfigured
- **Likelihood**: **Low** (requires admin privilege) but **High Impact** if compromised
- **Mitigation**: 
  - Role-based access control for rule updates
  - Query validation before persistence
  - Audit logging of rule changes

#### 2. **XTDB Query Injection**
- **Vector**: If template-model or custom handlers build queries from user input
- **Impact**: Unauthorized data access, denial-of-service
- **Likelihood**: **Medium** (depends on implementation)
- **Mitigation**: 
  - Always use parameterized queries (datalog `:in` bindings)
  - Validate and sanitize user input before query construction
  - Document safe query patterns

#### 3. **Bearer Token Leakage**
- **Vector**: Tokens stored unencrypted in browser storage or logs
- **Impact**: Session hijacking, impersonation
- **Likelihood**: **Medium** (common client-side mistake)
- **Mitigation**: 
  - Document token security best practices (httpOnly cookies, HTTPS)
  - Short token expiry (recommend < 1 hour)
  - Token rotation on sensitive operations

#### 4. **Template Injection via User-Controlled Model**
- **Vector**: If template model keys come from user input
- **Impact**: Server-side template injection (SSTI), arbitrary code execution
- **Likelihood**: **Medium** (depends on implementation)
- **Mitigation**: 
  - Never interpolate user input directly into template-model
  - Validate model structure before rendering
  - Use Selmer's safe tag subset

#### 5. **GraphQL Denial-of-Service**
- **Vector**: Complex nested queries or alias bombs
- **Impact**: Server CPU exhaustion, response timeout
- **Likelihood**: **High** (no limits documented)
- **Mitigation**: 
  - Implement GraphQL query depth limit (suggest max 10)
  - Implement complexity scoring
  - Per-user query rate limiting

#### 6. **CORS Bypass via Origin Spoofing**
- **Vector**: If CORS origin matching uses regex with unintended wildcards
- **Impact**: Cross-origin requests from attacker-controlled sites
- **Likelihood**: **Low** (requires misconfiguration)
- **Mitigation**: 
  - Clarify origin matching semantics
  - Prefer exact match over regex
  - Document CORS security trade-offs

#### 7. **Conditional Request Timing Channel**
- **Vector**: If-Modified-Since evaluated before auth reveals resource modification time
- **Impact**: Information disclosure about resource existence and modification
- **Likelihood**: **Low** (indirect attack)
- **Mitigation**: 
  - Move conditional evaluation after authorization
  - Return identical response headers for 403/404

#### 8. **ETag Collision / Weak ETag Bypass**
- **Vector**: If weak ETags used for If-Match (strong-matching context)
- **Impact**: Precondition bypass; overwrite resource when condition should fail
- **Likelihood**: **Low** (spec defines strong matching)
- **Mitigation**: 
  - Verify implementation uses strong ETag comparison for If-Match
  - Document ETag generation to avoid collisions

---

## Potential Bugs

### Behavioral Inconsistencies & Spec Violations

#### 1. **Session Expiration Race Condition**
- **Description**: If session expires between lookup and use, no guarantee of atomicity
- **Spec Location**: data_flows.allium line 264-266
- **Likelihood**: **Medium**
- **Impact**: Concurrent request might be authorized after token expires
- **Fix**: Atomically check expiry when issuing subject; fail early if expired

#### 2. **Missing Representation → 404 vs 406 Ambiguity**
- **Description**: Spec says 404 for GET/HEAD with no representations, but 406 if Accept headers don't match
- **Spec Location**: api_behaviour.allium line 136-137
- **Likelihood**: **Low**
- **Impact**: Clients confused by response code (should be 406 if Accept mismatch)
- **Fix**: Clarify: (a) is there a representation? (b) does it match Accept? Then choose 404 vs 406

#### 3. **Vary Header Incomplete**
- **Description**: `Vary` header must list all negotiation axes, but spec intersection logic unclear
- **Spec Location**: api_behaviour.allium line 157-160
- **Likelihood**: **Medium**
- **Impact**: CDN caching could serve wrong representation if Vary incomplete
- **Fix**: Explicitly list all axes that affected selection (not just requested)

#### 4. **OPTIONS Method Bypasses Authorization**
- **Description**: Spec says OPTIONS bypasses auth entirely; could leak information about methods
- **Spec Location**: api_behaviour.allium line 63, core_domain.allium line 181
- **Likelihood**: **Low**
- **Impact**: Unauthenticated users learn what methods are available (usually acceptable)
- **Mitigation**: Document this as intended; consider if methods themselves should be hidden

#### 5. **Error Response Representation Negotiation Unclear**
- **Description**: Error responses should be content-negotiated, but spec mentions "default: text/html or text/plain"
- **Spec Location**: error_handling.allium line 349
- **Likelihood**: **Medium**
- **Impact**: Inconsistent error response content-types; JSON-only clients get HTML errors
- **Fix**: Always attempt content negotiation for error responses, with fallback

#### 6. **Request URI Validation Missing RFC Check**
- **Description**: Host header validated but request.uri not checked for absolute vs relative
- **Spec Location**: api_behaviour.allium line 13-26
- **Likelihood**: **Low**
- **Impact**: Request-target syntax errors not caught early
- **Fix**: Validate that request.uri is well-formed per RFC 3986

#### 7. **Limiting-Clauses Not Applied if Materialized Early**
- **Description**: If limiting-clauses intended to constrain subsequent queries, but query already materialized
- **Spec Location**: core_domain.allium line 72
- **Likelihood**: **Medium**
- **Impact**: Authorization might grant wider access than intended
- **Fix**: Clarify that limiting-clauses apply to: (a) all subsequent queries, (b) response generation, (c) both

#### 8. **Content-Length Validation Order**
- **Description**: Max-content-length check happens after Content-Length parsed, but before body read
- **Spec Location**: api_behaviour.allium line 181
- **Likelihood**: **Low**
- **Impact**: Malicious client could declare huge Content-Length; server attempts to read gigabytes
- **Fix**: Reject if declared Content-Length > max-content-length before reading body

#### 9. **HEAD Request Body Handling**
- **Description**: Spec says HEAD response identical to GET but body empty; is response serialization cost the same?
- **Spec Location**: api_behaviour.allium line 53-57
- **Likelihood**: **Low**
- **Impact**: If body generation is expensive, HEAD could still be slow
- **Mitigation**: Document that handlers should avoid body generation for HEAD

#### 10. **ETag Weak/Strong Matching Inconsistency**
- **Description**: RFC 7232 Section 2.3.2 requires weak matching for If-None-Match, strong for If-Match
- **Spec Location**: api_behaviour.allium line 211, 228
- **Likelihood**: **Medium**
- **Impact**: Precondition evaluation could violate HTTP spec
- **Fix**: Verify code implements weak vs strong matching correctly

#### 11. **Redirect Status Code Selection**
- **Description**: Spec mentions 302 or 307 but doesn't specify when to use each
- **Spec Location**: error_handling.allium line 172-179
- **Likelihood**: **Low** (covered by RFC 7231)
- **Impact**: If wrong status used, clients may change request method on redirect
- **Mitigation**: Document: 302 for temporary redirects allowing method change, 307 to preserve method

#### 12. **Trigger Query Evaluation Order**
- **Description**: Triggers executed post-response; if multiple triggers, execution order matters for side effects
- **Spec Location**: data_flows.allium line 93-100
- **Likelihood**: **Low**
- **Impact**: Non-deterministic trigger execution could cause race conditions
- **Mitigation**: Document trigger execution order (sequential, deterministic)

#### 13. **Bcrypt Cost-Time Bomb**
- **Description**: Bcrypt cost=11 takes ~100ms per login; if cost increased in library, login latency spikes
- **Spec Location**: external_contracts.allium line 236
- **Likelihood**: **Low** (design issue, not bug)
- **Impact**: Future library upgrades could cause denial-of-service
- **Mitigation**: Document cost assumption; test login performance on future crypto library upgrades

#### 14. **XTDB Transaction Timeout Unspecified**
- **Description**: `await-tx` may block "for seconds" but exact timeout undefined
- **Spec Location**: external_contracts.allium line 26
- **Likelihood**: **Medium**
- **Impact**: PUT/POST handlers might hang indefinitely if XTDB stalls
- **Fix**: Set explicit timeout; fail fast if XTDB unavailable

#### 15. **Representation Variant Loop Possible**
- **Description**: If `site/variant-of` references itself or forms a cycle, infinite loop in representation lookup
- **Spec Location**: data_model.allium line 243-248
- **Likelihood**: **Medium** (no referential integrity constraint)
- **Impact**: Response generation hangs
- **Fix**: Add invariant: `site/variant-of` must be acyclic; pre-compute reachability

---

## Recommendations

### Priority 1: Critical (Deploy Before Production)

#### 1. **Clarify Bearer Token Format and Validation**
- **Action**: Decide: opaque session ID or JWT? Document signing/validation mechanism
- **Why**: Token forgery/tampering prevention is fundamental to authentication security
- **Effort**: 2-4 hours documentation + 1-2 days code review
- **Risk**: High; incorrect tokens could enable session hijacking
- **Owner**: Security team + architects

#### 2. **Implement Query Timeout Guards**
- **Action**: Add hard timeout to XTDB queries (both rules and templates); fail fast if exceeded
- **Why**: Prevent denial-of-service from expensive rules or queries
- **Effort**: 1 day implementation + 2 days testing
- **Risk**: Medium; timeouts could cause 504 errors on legitimate complex queries
- **Owner**: Core team
- **Acceptance Criteria**: All queries timeout within 5 seconds; configurable per environment

#### 3. **Add GraphQL Query Complexity Limits**
- **Action**: Implement depth limit (max 10 nesting), complexity scoring, per-user rate limiting
- **Why**: Prevent alias bomb and nested query denial-of-service attacks
- **Effort**: 2-3 days implementation + testing
- **Risk**: Low; well-understood GraphQL hardening patterns
- **Owner**: GraphQL integration team
- **Acceptance Criteria**: Queries > depth 10 rejected; complexity score enforced; metrics tracked

#### 4. **Persist Sessions to XTDB**
- **Action**: Move sessions from in-memory to XTDB; add session table with TTL cleanup
- **Why**: Enable multi-instance deployments; prevent session loss on restart
- **Effort**: 3-4 days implementation + integration testing
- **Risk**: Medium; XTDB transaction overhead on every request
- **Owner**: Core team
- **Acceptance Criteria**: Sessions survive restart; multi-instance session sharing works; cleanup removes expired sessions

#### 5. **Document Rule Safety Review Process**
- **Action**: Create admin runbook for reviewing rules before deployment; add query complexity analyzer
- **Why**: Rules are code; prevent misconfigured rules from causing DoS or unauthorized access
- **Effort**: 2-3 days (runbook + tooling)
- **Risk**: Low; process/tooling, no code changes
- **Owner**: DevOps + Security

### Priority 2: High (Resolve Before Full Release)

#### 6. **Resolve Datalog Pattern Syntax Specification**
- **Action**: Document BNF grammar for rule patterns; specify supported Datalog features (negation, aggregation)
- **Why**: Prevent misconfigured rules; improve error messages
- **Effort**: 1-2 days documentation
- **Risk**: Low; clarification only
- **Owner**: Architects
- **Acceptance Criteria**: Grammar document + 5+ example rule patterns

#### 7. **Clarify Limiting-Clauses Application**
- **Action**: Specify whether limiting-clauses are applied to (a) all subsequent queries, (b) response generation, (c) enforced at response time
- **Why**: Security-critical; misunderstanding could grant excessive access
- **Effort**: 2-3 hours clarification + code review
- **Risk**: Medium; might require code changes if current implementation differs from spec
- **Owner**: Security team + core developers

#### 8. **Move Conditional Evaluation After Authorization**
- **Action**: Reorder middleware: locate resource → authenticate → authorize → evaluate conditionals
- **Why**: Prevent information disclosure about resource existence/modification time to unauthorized users
- **Effort**: 1-2 days middleware reordering + testing
- **Risk**: Medium; could affect cache hit rates or RFC compliance
- **Owner**: Core team
- **Acceptance Criteria**: Conditionals evaluated after auth; no timing differences between 403 and 304

#### 9. **Document Ring Adapter Compatibility**
- **Action**: Specify Ring 1 → Ring 2 key mapping; test with multiple Ring versions
- **Why**: Prevent middleware incompatibilities; clarify version constraints
- **Effort**: 1 day documentation + compatibility testing
- **Risk**: Low; testing and documentation only
- **Owner**: Integration team

#### 10. **Implement ETag Generation Strategy**
- **Action**: Define whether ETag = hash(content), version-id, or timestamp; document weak ETag policy
- **Why**: Enable client-side caching; clarify If-None-Match/If-Match semantics
- **Effort**: 1-2 days design + 1 day implementation
- **Risk**: Low; standard HTTP pattern
- **Owner**: Core team

### Priority 3: Medium (Roadmap, Non-Blocking)

#### 11. **Add Feature Flag for Session Encryption**
- **Action**: Make session storage encryption optional (AES-GCM); default to encrypted in production
- **Why**: Reduce risk of session leakage if memory is dumped
- **Effort**: 2 days implementation
- **Risk**: Low; optional feature, backward compatible
- **Owner**: Security team

#### 12. **Implement Range Request Support (RFC 7233)**
- **Action**: Currently rejected; add support for Content-Range, 206 Partial Content
- **Why**: Enable efficient large file downloads; improve performance for media streaming
- **Effort**: 3-4 days implementation + testing
- **Risk**: Medium; complex RFC; requires careful implementation
- **Owner**: Core team
- **Timeline**: Q3 2026 (not critical)

#### 13. **Add Query Complexity Analyzer for Rules**
- **Action**: Analyze rule patterns for potential performance issues; flag complex queries
- **Why**: Catch expensive rules before deployment
- **Effort**: 2-3 days implementation (static analysis)
- **Risk**: Low; advisory tooling only
- **Owner**: DevOps team

#### 14. **Document Template Performance Best Practices**
- **Action**: Create guide for efficient template-models; benchmark common patterns
- **Why**: Help developers avoid slow templates
- **Effort**: 1-2 days documentation + benchmarking
- **Risk**: Low; documentation only
- **Owner**: Documentation team

#### 15. **Add Request ID to Error Logs**
- **Action**: Ensure request-id (MDC) consistently logged with errors
- **Why**: Enable tracing of errors back to request context
- **Effort**: 1 day implementation + verification
- **Risk**: Low; logging enhancement
- **Owner**: Core team

#### 16. **Implement Circuit Breaker for XTDB Failures**
- **Action**: Fast-fail with 503 if XTDB consistently fails; periodic retry
- **Why**: Prevent cascading failures; graceful degradation
- **Effort**: 2-3 days implementation + testing
- **Risk**: Medium; requires careful state management
- **Owner**: Core team
- **Timeline**: Q2 2026

#### 17. **Document CORS Security Trade-offs**
- **Action**: Clarify CORS origin matching semantics; document exposure of OPTIONS response
- **Why**: Help operators understand CORS security implications
- **Effort**: 1 day documentation
- **Risk**: Low; documentation only
- **Owner**: Security team

#### 18. **Add Audit Trail for Sensitive Operations**
- **Action**: Ensure rule updates, user role changes logged to audit-log table in XTDB
- **Why**: Enable compliance reporting and forensic investigation
- **Effort**: 2 days implementation
- **Risk**: Low; additive logging
- **Owner**: DevOps + Security
- **Timeline**: Q2 2026

---

## Testing & Validation Gaps

Based on spec analysis, the following should have automated test coverage:

### 1. **Authorization Rule Coverage**
- [ ] Test each rule type (SuperuserAlwaysApproved, PublicResourcesReadable, etc.)
- [ ] Test rule combining: multiple :allow rules, multiple :deny rules, mixed
- [ ] Test limiting-clauses application
- [ ] Test invalid Datalog patterns (error handling)

### 2. **Content Negotiation**
- [ ] Test with all Accept* headers present/absent
- [ ] Test q-value parsing and scoring
- [ ] Test wildcard matching (*/*), specific media-types
- [ ] Test 406 Not Acceptable (no match)
- [ ] Test Vary header completeness

### 3. **Conditional Requests**
- [ ] If-Match: ETag match, no match, * wildcard
- [ ] If-None-Match: weak/strong matching, * wildcard
- [ ] If-Modified-Since: date parsing, comparison
- [ ] If-Unmodified-Since: date parsing, comparison
- [ ] Combination of multiple conditionals

### 4. **Error Handling**
- [ ] Test each error condition (400, 401, 403, 404, 406, 412, 413, 415, etc.)
- [ ] Test error response content-negotiation
- [ ] Test redaction rules (authenticated vs untrusted)
- [ ] Test logging (MDC, stack traces)

### 5. **Session Management**
- [ ] Test token generation randomness (cryptographic quality)
- [ ] Test token expiration
- [ ] Test concurrent session cleanup
- [ ] Test token lookup race conditions

### 6. **GraphQL Integration**
- [ ] Test query parsing errors
- [ ] Test schema validation errors
- [ ] Test resolver execution
- [ ] Test query timeout behavior
- [ ] Test query complexity limits (when implemented)

### 7. **Template Rendering**
- [ ] Test template loading from XTDB
- [ ] Test model resolution (symbol, string, map)
- [ ] Test xt-loader resource resolution
- [ ] Test template syntax errors
- [ ] Test missing template resource (500)

### 8. **Multi-Instance / Distributed**
- [ ] Test XTDB consistency with multiple node instances
- [ ] Test session sharing (after persistence)
- [ ] Test concurrent rule updates (XTDB transaction conflicts)

### 9. **Performance & Limits**
- [ ] Test max-content-length enforcement
- [ ] Test XTDB query timeout
- [ ] Test slow query detection
- [ ] Benchmark: rules per request, representation count
- [ ] Load test: concurrent requests with authorization

### 10. **Datalog Safety**
- [ ] Test rule evaluation with missing entities (not found)
- [ ] Test rule evaluation with circular role hierarchies (should fail invariant)
- [ ] Test query timeout on expensive queries
- [ ] Test negation in rules

---

## Summary of Findings

| Category | Count | Status |
|----------|-------|--------|
| **Open Questions** | 25 | ⚠️ Should resolve before production |
| **Architectural Concerns** | 15 | ⚠️ Documented trade-offs; monitoring required |
| **Security Vulnerabilities** | 0 (known CVEs) | ✅ No direct CVEs |
| **Attack Surface Issues** | 8 | ⚠️ Require hardening (query limits, token validation) |
| **Potential Bugs** | 15 | ⚠️ Need code verification / fixes |
| **Recommendations** | 18 | 5 Critical, 5 High, 8 Medium |

### Risk Assessment

**Overall Risk Level**: **MEDIUM**

- **No known CVEs** in direct dependencies (as of 2026-05-01)
- **Specification coverage** is comprehensive but **clarity gaps** on critical details (session handling, token format, query limits)
- **Authorization model** is strong (datalog-based) but **vulnerable to misconfiguration** and **denial-of-service**
- **Error handling** is documented but **needs validation** of actual implementation
- **Multi-instance deployment** not yet supported (in-memory sessions)

### Before Production Deployment

**Must-Have**:
1. Resolve Bearer token format and validation mechanism
2. Implement query timeout guards for XTDB
3. Add GraphQL query complexity limits
4. Clarify limiting-clauses application (security-critical)
5. Document rule safety review process

**Strongly-Recommended**:
6. Move sessions to persistent storage (XTDB)
7. Implement precondition evaluation after authorization
8. Document ring adapter compatibility

### Monitoring & Operations

Deploy with monitoring for:
- Authorization rule evaluation latency (p99 > 100ms = misconfigured rule)
- XTDB query timeout frequency (target: 0 per day)
- GraphQL query execution time (p99 > 5s = attack or complex query)
- Session expiration cleanup lag (memory leak indicator)
- Bearer token validation failures (attack indicator)
- Trigger side-effect failures (feature degradation)

---

## Appendix A: Specification Files Analyzed

1. **core_domain.allium** — Entities, rules, invariants (228 lines)
2. **data_model.allium** — Persistent data model, constraints (283 lines)
3. **api_behaviour.allium** — HTTP contracts, request/response handling (357 lines)
4. **data_flows.allium** — End-to-end request flows (458 lines)
5. **error_handling.allium** — Error conditions, recovery strategies (412 lines)
6. **external_contracts.allium** — External dependency contracts (397 lines)

**Total Spec Content**: ~2,200 lines of Allium specifications

**Invariants & Rules**: 120+ (data invariants, business rules, HTTP contracts)

---

## Appendix B: Scan Metadata

- **Repository**: juxt/site (commit a8d4-9416eec26458)
- **Language**: Clojure (deps.edn)
- **Scanner Used**: clj-watson (NVD)
- **Known CVEs Found**: 0
- **Scan Date**: 2026-05-01
- **Baseline CI**: ✅ Passing (25+ recent runs)

---

**Report Generated**: 2026-05-01 17:06:22 UTC  
**Spec Analysis Tool**: Allium Swarm Pipeline  
**Analyst**: Claude Code + Allium Specification Review
