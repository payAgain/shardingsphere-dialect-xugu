# Quality

## Baseline

- Java **1.8** (align with SS 5.5.3)
- JUnit 5 + Mockito
- Prefer `final` classes for SPI impls
- No placeholders/TODOs in delivered code

## Anti-patterns

- Bumping `shardingsphere.version` without an explicit task
- Mixing Proxy MySQL-storage backend jars with XuGu backend dialect
