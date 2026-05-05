# JUXT Site (juxt-site-full-2013) - Comprehensive Analysis Report

**Analysis Date:** 2026-05-01  
**Project:** juxt-site-full-2013 (Clojure implementation)  
**Spec Coverage:** 6 files covering core domain, data model, API behavior, data flows, error handling, and external contracts

---

## Open Questions

1. **ETag Generation Strategy**: The spec requires ETags on all representations for cacheability but doesn't document the algorithm. Are they deterministic (content-hash based) or sequential? This affects cache invalidation semantics.

2. **Bcrypt Cost Factor**: Bcrypt password hashing is used but the cost factor is not specified. Industry default (10-12) vs. higher security (14+) has operational implications.

3. **Random ID Collision Risk in Speculative DB**: Rule evaluation creates temporary entities with random xt/ids in a speculative database. What entropy source is used? Could collisions occur with concurrent rule evaluations?

4. **OpenID Connect Token Caching**: The spec mentions JWK set caching with TTL, but the specific TTL value and cache invalidation strategy on key rotation are not documented.

5. **Session Restart Behavior**: Sessions are stored in an in-memory atom. Is this documented to clients? Do they handle session loss on application restart?

6. **Resource Locator Pattern Matching Order**: When multiple ResourceLocators are configured, is there a documented precedence order, or is the current error (MultipleResourceLocatorMatches) the expected failure mode?

7. **Content Negotiation Default Fallback**: If all representations have qvalue 0 for Accept constraints, is there a documented fallback (e.g., return first, return error), or does the system always return 406?

8. **Datalog Query Compilation**: Are Rule target queries validated at write-time or only at evaluation-time? Invalid queries will cause runtime failures during authorization.

---

## Concerns

### Architectural Issues

**1. In-Memory Session Storage Without Persistence**
- **Issue**: Sessions stored in `sessions-by-access-token` atom (data_flows.allium, Session-Creation transaction)
- **Impact**: Session loss on application restart means all users are logged out; no cross-instance session sharing in distributed deployments
- **Severity**: Medium
- **Mitigation Options**: 
  - Add persistent session store (Redis, XTDB-backed)
  - Document session non-persistence in API contracts
  - Implement automatic re-authentication on restart

