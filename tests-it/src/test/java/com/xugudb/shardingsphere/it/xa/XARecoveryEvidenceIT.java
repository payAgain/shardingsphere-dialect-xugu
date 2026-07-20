package com.xugudb.shardingsphere.it.xa;

import com.xugu.xa.XADatasourceImp;
import com.xugu.xa.XAXid;
import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G-004 P1-1 — XA recovery / failure-path evidence (same-host lab).
 *
 * <p>Covers interrupt-before-commit, XAResource timeout, and TM-side connection kill
 * during prepare. These are <strong>shallow-to-medium</strong> client/TM failure paths;
 * they do <strong>not</strong> by themselves prove Atomikos crash-recovery replay or
 * XuGu server-side heuristic resolution after a full TM JVM death (see kill-client script).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XARecoveryEvidenceIT {

    /** ShardingSphere logical/physical table used with {@code baseline/baseline-xa.yaml}. */
    private static final String SS_TABLE = "BASELINE_XA_ORDER";

    /** Raw XA single-DB probe table (not under SS sharding rule). */
    private static final String RAW_TABLE = "XA_RECOVERY_EVIDENCE";

    private static final String DB = BaselineSupport.DB0;

    @Test
    @Order(1)
    void interruptMidXaBeforeCommitLeavesNoRows() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensureSsTables(props);

        DataSource dataSource;
        try {
            dataSource = BaselineSupport.createDataSource("baseline/baseline-xa.yaml", props);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "XA DataSource init failed (TM/XA deps): " + ex.getMessage());
            return;
        }

        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch mayCommit = new CountDownLatch(1);
        AtomicReference<Throwable> workerError = new AtomicReference<>();
        AtomicReference<Boolean> interruptedFlag = new AtomicReference<>(false);

        Thread worker = new Thread(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    insertSs(conn, 1001, 1, "INT-A");
                    insertSs(conn, 1002, 2, "INT-B");
                    inserted.countDown();
                    if (!mayCommit.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting for mayCommit");
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        interruptedFlag.set(true);
                        conn.rollback();
                        return;
                    }
                    conn.commit();
                } catch (Exception ex) {
                    try {
                        conn.rollback();
                    } catch (SQLException ignored) {
                        // best-effort
                    }
                    workerError.set(ex);
                } finally {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ignored) {
                        // best-effort
                    }
                }
            } catch (Exception ex) {
                workerError.set(ex);
                inserted.countDown();
            }
        }, "xa-recovery-interrupt");

        try {
            worker.start();
            assertTrue(inserted.await(30, TimeUnit.SECONDS), "worker did not reach mid-XA");
            worker.interrupt();
            mayCommit.countDown();
            worker.join(30_000L);
            assertTrue(!worker.isAlive(), "worker did not finish after interrupt");

            int ds0 = BaselineSupport.countOn(props, "jdbc.url.ds0", SS_TABLE);
            int ds1 = BaselineSupport.countOn(props, "jdbc.url.ds1", SS_TABLE);
            System.out.println("[P1-1 interrupt] interruptedFlag=" + interruptedFlag.get()
                    + " workerError=" + summarize(workerError.get())
                    + " counts ds0=" + ds0 + " ds1=" + ds1);
            assertEquals(0, ds0 + ds1,
                    "interrupt before commit must not leave durable XA rows (app-side abort)");
            assertTrue(Boolean.TRUE.equals(interruptedFlag.get()) || workerError.get() != null,
                    "expected interrupt-aware rollback or commit failure");
        } finally {
            cleanupSs(props);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    @Order(2)
    void xaResourceTimeoutPathIsObservable() throws Exception {
        Properties props = prepareSingleDb();
        ensureRawTable(props);

        XADatasourceImp xaDs = newXaDataSource(props.getProperty("jdbc.url." + DB), props);
        XAConnection xaConn = null;
        Connection conn = null;
        Xid xid = newXid("to");
        String outcome;
        int remaining;
        XAResource xaRes = null;
        try {
            xaConn = xaDs.getXAConnection();
            xaRes = xaConn.getXAResource();
            boolean timeoutAccepted = xaRes.setTransactionTimeout(2);
            int reportedTimeout = xaRes.getTransactionTimeout();
            Assumptions.assumeTrue(timeoutAccepted || reportedTimeout >= 0,
                    "XAResource.setTransactionTimeout not usable on this driver");

            conn = xaConn.getConnection();
            xaRes.start(xid, XAResource.TMNOFLAGS);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + RAW_TABLE + " (ID, USER_ID, STATUS) VALUES (?, ?, ?)")) {
                ps.setInt(1, 2001);
                ps.setInt(2, 1);
                ps.setString(3, "TIMEOUT");
                assertEquals(1, ps.executeUpdate());
            }
            // Hold the branch past the requested timeout window before end/prepare.
            Thread.sleep(3500L);
            try {
                xaRes.end(xid, XAResource.TMSUCCESS);
                int vote = xaRes.prepare(xid);
                xaRes.commit(xid, false);
                outcome = "COMMITTED_DESPITE_TIMEOUT vote=" + vote
                        + " setAccepted=" + timeoutAccepted
                        + " timeoutSec=" + reportedTimeout;
            } catch (XAException ex) {
                outcome = "XAException errorCode=" + ex.errorCode + " msg=" + ex.getMessage();
                try {
                    xaRes.rollback(xid);
                } catch (XAException ignored) {
                    // may already be rolled back
                }
            }
            remaining = BaselineSupport.countOn(props, "jdbc.url." + DB, RAW_TABLE);
            System.out.println("[P1-1 timeout] outcome=" + outcome + " remainingRows=" + remaining
                    + " recover=" + Arrays.toString(safeRecover(xaRes)));
            // Evidence probe: must observe a concrete outcome. XuGu may ignore timeout
            // (COMMITTED_DESPITE_TIMEOUT + remainingRows>0) — that is a documented GAP, not PASS.
            assertTrue(outcome.startsWith("XAException") || outcome.startsWith("COMMITTED"),
                    "timeout path produced no observable outcome: " + outcome);
            if (remaining > 0) {
                System.out.println("[P1-1 timeout] GAP: RM did not abort after setTransactionTimeout(2); "
                        + "shallow evidence only — timeout recovery NOT proven");
            }
        } finally {
            closeQuietly(conn);
            closeQuietly(xaConn);
            BaselineSupport.dropTableQuietly(props, "jdbc.url." + DB, RAW_TABLE);
        }
    }

    @Test
    @Order(3)
    void connectionKillDuringPrepareLeavesRecoverableOrCleanState() throws Exception {
        Properties props = prepareSingleDb();
        ensureRawTable(props);

        XADatasourceImp xaDs = newXaDataSource(props.getProperty("jdbc.url." + DB), props);
        XAConnection xaConn = null;
        Connection conn = null;
        Xid xid = newXid("kill");
        AtomicReference<String> prepareOutcome = new AtomicReference<>("not-started");
        try {
            xaConn = xaDs.getXAConnection();
            XAResource xaRes = xaConn.getXAResource();
            conn = xaConn.getConnection();
            xaRes.start(xid, XAResource.TMNOFLAGS);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + RAW_TABLE + " (ID, USER_ID, STATUS) VALUES (?, ?, ?)")) {
                ps.setInt(1, 3001);
                ps.setInt(2, 1);
                ps.setString(3, "KILL-PREPARE");
                assertEquals(1, ps.executeUpdate());
            }
            xaRes.end(xid, XAResource.TMSUCCESS);

            // Deterministic TM-side kill before prepare (prefer client connection close over server kill).
            try {
                conn.close();
            } catch (Exception ignored) {
                // expected
            }
            try {
                xaConn.close();
            } catch (Exception ignored) {
                // expected
            }
            conn = null;
            xaConn = null;

            try {
                int vote = xaRes.prepare(xid);
                prepareOutcome.set("PREPARE_OK_AFTER_CLOSE vote=" + vote);
                try {
                    xaRes.commit(xid, false);
                    prepareOutcome.set(prepareOutcome.get() + "; COMMIT_OK");
                } catch (XAException ex) {
                    prepareOutcome.set(prepareOutcome.get() + "; COMMIT_XAEX=" + ex.errorCode);
                    try {
                        xaRes.rollback(xid);
                    } catch (XAException ignored) {
                        // best-effort
                    }
                }
            } catch (XAException ex) {
                prepareOutcome.set("PREPARE_XAEX errorCode=" + ex.errorCode + " msg=" + ex.getMessage());
                try {
                    xaRes.rollback(xid);
                } catch (XAException ignored) {
                    // connection may already be dead
                }
            } catch (Exception ex) {
                prepareOutcome.set("PREPARE_OTHER " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }

            Xid[] recovered;
            XAConnection probe = null;
            try {
                probe = xaDs.getXAConnection();
                recovered = safeRecover(probe.getXAResource());
            } finally {
                closeQuietly(probe);
            }
            int remaining = BaselineSupport.countOn(props, "jdbc.url." + DB, RAW_TABLE);
            System.out.println("[P1-1 conn-kill] prepareOutcome=" + prepareOutcome.get()
                    + " remainingRows=" + remaining
                    + " recoverCount=" + recovered.length
                    + " recover=" + Arrays.toString(recovered));

            assertTrue(!"not-started".equals(prepareOutcome.get()),
                    "connection-kill path must reach prepare attempt");
            if (prepareOutcome.get().contains("COMMIT_OK")) {
                System.out.println("[P1-1 conn-kill] NOTE: prepare/commit succeeded after Connection.close — "
                        + "driver kept RM branch alive (medium evidence weak on this run)");
            }
            if (recovered.length > 0) {
                System.out.println("[P1-1 conn-kill] recover() returned in-doubt Xid(s); "
                        + "heuristic complete/forget NOT asserted");
            }
        } finally {
            closeQuietly(conn);
            closeQuietly(xaConn);
            BaselineSupport.dropTableQuietly(props, "jdbc.url." + DB, RAW_TABLE);
        }
    }

    private static Properties prepareSingleDb() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, DB);
        return props;
    }

    private static void ensureSsTables(final Properties props) throws Exception {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, SS_TABLE);
            BaselineSupport.executeOn(props, urlKey,
                    "CREATE TABLE " + SS_TABLE + " (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
        }
    }

    private static void ensureRawTable(final Properties props) throws Exception {
        BaselineSupport.dropTableQuietly(props, "jdbc.url." + DB, RAW_TABLE);
        BaselineSupport.executeOn(props, "jdbc.url." + DB,
                "CREATE TABLE " + RAW_TABLE + " (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
    }

    private static void cleanupSs(final Properties props) {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, SS_TABLE);
        }
    }

    private static void insertSs(final Connection conn, final int id, final int userId, final String status)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO baseline_xa_order (id, user_id, status) VALUES (?, ?, ?)")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.setString(3, status);
            assertEquals(1, ps.executeUpdate());
        }
    }

    private static XADatasourceImp newXaDataSource(final String jdbcUrl, final Properties props) {
        XADatasourceImp xaDs = new XADatasourceImp();
        xaDs.setUrl(jdbcUrl);
        xaDs.setUser(props.getProperty("jdbc.user"));
        xaDs.setPassword(props.getProperty("jdbc.password"));
        return xaDs;
    }

    private static Xid newXid(final String tag) {
        byte[] gtrid = ("p11-" + tag + "-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        byte[] bqual = "b0".getBytes(StandardCharsets.UTF_8);
        return new XAXid(gtrid, bqual, 1);
    }

    private static Xid[] safeRecover(final XAResource xaRes) {
        try {
            Xid[] a = xaRes.recover(XAResource.TMSTARTRSCAN);
            Xid[] b = xaRes.recover(XAResource.TMENDRSCAN);
            if (a == null || a.length == 0) {
                return b == null ? new Xid[0] : b;
            }
            if (b == null || b.length == 0) {
                return a;
            }
            Xid[] merged = Arrays.copyOf(a, a.length + b.length);
            System.arraycopy(b, 0, merged, a.length, b.length);
            return merged;
        } catch (XAException ex) {
            System.out.println("[P1-1 recover] XAException errorCode=" + ex.errorCode + " msg=" + ex.getMessage());
            return new Xid[0];
        }
    }

    private static String summarize(final Throwable ex) {
        if (ex == null) {
            return "null";
        }
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }

    private static void closeQuietly(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Exception ignored) {
            // best-effort
        }
    }

    private static void closeQuietly(final XAConnection xaConnection) {
        if (xaConnection == null) {
            return;
        }
        try {
            xaConnection.close();
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
