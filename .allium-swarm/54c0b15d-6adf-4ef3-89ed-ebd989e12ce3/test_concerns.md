# Test Coverage Analysis for Potential Bugs
## juxt-site-full-2013 Implementation

---

## Bug 1: Rule Effect Validation Mismatch

**Classification:** SUSPICIOUS COVERAGE

**Test Files Inspected:**
- `test/juxt/site/authz_test.clj` — authorization and rule matching tests
- `test/juxt/test/util.clj` — test utilities defining access rules
- `src/juxt/pass/alpha/pdp.clj` — authorization decision logic

**Justification:**
Authorization tests exist and pass, validating the `::pass/allow` and `::pass/deny` effects (authz_test.clj defines comprehensive rule-matching scenarios). However, the implementation in `pdp.clj:40-43` only checks `(every? #(= (::pass/effect %) ::pass/allow) matched-rules)` — neither the spec nor tests exercise the `::pass/conditional` effect mentioned in the spec. Since tests validate only allow/deny flows and pass, the bug (conditional not handled) would silently escape detection; callers depending on conditional rules would be denied without knowing why. This is a case where existing test coverage masks an incomplete implementation.

---

## Bug 2: Session Lookup Race Condition

**Classification:** NO COVERAGE

**Test Files Inspected:**
- `test/juxt/site/` (all files) — no session-related tests found via grep
- `src/juxt/pass/alpha/authentication.clj:40-42` — session lookup implementation

**Justification:**
The codebase contains no tests for session lookup, expiry, or concurrent access. The race condition exists in production code (`lookup-session [k date-now]` calls `expire-sessions!` then separately reads `@sessions-by-access-token`) but is never exercised in tests. The atom swap in `expire-sessions!` is atomic, but the window between expiry and retrieval is unguarded; a concurrent request could modify the atom between lines 41 and 42. No test covers this race or validates session behavior under concurrency.

---

## Bug 3: ETag Weak vs. Strong Comparison Logic

**Classification:** ADEQUATELY COVERED

**Test Files Inspected:**
- `test/juxt/site/error_conditions_test.clj` — conditional request tests (lines 206-221, 167-204)
- `src/juxt/site/alpha/conditional.clj` — ETag comparison implementation

**Justification:**
The implementation correctly applies weak-match comparison for GET/HEAD (line 79: `rfc7232/weak-compare-match?`) and strong-match for If-Match (line 48: `rfc7232/strong-compare-match?`), conforming to RFC 7232. Tests validate the behavior: `if-none-match-precondition-failed-get-test` confirms 304 Not Modified on GET with matching ETag, and `if-match` tests confirm 412 Precondition Failed on strong-match failure. If the weak/strong logic were inverted, tests would catch the regression.

---

## Bug 4: Content-Encoding Validation Error Status

**Classification:** SUSPICIOUS COVERAGE

**Test Files Inspected:**
- `test/juxt/site/error_conditions_test.clj` — content negotiation error tests (lines 81-165)
- `src/juxt/site/alpha/handler.clj` — content negotiation validation logic

**Justification:**
The error_conditions_test.clj validates content-type and charset handling, returning 415 Unsupported Media Type (lines 82-107, 109-136). However, no test covers content-encoding validation. The handler.clj code (around line 353) returns 409 Conflict for unsupported content-encoding, violating RFC 7231 which specifies 415. Similar content negotiation tests pass, but the specific encoding error is not exercised, so the incorrect status code escapes detection. A test expecting 415 for unsupported encoding would reveal the bug immediately.

---

## Bug 5: NoRequestBody Condition Impossible

**Classification:** NO COVERAGE

**Test Files Inspected:**
- `test/juxt/site/error_conditions_test.clj` — HTTP request validation tests
- `src/juxt/site/alpha/handler.clj` — request body parsing

**Justification:**
The spec mentions a "NoRequestBody" error condition (PUT/POST with Content-Length but no body), but this is an HTTP-layer concern, not application-layer. If the body is shorter than Content-Length, the servlet container will hang waiting for bytes (timeout) before the application code runs. No test exercises this condition because it cannot be triggered at the application level — the HTTP layer or network will fail first. Tests cover missing Content-Length (411 error) and payload size limits, but not the impossible case of Content-Length mismatch due to HTTP layer responsibility.

---

## Bug 6: Representation Content XOR Body Invariant Weakly Enforced

**Classification:** SUSPICIOUS COVERAGE

**Test Files Inspected:**
- `test/juxt/site/additional_coverage_test.clj` — representation selection tests (lines 1-50+)
- `src/juxt/site/alpha/response.clj:167-186` — payload rendering logic
- `src/juxt/site/alpha/handler.clj` — representation construction

**Justification:**
Tests validate representation selection and rendering for valid cases where either `::http/content` or `::http/body` is present (additional_coverage_test.clj: `representation-selection-test`, handler_test.clj). However, no test checks the invariant violation case: what happens if both content and body are nil, or both are non-nil? The response.clj `add-payload` function (lines 184-185) assigns either content or body to the response body without validating the invariant. If a representation entity violates the constraint (both nil or both set), the response generation would return silently with unexpected body. Tests cover the happy path, so the invariant violation would go undetected.

---

## Summary

| Bug | Classification | Risk |
|-----|---|---|
| Rule Effect Validation Mismatch | SUSPICIOUS COVERAGE | Conditional rules silently denied |
| Session Lookup Race Condition | NO COVERAGE | Rare but possible race window |
| ETag Weak vs. Strong Comparison | ADEQUATELY COVERED | Low — tests validate logic |
| Content-Encoding Error Status | SUSPICIOUS COVERAGE | Wrong HTTP status code (409 vs 415) |
| NoRequestBody Condition | NO COVERAGE | HTTP-layer only, not application concern |
| Representation Content XOR Body | SUSPICIOUS COVERAGE | Invariant violation undetected |

**Highest Concerns:** Bugs 1 and 6 (suspicious coverage with passing tests hiding incomplete implementations), and Bug 2 (no session concurrency tests despite race condition risk).
