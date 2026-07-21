# Testing

## Layers

| Layer | Meaning |
|-------|---------|
| Unit | Module `src/test` — SPI loaders, mappers, no DB |
| L0–L1 IT | `tests-it` — reachability + dialect/sharding under NONE |
| L2 | Multi-DS / RO routing on lab topology |
| L3 | XuGu cluster HA — out of dialect gate unless scoped |

## Commands

```bash
mvn -DskipITs test
mvn -pl tests-it -am verify   # when lab DB available
```

## Anti-patterns

- IT that only passes under MYSQL/ORACLE compatible modes.
- Claiming replica SLA from same-host different-DATABASE asserts.
