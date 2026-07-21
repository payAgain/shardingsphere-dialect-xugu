# SPI

## Contracts

- Type string is exactly `"XuGu"`.
- Register every implementation under `src/main/resources/META-INF/services/<interface FQCN>`.
- Prefer `TypedSPILoader` / `DatabaseTypedSPILoader` in tests.

## Anti-patterns

- Returning MySQL/Oracle from `getTrunkDatabaseType()` (native product forbids trunk).
- Implementing SPI without a services file.
- Using `"Xugu"` / `"xugu"` as the typed SPI key instead of `"XuGu"`.
