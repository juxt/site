# Site Pipeline - Comprehensive Analysis Report

**Project:** juxt-site-2bf30277-2c75-436b-a8d4-9416eec26458  
**Language:** Clojure  
**Scanner:** clj-watson  
**Report Date:** 2026-05-01

---

## Executive Summary

This JUXT Site implementation is a REST-based content management system with HTTP resource storage, content negotiation, and fine-grained access control via Datalog authorization rules. The system uses XTDB for bitemporal persistence and Ring/Jetty for HTTP serving.

**Key Findings:**
- **No direct security vulnerabilities detected** by dependency scanner (0 CVEs)
- **Architectural strengths:** bitemporal history, ACID transactions, datalog-driven authorization
- **Design concerns:** multiple areas of implicit trust and error-recovery gaps
- **Potential issues:** resource locator pattern conflicts, template rendering fallbacks, error handling edge cases

---

## Open Questions

### Specification Clarity

1. **Trigger Evaluation Timing**  
   - Flow Step 12 evaluates triggers *after* response generation (step 13) but the flow shows triggers collected during step 12. Does a trigger that modifies response state affect the final response sent to the client? Is trigger execution ordered or concurrent?

2. **Datalog Query Evaluation Scope in Authorization**  
   - When evaluating `rule.target` patterns in step 9 (authorization), which request context entities are available? The spec mentions building context with `subject`, `resource`, `request`, `representation`, `environment` but doesn't specify exact schema or indexing guarantees.

3. **OpenAPI Schema Validation Binding**  
   - The `openapi-validation-flow` is defined but not integrated into `http-request-handling` flow. Is OpenAPI validation optional, coexistent with URI-based routing, or a separate endpoint family?

4. **Representation Immutability vs. PUT Overwrites**  
   - The data model states "Once created, representations are immutable" but `put-request-flow` Step 3 stores new representations that appear to replace existing ones. Is this via version identity (new :xt/id) or soft-deletes of old variants? What does "existing-representation" mean in the decision point?

5. **Resource Locator Matching Priority**  
   - If multiple resource locators match the same URI, the error is 500 "multiple resource locators matched". But the location flow shows alternatives tried sequentially: exact URI lookup, then OpenAPI, then locators, then redirect, then default. Is the first match taken or is "multiple matches" an error only for locators specifically?

6. **Template Model Resolution**  
   - `template-model` can be a symbol, string URI, or map. For symbols, is the resolution static (defined in config) or dynamic (datalog query)? For string URIs, are they resource URIs resolved via the same resource location logic?

7. **Error Recovery Precedence**  
   - `error-recovery-strategies` defines retry-with-backoff, graceful-degradation, circuit-breaker, and error-propagation. Which strategy applies to which error surface, and what is the precedence if multiple could apply?

---

## Concerns

### Architectural & Design Issues

#### 1. **Implicit Trust in Function References** (Medium Concern)
   - **Issue:** `put-fn`, `post-fn`, `patch-fn`, `delete-fn`, `locator-fn`, and trigger `action` are stored as symbol or value-object references but resolution and invocation is unspecified.
   - **Location:** core_domain.allium (Resource entity), data_flows.allium (Step 11 method dispatch), error_handling.allium (handler-function-not-found recovery)
   - **Risk:** If function references are resolved via dynamic import/eval without namespacing, malicious or accidental overwrites could redirect requests. No spec for how Clojure symbols are validated as callable.
   - **Recommendation:** Specify allowed function namespaces and whitelist at runtime; validate function signatures at resource definition time.

#### 2. **Datalog Pattern Validation Gap** (Medium Concern)
   - **Issue:** Authorization rules store Datalog patterns as `:datalog-pattern` but no validation is performed at rule definition time. Step 9 evaluates patterns that may have syntax errors, unbound variables, or missing entity references.
   - **Location:** data_model.allium (rule-target-is-datalog-query rule), data_flows.allium (Step 9), error_handling.allium (query-evaluation-error)
   - **Risk:** Malformed rules silently match nothing (deny-by-default) or throw 500. No spec for query compilation/caching or query cost limits (potential DoS via complex recursive queries).
   - **Recommendation:** Pre-compile and validate Datalog rules at load time; reject rules with unbound variables; add query cost budget enforcement.

