# Same-host topology (T3=A)

> **Release:** 5.5.3-xugu.2  
> **Goal:** G-005 / B-005 Task 3 (T3=A)  
> **Lab:** `192.168.2.239:5138` · `SYSDBA` · `compatiblemode=NONE`

This note records what “same-host topology deepen” **does** and **does not** prove.

---

## What is covered

| Layer | Mechanism | Evidence |
|---|---|---|
| Routing | `write_ds` → `baseline_write`; `read_ds_*` → `baseline_read0` / `baseline_read1` on the **same host** | `ReadwriteSplittingIT.sameHostReadDsRoutingIsolation` |
| Privilege deepen | Restricted XuGu user `ss_ro_reader` (SELECT-only via `GRANT SELECT ANY TABLE`) on **read DATABASE URLs only**; write DS stays `SYSDBA` | `ReadwriteSplittingIT.sameHostReadOnlyUserDeepen` |
| Unqualified names | Read JDBC URLs append `current_schema=SYSDBA` so RO user can `SELECT` SYSDBA-owned baseline tables | `BaselineSupport.ensureReadOnlyUser` |

INSERT through the restricted user on each read URL must fail (XuGu `E18012` privilege denial). SELECT must succeed. ShardingSphere auto-commit SELECT still routes to read DS using that user.

---

## What is explicitly NOT claimed

- **Physical read replica** / streaming replication / lag / HA failover  
- Multi-machine or independent storage topology  
- XuGu `DialectDatabasePrivilegeChecker` product surface (still DEFER in the support matrix)  
- That `GRANT SELECT ANY TABLE` equals a vendor “READ ONLY” replica role

T3=A is **same-host only**. Do not cite these ITs as replica isolation evidence.

---

## BLOCKED_ENV

If the lab cannot `CREATE USER` / `GRANT` (permission policy, mode, or product limits):

1. `BaselineSupport.ensureReadOnlyUser` sets `topology.readonly.status=BLOCKED_ENV` and falls read DS credentials back to admin.  
2. `sameHostReadOnlyUserDeepen` still runs the **strongest alternative**: same-host **different-DATABASE** marker isolation (write vs read0/read1).  
3. Privilege deepen is **not** claimed for that run; routing isolation from B2/P0-2 remains the honest ceiling.

Observed lab path for this Goal: restricted user **OK** (not BLOCKED_ENV).

---

## How to run

```text
# Full baseline (includes deepened B2)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it "-Pbaseline" test

# Topology subset only
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it "-Ptopology" test
```

PowerShell: quote `-P` / `-D` flags as shown.
