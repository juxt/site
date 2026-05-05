# NVD Scan Comparison Report
## juxt-site-full-2013 (Clojure)

**Report Generated:** 2026-05-01  
**Scanner:** clj-watson  
**Status:** ⚠️ Unable to perform detailed comparison (pre-upgrade scan data unavailable)

---

## Executive Summary

The pre-upgrade scan results are not available for comparison. However, based on the post-upgrade scan findings (`nvd_upgrade_findings.json`), the scan shows:

- **Total CVEs Post-Upgrade:** 0
- **Status:** ✅ No vulnerabilities detected in direct dependencies

---

## Data Availability

| Scan | Status | Location |
|------|--------|----------|
| Pre-Upgrade Scan | ❌ Missing | `/nvd_scan/` (empty) |
| Post-Upgrade Scan | ✅ Available | `/nvd_scan_after/` (empty, results in nvd_upgrade_findings.json) |

---

## CVEs by Category

### Resolved CVEs
Unknown - pre-upgrade scan not available for comparison.

### Remaining CVEs
None detected post-upgrade.

### Newly Introduced CVEs
None detected post-upgrade (and no pre-upgrade data to establish baseline).

---

## Detailed Analysis

### Post-Upgrade Scan Results

**Total Vulnerabilities:** 0  
**Upgrade Attempts:** 0  
**Successful Upgrades:** 0  
**Failed Upgrades:** 0  
**Skipped Transitive CVEs:** 0  

**Dependencies Scanned (Direct):**
- XTDB
- Ring/Jetty
- XTDB RocksDB
- Selmer (templating)
- Malli (schema validation)
- crypto-password (bcrypt)
- juxt/pick
- juxt/grab
- Integrant
- And others

---

## Important Notes

1. **Limited Scope:** This scan analyzed only direct dependencies. Transitive dependency vulnerabilities were not captured.

2. **Database Lag:** There can be a 0-30 day delay between CVE publication and NVD indexing.

3. **Zero-Day Risk:** Unknown/unpublished vulnerabilities are not detected.

4. **Additional Risks:** The comprehensive analysis report identifies:
   - 6 spec-implied security risks
   - 9 architectural concerns
   - These are unrelated to known CVEs but warrant attention

---

## Recommendations

### Immediate Actions

1. **Run Transitive Dependency Scan:**
   ```bash
   clj -M:clj-watson --recursive
   ```
   This will analyze the full dependency tree including transitive dependencies.

2. **Establish Pre-Upgrade Baseline:**
   - Re-run the pre-upgrade scan with full results exported
   - Store scan results in `/nvd_scan/juxt-site-full-2013.json` for future comparisons

3. **Monitor for New CVEs:**
   Subscribe to CVE updates for key dependencies:
   - XTDB
   - Jetty/Ring
   - RocksDB

### Medium-term Actions

Implement security recommendations from comprehensive analysis:

**P0 (Security-Critical):**
- Add ETag generation algorithm documentation
- Fix Content-Encoding error status (409 → 415)
- Document Speculative DB ID generation strategy
- Add Bcrypt cost factor documentation

**P1 (Architecture):**
- Implement persistent session store
- Add rule validation at write time
- Add rate limiting
- Add CORS headers

**P2 (Robustness):**
- Add template size limits
- Implement authorization failure logging
- Add X-Content-Type-Options: nosniff header
- Document cache invalidation strategy

---

## Next Steps

1. Obtain pre-upgrade scan results for proper comparison
2. Run clj-watson with `--recursive` flag for transitive dependency analysis
3. Re-run scan every 4-6 weeks as new CVEs are published to NVD
4. Address P0 security-critical recommendations

---

**Analysis Tool:** clj-watson NVD scan  
**Confidence Level:** Low (pre-upgrade data missing; post-upgrade data shows zero vulnerabilities)
