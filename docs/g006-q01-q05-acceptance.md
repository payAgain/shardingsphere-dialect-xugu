# G-006 Acceptance — Q-01～Q-05 高影响补全

**Date:** 2026-07-20  
**Branch:** `feat/g006-q01-q05`  
**HEAD:** `25e9f23`  
**Version (frozen):** `5.5.3-xugu`  
**Spec:** `sharding/docs/superpowers/specs/2026-07-20-xugu-q01-q05-completion-design.md`

## Constraints

- `compatiblemode=NONE` · 无异构适配 · 版本不迭代  
- Q-05b **不阻塞** · Q-04 **全部尝试（证据驱动）**  
- 分层策略 L0–L3；L3 真集群 HA 非本 Goal 必过  

## Results

| ID | 目标 | Result | Evidence |
|---|---|---|---|
| Q-02 | XA 超时 | **CLOSED_AS_DEFER** | 驱动桩忽略 `setTransactionTimeout` · SHA `2b108a9` · 应用层规避 |
| Q-03 | Corpus DEFER↓ | **PASS** | 80 例：PASS **77** / DEFER **3** · SHA `276a7f5` |
| Q-04 | DDL+PL/SQL 全做 | **PASS（子集）** | Supported **36** / DEFER **4** · `ddl-plsql-coverage.md` · SHA `fc89de9` |
| Q-05a | 同机 L2 加深 | **PASS** | RO 写拒绝+清理 · SHA `6a42337` · baseline 24 |
| Q-05b | 双工作节点 | **BLOCKED_ENV** | 无第二 URL；不阻塞 Accept |
| Q-01 | XA Strong | **BLOCKED** | prepare 后杀 TM → `recover=0` 无 in-doubt · SHA `25e9f23` · 非假 PASS |

## Regression

| Suite | Result |
|---|---|
| `-Pbaseline` | **24/24** PASS |
| `-Pxa-recovery` | **5/5** PASS |
| Version | `5.5.3-xugu` |

## Still must not claim

- 金融级 XA / TM-log 强恢复（Q-01 BLOCKED）  
- RM XA timeout abort（Q-02 DEFER）  
- 物理副本 / 多工作节点已验证（Q-05b BLOCKED）  
- 全量 WINDOW / 部分 INDEX-VIEW 变体（见 DEFER 清单）  

## G-006 exit

必过项达标；Q-01/Q-05b 诚实 BLOCKED → **ACCEPT**。Stop before Ship/merge。