#### 3. **ETag Computation Dependencies Underspecified** (Medium Concern)
   - **Issue:** `core_domain.allium` rule "etag-identifies-representation" says ETags are "computed from representation.content, representation.content-type, representation.content-encoding" but the computation algorithm is not specified.
   - **Location:** core_domain.allium (line 185), data_model.allium (line 53), data_flows.allium (PUT Step 3, line 276)
   - **Risk:** Different implementations may produce different ETags for the same content, breaking conditional request semantics. No hash function specified (MD5, SHA-256?), no handling of binary vs. text content.
   - **Recommendation:** Specify ETag computation: e.g., "SHA-256(content-bytes) + content-type + encoding, base64-encoded" or use opaque format. Document immutability guarantee.

#### 4. **Content Negotiation Fallback Chain Unclear** (Low Concern)
   - **Issue:** If no representation matches client's Accept preferences (406 Not Acceptable), the spec doesn't define fallback behavior. Step 7 (content-negotiation) and error surface "no-acceptable-representation" both exist but recovery strategy is "server adds representation or client changes preference".
   - **Location:** api_behaviour.allium (not-acceptable contract), data_flows.allium (Step 7), error_handling.allium (graceful-degradation strategy)
   - **Risk:** Ambiguity in whether to return default representation, 406, or attempt content transformation.
   - **Recommendation:** Clarify: return 406 and require client to adjust; or return default representation (application/json?) with Vary header.

#### 5. **Template Rendering Failure Isolation** (Low Concern)
   - **Issue:** If template rendering fails (Step 3 of get-request-flow), error-recovery-strategy "graceful-degradation" suggests fallback to stored content, but this only applies if `representation.content` exists alongside `representation.template`.
   - **Location:** data_flows.allium (get-request-flow Step 3), error_handling.allium (template-rendering-error, graceful-degradation)
   - **Risk:** If representation has only template (no fallback content), 500 is inevitable. No spec for partial rendering or template caching.

#### 6. **Transaction Isolation and Concurrent PUT Handling** (Low Concern)
   - **Issue:** `put-request-flow` Step 3 calls `submit-xtdb-transaction` but doesn't specify what happens if two concurrent PUTs arrive for the same resource URI. XTDB's transaction semantics allow concurrent transactions, but whether existing representations are atomically replaced is unclear.
   - **Location:** data_flows.allium (put-request-flow Step 3), data_model.allium (representation-variants contract)
   - **Risk:** Race condition: two clients could both succeed in creating new representations, violating "representation-is-immutable" if they both claim to be the new current version.
   - **Recommendation:** Specify: use XTDB match precondition in transaction, or use :xt/valid-time to version representations explicitly.

---

### Behavioral Inconsistencies

#### 7. **Authorization Default is Deny, but No Allow Rule Exists** (Medium Risk)
   - **Issue:** Step 9 (authorization-evaluation) states "else (authorization = :deny)" but doesn't specify what happens when no rules are defined at all. Is anonymous access denied by default? Is this a security feature or a configuration gap?
   - **Location:** data_flows.allium (Step 9, lines 116-122), external_contracts.allium (authorization-decisions contract)
   - **Implication:** If no rules are loaded, all requests are denied. This is defensible but should be explicit in configuration validation.

#### 8. **Precondition Evaluation Order Missing for HEAD Requests** (Low Concern)
   - **Issue:** RFC 7232 Section 6 evaluation precedence is defined for general requests, but HEAD is specified as "identical to GET except body omitted". Does this include identical precondition order?
   - **Location:** api_behaviour.allium (HEAD-method contract, line 25-29), data_flows.allium (Step 8, line 81)

#### 9. **Response Status Ambiguity for POST Handlers** (Low Concern)
   - **Issue:** `post-request-flow` Step 2 invokes `resource.post-fn` and Step 3 returns "Handler determines response". But if post-fn doesn't return a valid Ring response, error handling is unspecified. Likely a 500, but no explicit rule.
   - **Location:** data_flows.allium (post-request-flow, lines 305-315), error_handling.allium (handler-function-not-found)

---

## Security Vulnerabilities

### Dependency Scanning Results

**Direct Dependencies:** 0 vulnerabilities reported by clj-watson  
**Transitive Dependencies:** 0 vulnerabilities reported

**Note:** This is a Clojure project scanned at the library/namespace level. The scanner reports zero CVEs, suggesting all declared dependencies are at patched versions or the vulnerability database may not cover internal Clojure dependencies comprehensively.

### Implicit Security Assumptions

The specification makes security-critical assumptions that should be validated:

