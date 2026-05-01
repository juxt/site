# Findings: juxt/site vs site.allium

10 findings.

## 1. [high] logout-response does not invalidate the server-side session

- **Classification:** likely_bug
- **Location:** `src/juxt/pass/alpha/authentication.clj:210`
- **Spec references:** RequestsAreAuthorized, DenyByDefault

logout-response does not invalidate the server-side session — the access_token remains valid in sessions-by-access-token until natural expiry, so a logged-out cookie copied elsewhere still authenticates.

**Suggested fix:** On logout, dissoc the access_token from sessions-by-access-token (and any cookie-derived token) before clearing the cookie.

## 2. [high] login-response performs an unvalidated redirect

- **Classification:** likely_bug
- **Location:** `src/juxt/pass/alpha/authentication.clj:136`
- **Spec references:** RequestsAreAuthorized

login-response performs an unvalidated redirect to a URL extracted from the posted form's _query.redirect field — classic open-redirect that can be abused for phishing after credential submission.

**Suggested fix:** Whitelist redirect targets (same-origin or explicit allowlist) before issuing the 302 Location.

## 3. [high] In-memory session atom contradicts the XTDB-backed spec

- **Classification:** brittle_code
- **Location:** `src/juxt/pass/alpha/authentication.clj:26`
- **Spec references:** RequestsAreAuthorized, ContentAddressableHistory

Session state lives in an in-memory atom (sessions-by-access-token); restarts wipe all sessions and the design cannot scale beyond a single JVM, which contradicts the XTDB-backed, content-addressable spec.

**Suggested fix:** Persist sessions (or use signed/JWT bearer tokens) so they survive restart and can be shared across nodes.

## 4. [medium] No deny-overrides / priority in rule combining

- **Classification:** logic_ambiguity
- **Location:** `src/juxt/pass/alpha/pdp.clj:40`
- **Spec references:** RequestsAreAuthorized, DenyByDefault

authorization treats 'allowed?' as (pos? matched-rules) AND every-rule-is-allow, but there is no explicit deny-overrides / priority rule combination — a matched ::pass/deny effect simply causes the same outcome as no match, so the spec's Pass policy semantics around deny precedence are undefined.

**Suggested fix:** Either drop ::pass/deny from the model or implement an explicit rule-combining algorithm (deny-overrides, first-applicable, etc.) and document it in the spec.

## 5. [medium] OPTIONS bypasses authn/authz, leaks resource metadata

- **Classification:** likely_bug
- **Location:** `src/juxt/site/alpha/handler.clj:472`
- **Spec references:** DenyByDefault

wrap-authenticate and wrap-authorize both short-circuit for method=:options, so a CORS preflight surfaces Allow / access-control-allow-methods for protected resources without any policy check, leaking the existence and method set of resources the caller cannot otherwise see.

**Suggested fix:** Run authorization for OPTIONS too (or at least gate the Allow/ACAO response on a deny-by-default policy decision).

## 6. [medium] Blanket exception swallow in match-targets

- **Classification:** brittle_code
- **Location:** `src/juxt/site/alpha/rules.clj:42`
- **Spec references:** RequestsAreAuthorized

match-targets wraps the rule evaluation in a blanket try/catch that swallows every Exception and returns []; while this fails closed (deny by default) it also silently masks malformed rules and XTDB query errors with only a log line.

**Suggested fix:** Narrow the catch to known query exceptions and re-throw or surface the error so admin-installed rule corruption is observable.

## 7. [medium] DELETE has no resource-level delete-fn dispatch

- **Classification:** likely_bug
- **Location:** `src/juxt/site/alpha/handler.clj:345`
- **Spec references:** DeleteRemovesResource

DELETE always issues [:xtdb.api/delete uri] without consulting any resource-level delete-fn or pre/postconditions, in contrast to PUT which dispatches via ::site/put-fn — custom resources cannot intercept DELETE and audit/derived state is lost.

**Suggested fix:** Mirror the PUT dispatch and look up ::site/delete-fn on the resource, falling back to the default xtdb delete only when none is configured.

## 8. [medium] PUT-create vs PUT-replace path is undefined

- **Classification:** logic_ambiguity
- **Location:** `src/juxt/site/alpha/handler.clj:289`
- **Spec references:** PutCreatesOrReplacesRepresentation

PUT requires the resource to already be located and to carry a ::site/put-fn, which conflicts with the spec rule PutCreatesOrReplacesRepresentation: a brand-new URI cannot be created via PUT unless the locator manufactures a resource stub for it — the create-vs-replace path is not made explicit anywhere.

**Suggested fix:** Document and (if needed) add a default put-fn for absent resources, or codify in the spec that PUT requires a pre-installed ApiDefinition / resource template.

## 9. [informational] Auth scheme set & precedence still an open question

- **Classification:** business_question
- **Location:** `specs/site.allium:158`
- **Spec references:** RequestsAreAuthorized

Spec leaves the supported authentication mechanisms and their precedence as an open question, but the implementation hard-codes Cookie > Basic > Bearer with OpenID Connect in a separate file — the ratified set and ordering need to be confirmed.

**Suggested fix:** Promote the auth-scheme list and precedence into the spec (entity or rule) once product confirms.

## 10. [low] expire-sessions! sweeps full map on every lookup

- **Classification:** inefficiency
- **Location:** `src/juxt/pass/alpha/authentication.clj:33`
- **Spec references:** (none)

Every session lookup calls expire-sessions! which rebuilds the whole sessions map (O(n) per request); under load this dominates lookup cost and contends on the atom.

**Suggested fix:** Use a TTL cache (e.g. core.cache TTLCacheQ, Caffeine) or expire lazily on individual lookups.
