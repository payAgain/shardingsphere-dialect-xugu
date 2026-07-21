# Release

## Version

Maven / GitHub tag coordinate: **`5.5.3-xugu`** / **`v5.5.3-xugu`**.

## Build & package

```bash
mvn -DskipITs clean package
# Collect module jars (+ proxy-dialect POM) into dist/ and zip:
# shardingsphere-dialect-xugu-5.5.3-xugu-jars.zip
```

Asset names must stay stable for consumers (see existing GitHub Release).

## Publish channel

- Default: GitHub Release assets on `payAgain/shardingsphere-dialect-xugu`
- Packages/Central: optional; do not claim published unless credentials succeed

## Notes

- Update `docs/RELEASE-NOTES-5.5.3-xugu.md` when republishing.
- Process is Trellis-driven (task → build → tag/release); do not use Superpowers plans as the runner.