#### **Password Storage** ✓ Well-Specified
- **Spec:** data_model.allium (password-encryption contract): "Passwords are stored using bcrypt encryption, never in plaintext"
- **Status:** Secure. Bcrypt with proper cost factor is industry standard.
- **Dependency:** Requires trusted bcrypt library (referenced in external_contracts.allium as `crypto.password.bcrypt`)
- **Action Required:** Verify bcrypt cost factor ≥ 12; monitor for bcrypt library updates.

#### **Authentication Token Validation** ⚠ Partially Specified
- **Spec:** external_contracts.allium (authentication-service contract): "JWT tokens verified against signing key"
- **Gaps:** 
  - No signature algorithm specified (RS256? HS256? Algorithm negotiation?)
  - No key rotation strategy
  - No token expiration handling specified beyond error surface (invalid-token recovery says "Client must obtain new valid token")
  - No handling of token in request body, only Authorization header and Cookie
- **Risk:** Token validation bypass if algorithm negotiation is enabled (e.g., allow "none" algorithm)
- **Action Required:** Enforce specific algorithm (RS256 preferred); disable algorithm negotiation; implement key rotation; log token validation failures.

#### **Authorization Rule Injection** ⚠ Potential Concern
- **Spec:** external_contracts.allium (rule-evaluation contract): "Rules are evaluated by matching their target pattern against request context"
- **Concern:** If rule patterns can reference system functions (e.g., via Clojure symbol resolution), malicious rule insertion could execute code.
- **Mitigation in Spec:** Datalog patterns are restricted to entity matching (no function calls), but implementation validation is critical.
- **Action Required:** Audit Datalog evaluator to ensure no code execution paths; whitelist allowed predicates in rule patterns.

#### **Payload Size Limits** ✓ Specified
- **Spec:** api_behaviour.allium (payload-size-validation): "Default max 16MB unless ::http/max-content-length specified"
- **Potential Issue:** 16MB default may be excessive for some deployments; no spec for per-resource limits.
- **Action Required:** Make limit configurable per resource; monitor for slow POST/PUT handling; add streaming support for large payloads.

#### **Template Injection** ⚠ Design Risk
- **Spec:** data_flows.allium (get-request-flow Step 3): Template rendering invoked with `representation.template`
- **Risk:** If template content is user-supplied (e.g., via PUT) and Selmer allows code execution, this is a template injection vulnerability.
- **Mitigation in Spec:** No mention of sandboxing or escaping; Selmer is a macro-based template engine with custom filter support.
- **Action Required:** Audit Selmer usage; disable dangerous filters (no exec, system calls); validate template syntax at PUT time; consider sandboxing template evaluation.

#### **Redirect Injection** ⚠ Design Risk
- **Spec:** data_flows.allium (Step 3): "response-header Location resource.location"
- **Concern:** If resource.location is user-supplied, open redirect attacks are possible.
- **Mitigation:** Not specified in spec. Likely handled at application boundary.
- **Action Required:** Validate redirect targets (internal URIs only or whitelist external domains); log all redirects; use 303 or 307 instead of 302 to prevent caching.

---

## Potential Bugs

### Behavioral Inconsistencies and Spec-to-Implementation Risks

#### 1. **Representation Variant Selection Ambiguity**
- **Symptom:** `data_flows.allium` Step 7 (content-negotiation) collects all current representations and picks one. But `data_model.allium` "multiple-variants contract" says representations have `variant-of` pointing to resource URI. What if a representation has no `variant-of`? Is it still current?
- **Likely Bug:** If `variant-of` is optional, some representations may be orphaned or permanently selected.
- **Recommendation:** Clarify: is `variant-of` required? Or is immutability per-representation, so "current" means "latest by transaction-time"?

#### 2. **ETag Matching with Wildcard in If-Match**
- **Symptom:** RFC 7232 says `If-Match: *` means "only succeed if resource exists". But `api_behaviour.allium` (if-match-validation) and error surface (if-match-failed) don't mention wildcard semantics.
- **Data Flow Risk:** Step 8 (precondition-evaluation) calls `evaluate-if-match` but doesn't handle `*` specifically. If implementation treats `*` as a literal etag string, precondition silently fails.
- **Recommendation:** Explicitly handle `If-Match: *` case in precondition-evaluation step.

