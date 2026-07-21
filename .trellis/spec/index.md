# shardingsphere-dialect-xugu Spec Index

> Sole managed project for XuGu × ShardingSphere **5.5.3-xugu**. Workflow: Trellis only (no Superpowers / harness).

## Project facts

| Item | Value |
|------|--------|
| GroupId | `com.xugudb.shardingsphere` |
| Version | `5.5.3-xugu` |
| Upstream | `org.apache.shardingsphere:*:5.5.3` |
| Driver | `com.xugudb:xugu-jdbc:12.3.6` |
| Compatible mode | **`NONE` only** |
| `DatabaseType` | `"XuGu"` — **no trunk** (native NONE grammar) |

## Layers

| Layer | Focus |
|-------|--------|
| [modules](./modules/index.md) | Maven module map |
| [spi](./spi/index.md) | META-INF/services + type key |
| [connector](./connector/index.md) | URL / metadata / ResultSet |
| [parser](./parser/index.md) | ANTLR engine + statements |
| [testing](./testing/index.md) | Unit + IT layers |
| [release](./release/index.md) | Build, dist, GitHub Release |
| [quality](./quality/index.md) | Java 8, mocks, naming |
| [guides](./guides/index.md) | Cross-cutting thinking |

## Pre-Development Checklist

1. Confirm change belongs in this plugin repo (not upstream SS fork).
2. Read the relevant layer index above.
3. Keep `compatiblemode=NONE`; do not reintroduce MySQL trunk.
4. Update `docs/support-matrix.md` if capability claims change.

## Quality Check

- Specs cite paths under this repo.
- No frontend/backend template leftovers.
- Release claims match `docs/support-matrix.md` and `docs/RELEASE-NOTES-5.5.3-xugu.md`.
