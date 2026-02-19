# Security Fixes – Rack Gem

## Summary

Updated `rack` from **3.1.19** to **3.2.5** to address two vulnerabilities in `Rack::Directory`.

## Vulnerabilities

### CVE-2026-22860 – Directory Traversal via Rack::Directory (High)

- **Severity:** CVSS 7.5 (High)
- **Component:** `Rack::Directory`
- **Issue:** The path check used a string prefix match on the expanded path. A request like `/../root_example/` could escape the configured root if the target path starts with the root string, allowing directory listing outside the intended root.
- **Fixed in:** rack 2.2.22, 3.1.20, 3.2.5
- **Reference:** https://github.com/rack/rack/security/advisories/GHSA-7wqh-767x-r66v

### CVE-2026-25500 – Stored XSS in Rack::Directory via `javascript:` Filenames (Moderate)

- **Severity:** CVSS 5.4 (Medium)
- **Component:** `Rack::Directory`
- **Issue:** `Rack::Directory` generates an HTML directory index where each file entry is rendered as a clickable link. If a file exists on disk whose basename starts with the `javascript:` scheme (e.g. `javascript:alert(1)`), the generated index contains an anchor whose `href` attribute is exactly `javascript:alert(1)`. Clicking the entry executes arbitrary JavaScript in the browser.
- **Fixed in:** rack 2.2.22, 3.1.20, 3.2.5
- **Reference:** https://github.com/rack/rack/security/advisories/GHSA-whrj-4476-wvmp

## Changes

| Gem | Previous Version | New Version |
|------|-----------------|-------------|
| rack | 3.1.19 | 3.2.5 |
| rails | 7.2.2.2 | 7.2.3 |
| rackup | 2.2.1 | 2.3.1 |