#### 3. **Authentication Subject Binding Leak**
- **Symptom:** Step 5 (authentication) says "when request.method != :options" but Step 6 (locate-representations) happens regardless. If authentication fails, `subject = nil`. But Step 9 (authorization) still evaluates rules with nil subject context. What rules match nil?
- **Likely Bug:** Unauthenticated requests might match rules intended for authenticated users, or vice versa, depending on rule specificity.
- **Recommendation:** Add explicit unauthenticated-denial rule (e.g., `[[subject :authenticated false]] -> :deny`) and test rule-matching with nil subject.

#### 4. **Trigger Execution Order and Fault Isolation**
- **Symptom:** Step 12 (evaluate-triggers) collects matched triggers, then Step 13 executes them. But if a trigger action throws or modifies response state (unlikely per spec), the order and failure semantics are unspecified.
- **Risk:** Triggers could silently fail or affect each other.
- **Recommendation:** Specify: triggers execute sequentially or in parallel; if one fails, others continue; failed triggers are logged but don't fail the request.

#### 5. **Redirect Response Status Code Ambiguity**
- **Symptom:** Step 3 (redirect-detection) says response-status is `(if :get|:head "302 Found" "307 Temporary Redirect")` but RFC 7231 semantics differ: 302 allows method change, 307 preserves method. Choosing by method is inverted—should be GET/HEAD use 303 (see other), others use 307.
- **Implementation Bug:** Clients expecting 307 (preserve POST) may get 302 (allow POST->GET conversion).
- **Recommendation:** Use 303 (See Other) for GET-like redirects, 307 for others; or document the intentional divergence.

#### 6. **Content-Length Validation Missing for GET/DELETE**
- **Symptom:** Step 10 (receive-request-payload) only applies "when request.method in [:put :post]". But GET or DELETE with Content-Length and body is technically allowed (ignored per spec). However, no step explicitly rejects GET/DELETE bodies.
- **Risk:** If a client sends GET with body and Content-Length, implementation may buffer unnecessary data.
- **Recommendation:** Explicitly reject requests with unexpected bodies; or clarify that bodies in GET/DELETE are silently ignored.

---

## Recommendations

### Priority 1: Must Address Before Production

1. **Pre-compile and Validate Datalog Authorization Rules**
   - **Action:** Add rule validation step at system start; reject rules with unbound variables, syntax errors, or excessive complexity.
   - **Spec Location:** data_model.allium (rule-entity contract)
   - **Impact:** Prevents authorization bypass via malformed rules; improves observability.
   - **Effort:** Medium (requires Datalog parsing/validation library integration)

2. **Specify and Enforce ETag Computation Algorithm**
   - **Action:** Define: `SHA-256(content-bytes || content-type || content-encoding), base64-url-encoded`
   - **Spec Location:** core_domain.allium (line 185), data_flows.allium (put-request-flow Step 3)
   - **Impact:** Ensures consistency across restarts, CDNs, and distributed instances.
   - **Effort:** Low

3. **Add Template Injection Prevention**
   - **Action:** Audit Selmer usage; disable code-execution filters; validate template syntax at representation PUT time; document dangerous features.
   - **Spec Location:** data_flows.allium (get-request-flow Step 3, line 223), external_contracts.allium (template-rendering)
   - **Impact:** Prevents template-based RCE if templates are user-supplied.
   - **Effort:** Medium (audit + testing)

4. **Clarify and Fix Redirect Status Codes**
   - **Action:** Align with RFC 7231: use 303 for GET/HEAD redirects, 307 for others; or document intentional divergence.
   - **Spec Location:** data_flows.allium (step-3-redirect-detection, line 32)
   - **Impact:** Prevents client-side method mutation bugs.
   - **Effort:** Low

5. **Enforce Function Namespace Whitelisting**
   - **Action:** Add constraint: `put-fn`, `post-fn`, etc. must be symbols in whitelisted namespaces. Validate at resource definition time.
   - **Spec Location:** core_domain.allium (Resource entity)
   - **Impact:** Prevents handler function injection attacks.
   - **Effort:** Medium

### Priority 2: Should Address Before Production

6. **Implement Token Signature Algorithm Enforcement**
   - **Action:** Require RS256 or HS256 with HMAC-SHA256; disable algorithm negotiation; implement key rotation.
   - **Spec Location:** external_contracts.allium (token-management contract, line 172)
   - **Impact:** Prevents JWT algorithm-downgrade attacks.
   - **Effort:** Low

7. **Add Explicit Unauthenticated User Authorization Rule**
   - **Action:** Define system rule: `[subject :authenticated false] -> :deny` unless explicitly allowed.
   - **Spec Location:** data_flows.allium (Step 9), external_contracts.allium (authorization-decisions)
   - **Impact:** Clarifies default-deny semantics for unauthorized users.
   - **Effort:** Low

