# PRD: Fix release ZIP parent POM and consumer docs

## Goal

Address black-box acceptance findings (`TEST-REPORT.md` §7): Release ZIP missing parent POM, incomplete Quick Start deps, misleading Proxy MySQL docs, JDBC GAV drift notes, COUNT alias / multi-port topology guidance. Rebuild and republish GitHub Release `v5.5.3-xugu` assets.

## Acceptance

1. ZIP includes `shardingsphere-dialect-xugu-parent-5.5.3-xugu.pom` + per-module POMs + JARs.
2. `scripts/install-release-assets.ps1` can install without hand-adding parent.
3. Quick Start / Proxy / support-matrix / RELEASE notes updated per findings.
4. GitHub Release `v5.5.3-xugu` assets and notes refreshed from current `main`.

## Out of scope

- Maven Central / Packages publish
- Changing Maven version coordinate (stays `5.5.3-xugu`)
- Dialect code changes for COUNT(*) merge behavior
