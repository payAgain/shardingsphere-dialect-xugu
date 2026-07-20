# G-004 Acceptance — Production hardening P0+P1（同机）

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**HEAD:** `f79af9e`  
**Version:** `5.5.3-xugu.2`  
**Spec:** `sharding/docs/superpowers/specs/2026-07-20-xugu-production-hardening-p0-p1-design.md`  
**Plan:** `sharding/docs/superpowers/plans/2026-07-20-xugu-production-hardening-p0-p1.md`

## Constraints（人类确认）

- 同机拓扑模拟；**不多机 / 不物理副本**
- 客户端进程 kill 允许（XA / fault）
- `compatiblemode=NONE` only

## P0

| ID | Item | Result | Evidence |
|---|---|---|---|
| P0-1 | B1–B7 扩测（happy / boundary / concurrency） | PASS | SHA `74d98c6` · `-Pbaseline` **21→22** tests（含 P0-2 路由用例） |
| P0-2 | 同机不同 DATABASE 读写路由断言 | PASS | SHA `d5523c3` · 非物理副本，见 catalog |
| P0-3 | 支持矩阵 | PASS | `docs/support-matrix.md` · SHA `8284601` |
| P0-4 | 发版 `5.5.3-xugu.2` + release notes | PASS | SHA `3584365` · `docs/RELEASE-NOTES-5.5.3-xugu.2.md`（未 tag/push） |

## P1

| ID | Item | Result | Evidence |
|---|---|---|---|
| P1-1 | XA 恢复证据 | PASS（浅～弱中） | `docs/xa-recovery-evidence.md` · SHA `61e7d74` · `-Pxa-recovery`；超时路径为 GAP；强 recover 未证明 |
| P1-2 | 压测 + 故障注入 | PASS | `docs/perf-fault-report.md` · SHA `f79af9e` · ~365–408 ops/s；池耗尽 32/32 超时；kill 后新连接恢复 |
| P1-3 | 同机第二命名空间 baseline | PASS | `docs/env2-baseline-result.md` · SHA `1d8ec6e` · `-Penv2` **22/22**；非第二机 |
| P1-4 | ExceptionMapper 扩面 | PASS | `docs/error-code-map.md` · SHA `f81ce42` · **20** vendor mappings · 21 unit tests |

## Still out of scope / known limits

- 多机 / 物理只读副本
- PrivilegeChecker / SHOW DAL / 全量 PL/SQL（仍 DEFER）
- XA：prepare 后 TM 崩溃再 `recover()` 未证明；驱动超时设置无效（GAP）
- 受控试点可用；非无限规模生产 hardening

## Commands（lab）

```text
mvn -pl tests-it -am test "-Pbaseline"
mvn -pl tests-it test "-Penv2"
mvn -pl tests-it test "-Pxa-recovery"
mvn -pl tests-it test "-Pperf-fault"
mvn -pl database-exception-xugu -am test
```

## G-004 exit

P0+P1（同机约束）达标 → **ACCEPT**。Stop before Ship（不 push / tag / release，除非人类授权）。