8. **Validate Redirect Targets to Prevent Open Redirects**
   - **Action:** Whitelist internal URIs or domains; reject redirects to arbitrary URLs; add logging.
   - **Spec Location:** data_flows.allium (Step 3-redirect-detection)
   - **Impact:** Prevents phishing attacks via redirect chains.
   - **Effort:** Low

9. **Clarify Representation Variant-of Semantics**
   - **Action:** Update spec: is `variant-of` required? How are orphaned representations handled? Is "current" by transaction-time or explicit flag?
   - **Spec Location:** data_model.allium (representation-variants contract, line 33)
   - **Impact:** Prevents subtle bugs in variant selection.
   - **Effort:** Low (spec clarification)

10. **Handle If-Match Wildcard Explicitly**
    - **Action:** Ensure `If-Match: *` succeeds if resource exists, fails if not (per RFC 7232).
    - **Spec Location:** data_flows.allium (Step 8, precondition-evaluation)
    - **Impact:** Ensures RFC 7232 compliance; prevents false positives/negatives in conditional requests.
    - **Effort:** Low

### Priority 3: Nice-to-Have Improvements

11. **Implement Query Cost Budgeting for Datalog Rules**
    - **Action:** Add query cost estimation and limits to prevent DoS via complex recursive queries.
    - **Spec Location:** data_flows.allium (Step 9, authorization-evaluation)
    - **Impact:** Improves resilience under adversarial rule definitions.
    - **Effort:** High

12. **Add Streaming Support for Large Payloads**
    - **Action:** Allow PUT/POST of payloads > 16MB via chunked encoding; store in blob storage.
    - **Spec Location:** api_behaviour.allium (payload-size-validation), data_flows.allium (Step 10)
    - **Impact:** Enables large file uploads; reduces memory pressure.
    - **Effort:** High

13. **Document Trigger Execution Guarantees**
    - **Action:** Specify: trigger ordering, atomicity (all-or-nothing vs. best-effort), error handling, and side effects.
    - **Spec Location:** data_flows.allium (Step 12, evaluate-triggers), external_contracts.allium
    - **Impact:** Clarifies semantics for trigger-based workflows.
    - **Effort:** Medium

14. **Add Graceful Degradation Examples for Content Negotiation**
    - **Action:** Document fallback behavior when no representation matches; provide configuration options.
    - **Spec Location:** api_behaviour.allium (not-acceptable contract), error_handling.allium (graceful-degradation)
    - **Impact:** Improves developer experience and resilience.
    - **Effort:** Low

---

## Summary

### Security Posture
- **Vulnerability Status:** ✓ No known CVEs in declared dependencies
- **Cryptography:** ✓ Bcrypt password hashing is well-specified and secure
- **Implicit Risks:** ⚠ Authentication algorithm negotiation, template injection, redirect injection not addressed in spec
- **Recommendations:** Enforce RS256/HS256 tokens, sandbox Selmer templates, whitelist handler functions and redirect targets

### Architectural Quality
- **Strengths:** Bitemporal XTDB storage, ACID transactions, Datalog-driven fine-grained authorization
- **Weaknesses:** Datalog rule validation gaps, ETag computation underspecified, function reference trust implicit
- **Recommendations:** Pre-compile and validate rules, specify ETag algorithm, enforce namespace whitelisting

### Specification Completeness
- **Coverage:** Comprehensive; all major request flows, data models, and error paths documented
- **Clarity Issues:** 7 open questions identified; 9 behavioral inconsistencies and potential bugs noted
- **Recommendations:** Clarify trigger execution, representation variant-of semantics, authentication subject binding, precondition evaluation for wildcard If-Match

### Deployment Readiness
- **Pre-Prod Requirements:** Address Priority 1 items (rules validation, ETag algorithm, template injection prevention, redirect codes, function whitelisting)
- **Pre-Prod Nice-to-Have:** Address Priority 2 items (token enforcement, unauthenticated rule, redirect validation, variant semantics)
- **Post-Prod:** Address Priority 3 items as operational needs arise

---

**Report Generated:** 2026-05-01  
**Spec Files Analyzed:** 6 (core_domain, data_model, api_behaviour, data_flows, error_handling, external_contracts)  
**Total Recommendations:** 14 (5 critical, 5 important, 4 enhancements)
