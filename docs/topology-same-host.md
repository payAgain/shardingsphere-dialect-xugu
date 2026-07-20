# Same-host topology (T3=A / G-006 Q-05a)

> **Release:** 5.5.3-xugu.2（frozen）  
> **Goal:** G-005 T3=A · G-006 Q-05a L2 harden  
> **Lab:** `192.168.2.239:5138` · `SYSDBA` · `compatiblemode=NONE`

This note records what “same-host topology deepen” **does** and **does not** prove.

---

## What is covered

| Layer | Mechanism | Evidence |
|---|---|---|
| Routing | `write_ds` → `baseline_write`; `read_ds_*` → `baseline_read0` / `baseline_read1` on the **same host** | `ReadwriteSplittingIT.sameHostReadDsRoutingIsolation` |
| Privilege deepen | Restricted XuGu user `ss_ro_reader` (SELECT-only via `GRANT SELECT ANY TABLE`) on **read DATABASE URLs only**; write DS stays `SYSDBA` | `ReadwriteSplittingIT.sameHostReadOnlyUserDeepen` |
| Failure / edge (Q-05a) | RO user **INSERT / UPDATE / DELETE / DROP** deny; denied DML leaves no rows; read0≠read1; post-`DROP` cleanup verified; SS write still write-only after RO deny | `ReadwriteSplittingIT.sameHostReadPathFailureAndCleanup` |
| Unqualified names | Read JDBC URLs append `current_schema=SYSDBA` so RO user can `SELECT` SYSDBA-owned baseline tables | `BaselineSupport.ensureReadOnlyUser` |

INSERT/UPDATE/DELETE/DROP through the restricted user on each read URL must fail (XuGu `E18012` privilege denial). SELECT must succeed. ShardingSphere auto-commit SELECT still routes to read DS using that user.

Profiles: **`-Pbaseline`** (full B1–B7, includes deepened B2) and **`-Ptopology`** (B2 / `ReadwriteSplittingIT` only) both cover Q-05a deepen.

---

## What is explicitly NOT claimed

- **Physical read replica** / streaming replication / lag / HA failover  
- Multi-machine or independent storage topology  
- XuGu `DialectDatabasePrivilegeChecker` product surface (still DEFER in the support matrix)  
- That `GRANT SELECT ANY TABLE` equals a vendor “READ ONLY” replica role

T3=A is **same-host only**. Do not cite these ITs as replica isolation evidence.

---

## BLOCKED_ENV (RO user)

If the lab cannot `CREATE USER` / `GRANT` (permission policy, mode, or product limits):

1. `BaselineSupport.ensureReadOnlyUser` sets `topology.readonly.status=BLOCKED_ENV` and falls read DS credentials back to admin.  
2. `sameHostReadOnlyUserDeepen` / `sameHostReadPathFailureAndCleanup` still run the **strongest alternative**: same-host **different-DATABASE** marker isolation (write vs read0/read1, including cross-read absence).  
3. Privilege deepen is **not** claimed for that run; routing isolation from B2/P0-2 remains the honest ceiling.

Observed lab path for this Goal: restricted user **OK** (not BLOCKED_ENV).

---

## Q-05b dual work-node entry — BLOCKED_ENV

| Item | Status |
|---|---|
| Second work-node JDBC URL | **Not provided** |
| Profile | Future `-Pcluster-entry` (P2-1); **not run** |
| Disposition | **BLOCKED_ENV** — does **not** block G-006 Accept |

Q-05a (same-host L2) is sufficient for Accept. Do not invent a second host/URL. When a human supplies ≥2 work-node URLs, enable dual-entry B1 smoke under `-Pcluster-entry` and replace this section.

---

## How to run

```text
# Full baseline (includes deepened B2 + Q-05a harden)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test "-Pbaseline"

# Topology subset only (B2 / Q-05a)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test "-Ptopology"
```

PowerShell: quote `-P` / `-D` flags as shown.
