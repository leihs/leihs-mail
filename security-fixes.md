# Security Fixes

**Date:** 2026-02-19

## Summary

Updated `rack` from **3.1.19** to **3.2.5** and `nokogiri` from **1.18.10** to **1.19.1** to resolve 3 Dependabot security alerts.

## Resolved Alerts

### #116 – CVE-2026-22860: Directory Traversal via Rack::Directory (High)

- **Severity:** CVSS 7.5 (High)
- **Component:** `Rack::Directory`
- **Issue:** The path check used a string prefix match on the expanded path. A request like `/../root_example/` could escape the configured root if the target path starts with the root string, allowing directory listing outside the intended root.
- **Fixed in:** rack 2.2.22, 3.1.20, 3.2.5
- **Reference:** https://github.com/rack/rack/security/advisories/GHSA-7wqh-767x-r66v

### #117 – CVE-2026-25500: Stored XSS in Rack::Directory via `javascript:` Filenames (Moderate)

- **Severity:** CVSS 5.4 (Medium)
- **Component:** `Rack::Directory`
- **Issue:** `Rack::Directory` generates an HTML directory index where each file entry is rendered as a clickable link. If a file exists on disk whose basename starts with the `javascript:` scheme (e.g. `javascript:alert(1)`), the generated index contains an anchor whose `href` attribute is exactly `javascript:alert(1)`. Clicking the entry executes arbitrary JavaScript in the browser.
- **Fixed in:** rack 2.2.22, 3.1.20, 3.2.5
- **Reference:** https://github.com/rack/rack/security/advisories/GHSA-whrj-4476-wvmp

### #1 – GHSA-wx95-c6cv-8532: Nokogiri does not check return value from xmlC14NExecute (Moderate)

- **Severity:** Moderate
- **Component:** `nokogiri` (vendored libxml2)
- **Issue:** Nokogiri's vendored libxml2 did not check the return value from `xmlC14NExecute`, which was identified as a contributing cause to the ruby-saml vulnerability GHSA-x4h9-gwv3-r4m4.
- **Fixed in:** nokogiri >= 1.19.1
- **Reference:** https://github.com/sparklemotion/nokogiri/security/advisories/GHSA-wx95-c6cv-8532

## Changes

| Gem | Previous Version | New Version |
|------|-----------------|-------------|
| rack | 3.1.19 | 3.2.5 |
| rails | 7.2.2.2 | 7.2.3 |
| rackup | 2.2.1 | 2.3.1 |
| nokogiri | 1.18.10 | 1.19.1 |