**2. Speculative Database for Authorization with Random Entity IDs**
- **Issue**: Rule evaluation (data_flows.allium, lines 268-274) creates temporary entities with random xt/ids
- **Risk**: 
  - ID collisions are theoretically possible (even with cryptographic randomness, collision probability grows with concurrent evaluations)
  - No documented uniqueness guarantee
  - Errors in speculative DB could silently fail (if query returns no results, rule doesn't match)
- **Likelihood**: Low (RNG collision unlikely), but impact is high if occurs
- **Recommendation**: Use deterministic ID derivation (e.g., hash of request context) or UUID v5 to guarantee collision-freedom

**3. Cascading Authorization Failures**
- **Issue**: Rule evaluation failure (error_handling.allium, RuleEvaluationFailure) defaults to empty rule set, which triggers DENY
- **Risk**: Database issues, malformed rules, or Datalog errors silently deny all requests
- **Severity**: High
- **Missing**: 
  - Error logging with context (which rule failed, why)
  - Monitoring/alerting hooks
  - Manual override capability

**4. Template Rendering Without Size Limits**
- **Issue**: Selmer template rendering (data_flows.allium, HTTP-GET-Processing step 5) has no output size constraints
- **Risk**: Malicious or buggy templates could produce unbounded output (memory exhaustion, slow response)
- **Missing**: Max output size validation, template size limits

**5. Content-Length Validation vs. Actual Body**
- **Issue**: Content-Length is validated but no guarantee that actual body matches declared length (data_flows.allium, HTTP-PUT-Processing step 1)
- **Current State**: Network provides actual length; mismatch usually detected by servlet layer
- **Gap**: Spec doesn't document behavior if body shorter than Content-Length

### Design Gaps

**6. No CORS Policy Specification**
- **Issue**: api_behaviour.allium contracts don't mention CORS headers (Access-Control-Allow-Origin, etc.)
- **Risk**: Browser-based clients cannot access resources cross-origin by default
- **Current State**: Spec is silent; implementation may not include CORS

**7. No Rate Limiting**
- **Issue**: No rate-limiting rules or contracts documented
- **Risk**: Denial-of-service via request flooding, brute-force password attacks
- **Recommendation**: Add rate-limiting rules (per-IP, per-user, per-resource)

**8. Missing CSRF Token Protection**
- **Issue**: No mention of CSRF tokens in PUT/POST/DELETE contracts (api_behaviour.allium)
- **Risk**: Cross-site request forgery on state-changing operations
- **Severity**: Medium (affects browser-based clients; API clients using Bearer tokens are safer)
- **Mitigation**: Add sameSite cookie attribute documentation, CSRF token flow if cookies used

**9. No Cache Invalidation Strategy**
- **Issue**: ETags are generated but no documented strategy for cache invalidation on updates
- **Current State**: Cache-Control headers mentioned in response but not specified in contracts
- **Gap**: Unclear if resources are cache-busted after PUT/POST/DELETE, or if stale caches expected

---

## Security Vulnerabilities

### Direct Dependencies

**Status: ZERO vulnerabilities reported by clj-watson scanner**

The NVD scan for juxt-site-full-2013 returned `vuln_count: 0`. This indicates:
- No known CVEs in direct dependencies as of scan date (2026-05-01)
- Dependencies include: XTDB, Ring/Jetty, XTDB RocksDB, Selmer, Malli, crypto-password (bcrypt), juxt/pick, juxt/grab, and others

**Scan Confidence Note**: The absence of vulnerabilities does not mean zero risk. XTDB, Jetty, and crypto libraries are mature and actively maintained, reducing risk, but:
- Custom vulnerability logic not detected by NVD (e.g., authorization bypass)
- Vulnerability database lag (0-30 days for published CVEs to reach NVD)
- Unpatched zero-days unknown

### Transitive Dependencies

**Assessment**: No transitive vulnerability data provided in scan output. Recommend:
1. Run `clj -M:clj-watson` with `--recursive` flag to analyze full tree
2. Check XTDB (uses RocksDB as submodule), Jetty (uses many net libs), and Integrant dependency trees

### Spec-Implied Security Risks

**1. Bcrypt Implementation Risk**
- **Library**: crypto-password (crypto.password.bcrypt)
- **Standard**: Bcrypt is industry-standard and timing-safe (good)
- **Gap in Spec**: Cost factor not documented
  - Cost factor 10 (default): ~10ms/verification (acceptable)
  - Cost factor 12+: ~40ms+/verification (better security, slower auth)
- **Recommendation**: Document required cost factor; implement cost-factor upgrade path for existing passwords

**2. Session Token Entropy**
- **Spec**: 24-byte random tokens, base64-encoded (data_flows.allium, line 237)
- **Assessment**: 24 bytes = 192 bits entropy (sufficient); SecureRandom is timing-safe
- **Gap**: No documented RNG source (java.security.SecureRandom? crypto-lib?)
- **Recommendation**: Explicitly specify `java.util.SecureRandom` or `clojure.tools.cli/random-bytes`

**3. Bcrypt Hash Verification Timing**
- **Standard**: crypto.password.bcrypt/check uses timing-safe comparison (good)
- **Vulnerability**: No documented protection against enumeration attacks (username guessing)
  - Attack: attacker sends many wrong passwords for non-existent users, measures timing
  - Mitigation: constant-time response regardless of user existence
- **Recommendation**: Add rate limiting per IP and per username to prevent enumeration

**4. OpenID Connect Token Validation**
- **Spec**: JWKs fetched and cached; JWT signature verified with keys (external_contracts.allium, lines 217-244)
- **Risks**:
  - Network latency: if OIDC provider unreachable, jwks-rsa cache may serve stale keys (key rotation window = vulnerability)
  - Key pinning not documented: no mechanism to reject compromised keys
  - Token expiry validated (claim validation in spec) but clock skew tolerance not documented
- **Recommendation**: 
  - Document cache TTL and max-age tolerance
  - Add key pinning (backup public key in config)
  - Document clock skew tolerance (±5 seconds recommended)

**5. Content-Type Sniffing Risk**
- **Current State**: api_behaviour.allium specifies Content-Type header in responses
- **Gap**: No X-Content-Type-Options: nosniff header documented
- **Risk**: Browser content-type sniffing could execute user-uploaded HTML/JS as scripts
- **Recommendation**: Add X-Content-Type-Options: nosniff to all responses

**6. Authorization Query Injection via Datalog**
- **Spec**: Rules are Datalog queries stored in database (core_domain.allium, Rule entity)
- **Risk**: If rule targets are user-modifiable (not documented), Datalog injection is possible
- **Current State**: Rules appear admin-configured, not user-input (low risk)
- **Recommendation**: Document that rule creation requires superuser privilege; validate rule syntax at write-time

---

## Potential Bugs (Spec Inconsistencies)

**1. Rule Effect Validation Mismatch**
- **Defined in core_domain.allium (line 104)**: effect is a keyword (e.g., ::pass/allow, ::pass/deny)
- **Defined in data_flows.allium (lines 289-291)**: algorithm checks for ::pass/allow and ::pass/deny
- **Defined in core_domain.allium (line 165)**: rule mentions `::pass/conditional` but data-model.allium (line 98) only validates allow/deny/conditional
- **Issue**: Conditional effect not handled in authorization-decision algorithm
- **Fix**: Either remove conditional from spec or implement conditional rule handling logic

**2. Session Lookup Race Condition**
- **Spec (data_flows.allium, Session-Lookup)**:
  1. Call expire-sessions! (atomic swap)
  2. Get session from atom
  3. Return session
- **Issue**: No documented synchronization after expire-sessions! completes; another request could modify atom between steps 1 and 2 (unlikely but possible with high concurrency)
- **Severity**: Low (atom swap is atomic; race window tiny)
- **Recommendation**: Get and expire in single swap operation: `(swap! atom (fn [m] (dissoc-expired m)))`

**3. ETag Weak vs. Strong Comparison Logic**
- **api_behaviour.allium (Conditional-Request-Evaluation, lines 205-214)**: 
  - If-Match: strong match only
  - If-None-Match on PUT/DELETE: 412 Precondition Failed (undocumented; RFC 7232 allows weak match to trigger 412)
  - If-None-Match on GET/HEAD: 304 Not Modified (weak match allowed per RFC)
- **Spec Gap**: Line 212 says "weak-matches" but RFC 7232 is ambiguous; implementation detail not documented
- **Fix**: Explicitly state: "If-None-Match uses weak ETag comparison for GET/HEAD, strong comparison for PUT/DELETE" per RFC 7232 Section 3.2

**4. Content-Encoding Validation Error Status**
- **error_handling.allium (UnsupportedContentEncoding, line 69)**: Returns 409 Conflict
- **RFC 7231**: Standard error for unsupported encoding is 415 Unsupported Media Type or 400 Bad Request
- **Issue**: 409 Conflict is semantically for "request conflict with current state" (used for version conflicts), not encoding
- **Fix**: Change error status to 415 (consistent with UnsupportedContentType and UnsupportedCharset)

**5. NoRequestBody Condition Impossible**
- **error_handling.allium (NoRequestBody, lines 26-31)**: "PUT/POST request has Content-Length but no body"
- **Issue**: If Content-Length is N > 0 and body is absent, the HTTP layer will hang waiting for N bytes (servlet timeout, not application error)
- **Spec Problem**: Condition is HTTP-layer, not application-layer; likely never caught by application code
- **Fix**: Remove this condition or document as "client protocol error" (HTTP layer responsibility)

**6. Representation Content XOR Body Invariant Weakly Enforced**
- **data_model.allium (line 45)**: Invariant `i_representation_content_xor_body`: "exactly one of content or body must be non-nil"
- **Issue**: This is data-model constraint but data_flows.allium never enforces it at write time; possible to violate invariant
- **Impact**: Ambiguous state (both set or both nil) could cause 500 errors during GET rendering
- **Fix**: Add write-time validation step in Resource-Write transaction

---

## Recommendations (Prioritized)

### P0: Security & Correctness (Ship Immediately)

1. **Add ETag Generation Algorithm Documentation**
   - Impact: Affects cache correctness
   - Action: Document: "ETags are SHA-256(content + last-modified) for deterministic cache invalidation" or choose algorithm
   - Effort: 1 hour (spec documentation)

2. **Fix Content-Encoding Error Status**
   - Impact: Client confusion (409 vs 415)
   - Action: Change UnsupportedContentEncoding status from 409 to 415 in error_handling.allium
   - Effort: 15 minutes

3. **Document Speculative DB ID Generation**
   - Impact: Authorization security
   - Action: Spec says "random xt/ids"; replace with deterministic UUID v5 of request context or add collision detection
   - Effort: 2-4 hours (implementation + test)

4. **Add Bcrypt Cost Factor Documentation**
   - Impact: Security vs. performance tradeoff
   - Action: Document cost factor requirement in external_contracts.allium and password verification flow
   - Effort: 1 hour

### P1: Architecture (Ship in Next Sprint)

5. **Implement Persistent Session Store**
   - Impact: Sessions survive restart; enables multi-instance deployment
   - Options: 
     - XTDB-backed sessions (consistent with design)
     - Redis-backed (external dependency)
   - Effort: 8-16 hours
   - Recommend: XTDB-backed for consistency; add session garbage collection trigger

6. **Add Rule Validation at Write Time**
   - Impact: Prevents invalid Datalog rules from silently denying all requests
   - Action: Validate Datalog syntax when Rule entity created (pre-submission)
   - Effort: 4-6 hours (may need Datalog parser or try-catch parsing)

7. **Add Rate Limiting**
   - Impact: Prevents brute-force attacks, DoS
   - Action: Add rate-limiting rules (per-IP: 100 req/min, per-user: 10 auth-attempts/min)
   - Effort: 8-12 hours (rule design + implementation + testing)

8. **Add CORS Headers**
   - Impact: Enables browser-based clients
   - Action: Add CORS contract to api_behaviour.allium; implement Access-Control-Allow-* headers
   - Effort: 4-6 hours

### P2: Robustness (Ship in Following Sprint)

9. **Add Template Size Limits**
   - Impact: Prevents memory exhaustion from malicious templates
   - Action: Add max-output-size validation after Selmer render
   - Effort: 2-4 hours

10. **Implement Authorization Failure Logging**
    - Impact: Observability for debugging auth issues
    - Action: Log rule evaluation errors with context (rule ID, request context, error)
    - Effort: 2-3 hours

11. **Fix Session Lookup Atomicity**
    - Impact: Prevents race condition (unlikely but possible)
    - Action: Combine expire-sessions and lookup in single atomic operation
    - Effort: 1-2 hours

12. **Add Content-Type: nosniff Header**
    - Impact: Prevents content-type sniffing attacks
    - Action: Add X-Content-Type-Options: nosniff to all responses
    - Effort: 1-2 hours

13. **Document Cache Invalidation Strategy**
    - Impact: Clarifies cache semantics for clients
    - Action: Specify Cache-Control directives for each response type (no-store for user data, max-age for static, etc.)
    - Effort: 2-3 hours

14. **Fix Rule Effect Enum Ambiguity**
    - Impact: Clarifies conditional rule behavior
    - Action: Either implement conditional rule effect handling or remove from spec
    - Effort: 4-8 hours (if implementing) or 30 minutes (if removing)

### P3: Documentation & Testing

15. **Add OpenID Connect Key Rotation Testing**
    - Impact: Validates OIDC token verification under key rotation
    - Action: Add integration test for JWK cache expiry and key rotation scenario
    - Effort: 4-6 hours

16. **Document Session Restart Behavior**
    - Impact: Prevents client surprises
    - Action: Add API documentation: "Sessions are lost on application restart; clients should handle 401 and re-authenticate"
    - Effort: 1 hour

---

## Summary Table

| Category | Count | Severity |
|----------|-------|----------|
| Open Questions | 8 | Informational |
| Architectural Concerns | 9 | Medium-High |
| Security Vulnerabilities (Known) | 0 | - |
| Security Risks (Spec-Implied) | 6 | Medium |
| Bugs/Inconsistencies | 6 | Low-Medium |
| Total Recommendations | 16 | P0:4, P1:4, P2:6, P3:2 |

---

## Next Steps

1. **Immediate** (this sprint):
   - Assign P0 recommendations to team
   - Open security review for bcrypt cost factor and session token entropy
   - Fix error status code (409 → 415)

2. **Short-term** (next 2 sprints):
   - Implement persistent session store
   - Add rule validation and rate limiting
   - Security testing for OIDC key rotation

3. **Medium-term** (next quarter):
   - Refactor authorization error handling with proper logging
   - Performance test with high-concurrency session lookup
   - Penetration test with CSRF, enumeration attacks

---

**Report generated:** 2026-05-01  
**Analysis tool:** Allium spec review + security checklist  
**Confidence:** Spec-level analysis; implementation may diverge
