# Release

## Version

Maven / GitHub tag coordinate: **`5.5.3-xugu`** / **`v5.5.3-xugu`**.

## Build & package

```bash
mvn -DskipITs clean package
# Collect parent POM + module jars/POMs + proxy-dialect POM into dist/ and zip:
powershell -File scripts/package-release-assets.ps1
# → dist/shardingsphere-dialect-xugu-5.5.3-xugu-jars.zip
```

### Assets that MUST be in the ZIP

| File | Why |
|---|---|
| `shardingsphere-dialect-xugu-parent-5.5.3-xugu.pom` | Every module POM inherits this parent; omit → `install-file` cannot resolve the graph |
| `*-5.5.3-xugu.jar` + matching `*.pom` | Prefer `-DpomFile=` install over JAR-embedded POM alone |
| `shardingsphere-proxy-dialect-xugu-5.5.3-xugu.pom` | Proxy aggregate BOM for `ext-lib` |

Consumer install helper: `scripts/install-release-assets.ps1` (parent → JDBC 12.3.6 explicit → modules → proxy POM).

Asset names must stay stable for consumers (see existing GitHub Release). When republishing, refresh tracked `docs/github-release-body.md` (copied to `dist/RELEASE-BODY.md` by the package script) and paste it into the GitHub Release description.

## Publish channel

- Default: GitHub Release assets on `payAgain/shardingsphere-dialect-xugu`
- Packages/Central: optional; do not claim published unless credentials succeed

## Notes

- Update `docs/RELEASE-NOTES-5.5.3-xugu.md` when republishing.
- Process is Trellis-driven (task → build → tag/release); do not use Superpowers plans as the runner.
- JDBC: always install as `com.xugudb:xugu-jdbc:12.3.6` with `-DgeneratePom=true` (embedded metadata may say 12.3.4).
