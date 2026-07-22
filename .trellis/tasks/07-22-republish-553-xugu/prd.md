# PRD: Republish v5.5.3-xugu

## Goal

Bootstrap (`00-bootstrap-guidelines`) is complete. Rebuild JARs and **re-publish** GitHub Release `v5.5.3-xugu` so tag tip includes Trellis adoption + archived bootstrap, and assets match a fresh `mvn -DskipITs clean package`.

## Acceptance

1. Tag `v5.5.3-xugu` points at current `main` tip (includes Trellis + bootstrap archive).
2. Release https://github.com/payAgain/shardingsphere-dialect-xugu/releases/tag/v5.5.3-xugu exists (non-draft).
3. Assets include zip + per-module jars/POM (same naming as before).
4. Release notes mention Trellis-managed republish.

## Out of scope

- Maven Central / GitHub Packages publish
- Version coordinate change (stays `5.5.3-xugu`)
