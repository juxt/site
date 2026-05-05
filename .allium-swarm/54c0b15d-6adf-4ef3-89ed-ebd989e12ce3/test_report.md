# Test Project Classification Report

## juxt-site (main)

**Classification:** CONDITIONALLY SAFE

**Justification:** In-memory XTDB database and HTTP handler invocations are fully isolated within test fixtures; requires Java/Clojure tooling setup.

**Setup Requirements:**
- Clojure CLI (tools.deps) installed
- OpenJDK 11+ (CircleCI uses 11, Docker uses 17)
- Maven cache (~/.m2/repository, ~/.gitlibs, ~/.deps.clj)
- Framework: clojure.test + kaocha
- Run: `make test` or `clojure -M:test -m kaocha.runner`

---

## opt/insite

**Classification:** SAFE

**Justification:** Jest unit tests with React Testing Library; no side effects, completely isolated from external systems.

**Setup Requirements:**
- Node.js 12+
- npm or yarn
- Run: `npm test -- --watchAll=false`

---

## opt/insite-console

**Classification:** SAFE

**Justification:** Jest framework configured but no test files present; safe to run (trivial pass).

**Setup Requirements:**
- Node.js 12+
- npm or yarn
- Run: `npm test -- --watchAll=false`

---

## docs

**Classification:** SAFE

**Justification:** Linting and type checking only (prettier, eslint, TypeScript); read-only analysis with no side effects.

**Setup Requirements:**
- Node.js 12+
- npm
- Run: `npm run ci-check` (runs prettier:diff, lint, and tsc in parallel)

---

## opt/graphiql

**Classification:** SAFE

**Justification:** No test files present; safe to run.

**Setup Requirements:**
- Node.js 12+
- npm or yarn

---

## Summary

| Project | Classification | Risk Level |
|---------|----------------|-----------|
| juxt-site (main) | CONDITIONALLY SAFE | Low (isolated, no external services) |
| opt/insite | SAFE | None |
| opt/insite-console | SAFE | None |
| docs | SAFE | None |
| opt/graphiql | SAFE | None |

**Overall:** 4/5 projects are fully safe to run. The juxt-site project is conditionally safe pending availability of Java/Clojure tooling and Maven cache.
