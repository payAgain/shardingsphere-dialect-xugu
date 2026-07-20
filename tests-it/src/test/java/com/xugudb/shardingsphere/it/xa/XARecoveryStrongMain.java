package com.xugudb.shardingsphere.it.xa;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.xugu.xa.XADatasourceImp;

import javax.sql.XAConnection;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * G-006 Q-01 Strong XA recovery attempt.
 *
 * <p>Modes:
 * <ul>
 *   <li>{@code prepare-hold} — Atomikos enlist + commit path that blocks <em>after</em>
 *       RM {@code prepare} succeeds, printing {@code READY_FOR_KILL} so a script can kill
 *       this JVM while TM logs are on disk.</li>
 *   <li>{@code recover-resolve} — new JVM: restart Atomikos against the same log dir,
 *       then RM {@code recover()} + heuristic commit/rollback of any in-doubt Xids;
 *       print {@code STRONG_PASS} or {@code STRONG_BLOCKED} with reason.</li>
 * </ul>
 */
public final class XARecoveryStrongMain {

    private static final String TABLE = "XA_RECOVERY_STRONG";

    private static final String DB = "shard_ds0";

    private XARecoveryStrongMain() {
    }

    public static void main(final String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "prepare-hold";
        Path logDir = Paths.get(args.length > 1 ? args[1] : "tests-it/logs/xa-strong-tm").toAbsolutePath();
        if ("recover-resolve".equalsIgnoreCase(mode)) {
            recoverResolve(logDir);
            return;
        }
        prepareHold(logDir);
    }

    private static void prepareHold(final Path logDir) throws Exception {
        Files.createDirectories(logDir);
        configureAtomikos(logDir);
        Properties props = loadProps();
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
        String baseUrl = props.getProperty("jdbc.url");
        ensureDatabase(baseUrl, user, password, DB);
        String dbUrl = rewriteDatabase(baseUrl, DB);

        try (Connection admin = DriverManager.getConnection(dbUrl, user, password);
             Statement st = admin.createStatement()) {
            tryExecute(st, "DROP TABLE " + TABLE);
            st.execute("CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
        }

        XADatasourceImp xaDs = new XADatasourceImp();
        xaDs.setUrl(dbUrl);
        xaDs.setUser(user);
        xaDs.setPassword(password);

        XAConnection xaConn = xaDs.getXAConnection();
        XAResource xaRes = xaConn.getXAResource();
        Connection conn = xaConn.getConnection();
        KillAfterPrepareXAResource wrapper = new KillAfterPrepareXAResource(xaRes);

        UserTransactionManager utm = new UserTransactionManager();
        utm.setForceShutdown(true);
        utm.setTransactionTimeout(120);
        utm.init();
        System.out.println("ATOMIKOS_LOG_DIR=" + logDir);
        System.out.println("STRONG_PHASE=BEGIN");
        try {
            utm.begin();
            Transaction tx = utm.getTransaction();
            try {
                tx.enlistResource(wrapper);
            } catch (Exception enlistEx) {
                System.out.println("STRONG_PHASE=ATOMIKOS_ENLIST_FAIL " + enlistEx.getClass().getSimpleName()
                        + ": " + enlistEx.getMessage());
                try {
                    utm.rollback();
                } catch (Exception ignored) {
                    // best-effort
                }
                try {
                    utm.close();
                } catch (Exception ignored) {
                    // best-effort
                }
                closeQuietly(conn);
                closeQuietly(xaConn);
                prepareHoldRawXa(logDir, dbUrl, user, password);
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (ID, USER_ID, STATUS) VALUES (?, ?, ?)")) {
                ps.setInt(1, 9101);
                ps.setInt(2, 1);
                ps.setString(3, "STRONG-PREPARE");
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("insert failed");
                }
            }
            System.out.println("STRONG_PHASE=COMMIT_ENTER");
            System.out.flush();
            // Blocks inside wrapper.prepare after RM prepare succeeds → script kills JVM.
            utm.commit();
            System.out.println("STRONG_PHASE=COMMIT_RETURNED (unexpected — kill window missed)");
        } catch (Exception ex) {
            System.out.println("STRONG_PHASE=COMMIT_ERROR " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            try {
                if (utm.getStatus() != Status.STATUS_NO_TRANSACTION) {
                    utm.rollback();
                }
            } catch (Exception ignored) {
                // best-effort
            }
            throw ex;
        } finally {
            closeQuietly(conn);
            closeQuietly(xaConn);
            try {
                utm.close();
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    private static void recoverResolve(final Path logDir) throws Exception {
        Files.createDirectories(logDir);
        configureAtomikos(logDir);
        Properties props = loadProps();
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
        String dbUrl = rewriteDatabase(props.getProperty("jdbc.url"), DB);

        System.out.println("ATOMIKOS_LOG_DIR=" + logDir);
        System.out.println("STRONG_PHASE=TM_RESTART");
        String tmRestart = "TM_RESTART_OK";
        UserTransactionManager utm = new UserTransactionManager();
        try {
            utm.setForceShutdown(true);
            utm.init();
            // Give recovery domain a brief window to scan logs / contact RMs.
            Thread.sleep(3000L);
            utm.close();
        } catch (Exception ex) {
            tmRestart = "TM_RESTART_ERROR " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
            System.out.println(tmRestart);
        }
        System.out.println("TM_RESTART_RESULT=" + tmRestart);

        int count = countRows(dbUrl, user, password);
        XADatasourceImp xaDs = new XADatasourceImp();
        xaDs.setUrl(dbUrl);
        xaDs.setUser(user);
        xaDs.setPassword(password);

        List<String> resolveActions = new ArrayList<String>();
        int recoverBefore = -1;
        int recoverAfter = -1;
        XAConnection xaConn = null;
        try {
            xaConn = xaDs.getXAConnection();
            XAResource xaRes = xaConn.getXAResource();
            Xid[] recovered = safeRecover(xaRes);
            recoverBefore = recovered.length;
            System.out.println("RECOVER_BEFORE count=" + recoverBefore + " xids=" + Arrays.toString(recovered));
            for (Xid xid : recovered) {
                String action = heuristicResolve(xaRes, xid);
                resolveActions.add(action);
                System.out.println("HEURISTIC " + action);
            }
            Xid[] after = safeRecover(xaRes);
            recoverAfter = after.length;
            System.out.println("RECOVER_AFTER count=" + recoverAfter + " xids=" + Arrays.toString(after));
        } finally {
            closeQuietly(xaConn);
        }

        int countAfter = countRows(dbUrl, user, password);
        System.out.println("PROBE_COUNT_BEFORE_RESOLVE=" + count + " PROBE_COUNT_AFTER_RESOLVE=" + countAfter);

        // Strong PASS: in-doubt Xid(s) observed after TM death and heuristic commit/rollback completed
        // with empty recover scan afterward (rows may be 0 or 1 depending on rollback vs commit).
        final boolean strongPass = recoverBefore > 0
                && recoverAfter == 0
                && !resolveActions.isEmpty()
                && allResolved(resolveActions);
        String reason;
        if (strongPass) {
            reason = "in-doubt recovered and heuristic resolved after TM kill";
        } else if (recoverBefore <= 0) {
            reason = "NO_IN_DOUBT_AFTER_TM_KILL: recover() empty after prepare-then-kill; "
                    + "XuGu left CLEAN_ROLLBACK_OR_ABORT (rows=" + countAfter + "); "
                    + "Atomikos TM-log restart did not surface RM in-doubt for commit/rollback";
        } else {
            reason = "IN_DOUBT_PRESENT_BUT_NOT_RESOLVED recoverBefore=" + recoverBefore
                    + " recoverAfter=" + recoverAfter + " actions=" + resolveActions;
        }
        String verdict = strongPass ? "STRONG_PASS" : "STRONG_BLOCKED";
        System.out.println("STRONG_VERDICT=" + verdict + " reason=" + reason);
        System.out.println("RESOLVE_ACTIONS=" + resolveActions);
    }

    private static boolean allResolved(final List<String> actions) {
        for (String a : actions) {
            if (a.startsWith("RESOLVE_FAIL") || a.startsWith("UNRESOLVED")) {
                return false;
            }
        }
        return true;
    }

    private static String heuristicResolve(final XAResource xaRes, final Xid xid) {
        try {
            xaRes.rollback(xid);
            return "ROLLBACK_OK xid=" + xid;
        } catch (XAException rbEx) {
            try {
                xaRes.commit(xid, false);
                return "COMMIT_OK_AFTER_ROLLBACK_FAIL rb=" + rbEx.errorCode + " xid=" + xid;
            } catch (XAException cEx) {
                try {
                    xaRes.forget(xid);
                    return "FORGET_OK after rb=" + rbEx.errorCode + " commit=" + cEx.errorCode + " xid=" + xid;
                } catch (XAException fEx) {
                    return "RESOLVE_FAIL rb=" + rbEx.errorCode + " commit=" + cEx.errorCode
                            + " forget=" + fEx.errorCode + " xid=" + xid;
                }
            }
        }
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
            System.out.println("RECOVER_XAEX errorCode=" + ex.errorCode + " msg=" + ex.getMessage());
            return new Xid[0];
        }
    }

    private static int countRows(final String dbUrl, final String user, final String password) {
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TABLE)) {
            rs.next();
            return rs.getInt(1);
        } catch (Exception ex) {
            System.out.println("PROBE_COUNT=UNAVAILABLE msg=" + ex.getMessage());
            return -1;
        }
    }

    private static void configureAtomikos(final Path logDir) throws Exception {
        Files.createDirectories(logDir);
        String dir = logDir.toString().replace('\\', '/');
        Path jtaProps = logDir.resolve("jta.properties");
        StringBuilder cfg = new StringBuilder();
        cfg.append("com.atomikos.icatch.log_base_dir=").append(dir).append('\n');
        cfg.append("com.atomikos.icatch.output_dir=").append(dir).append('\n');
        cfg.append("com.atomikos.icatch.enable_logging=true\n");
        cfg.append("com.atomikos.icatch.force_shutdown_on_vm_exit=false\n");
        cfg.append("com.atomikos.icatch.automatic_resource_registration=true\n");
        cfg.append("com.atomikos.icatch.registered=true\n");
        Files.write(jtaProps, cfg.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.setProperty("com.atomikos.icatch.file", jtaProps.toString().replace('\\', '/'));
        System.setProperty("com.atomikos.icatch.log_base_dir", dir);
        System.setProperty("com.atomikos.icatch.output_dir", dir);
        System.setProperty("com.atomikos.icatch.enable_logging", "true");
        System.setProperty("com.atomikos.icatch.force_shutdown_on_vm_exit", "false");
        System.setProperty("com.atomikos.icatch.registered", "true");
        // Override ShardingSphere transactions.properties default (false) so temporary
        // XAResource enlist is allowed for this Strong probe.
        System.setProperty("com.atomikos.icatch.automatic_resource_registration", "true");
    }

    /**
     * Raw XA prepare-hold (no Atomikos enlist). Used when Atomikos rejects unregistered
     * XAResource; still satisfies prepare → kill → recover-resolve Strong attempt shape.
     */
    private static void prepareHoldRawXa(final Path logDir, final String dbUrl, final String user,
                                         final String password) throws Exception {
        Files.createDirectories(logDir);
        System.out.println("ATOMIKOS_LOG_DIR=" + logDir + " (raw-XA fallback; TM log may be empty)");
        System.out.println("STRONG_PHASE=BEGIN_RAW_XA");
        XADatasourceImp xaDs = new XADatasourceImp();
        xaDs.setUrl(dbUrl);
        xaDs.setUser(user);
        xaDs.setPassword(password);
        XAConnection xaConn = xaDs.getXAConnection();
        XAResource xaRes = xaConn.getXAResource();
        Connection conn = xaConn.getConnection();
        Xid xid = new com.xugu.xa.XAXid(
                ("strong-raw-" + java.util.UUID.randomUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "b0".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                1);
        try {
            xaRes.start(xid, XAResource.TMNOFLAGS);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (ID, USER_ID, STATUS) VALUES (?, ?, ?)")) {
                ps.setInt(1, 9101);
                ps.setInt(2, 1);
                ps.setString(3, "STRONG-PREPARE");
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("insert failed");
                }
            }
            xaRes.end(xid, XAResource.TMSUCCESS);
            int vote = xaRes.prepare(xid);
            System.out.println("PREPARED vote=" + vote + " xid=" + xid);
            System.out.println("READY_FOR_KILL phase=AFTER_PREPARE_RAW_XA xid=" + xid);
            System.out.flush();
            Thread.sleep(300_000L);
            xaRes.commit(xid, false);
            System.out.println("STRONG_PHASE=COMMIT_RETURNED (unexpected — kill window missed)");
        } finally {
            closeQuietly(conn);
            closeQuietly(xaConn);
        }
    }

    private static Properties loadProps() throws Exception {
        Properties props = new Properties();
        try (InputStream in = XARecoveryStrongMain.class.getClassLoader()
                .getResourceAsStream("it-xugu.properties")) {
            if (in == null) {
                throw new IllegalStateException("it-xugu.properties missing");
            }
            props.load(in);
        }
        Class.forName(props.getProperty("jdbc.driver"));
        return props;
    }

    private static void ensureDatabase(final String baseUrl, final String user, final String password,
                                       final String database) throws Exception {
        try (Connection admin = DriverManager.getConnection(baseUrl, user, password);
             Statement st = admin.createStatement()) {
            tryExecute(st, "CREATE DATABASE " + database);
        }
    }

    private static String rewriteDatabase(final String jdbcUrl, final String database) {
        int schemeEnd = jdbcUrl.indexOf("://");
        int pathStart = jdbcUrl.indexOf('/', schemeEnd + 3);
        int queryStart = jdbcUrl.indexOf('?', pathStart);
        if (queryStart < 0) {
            return jdbcUrl.substring(0, pathStart + 1) + database;
        }
        return jdbcUrl.substring(0, pathStart + 1) + database + jdbcUrl.substring(queryStart);
    }

    private static void tryExecute(final Statement st, final String sql) {
        try {
            st.execute(sql);
        } catch (Exception ignored) {
            // best-effort
        }
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

    /**
     * Delegating XAResource that blocks after a successful prepare so the orchestrating
     * script can kill the TM/client JVM with Atomikos logs still on disk.
     */
    static final class KillAfterPrepareXAResource implements XAResource {

        private final XAResource delegate;

        KillAfterPrepareXAResource(final XAResource delegate) {
            this.delegate = delegate;
        }

        @Override
        public int prepare(final Xid xid) throws XAException {
            int vote = delegate.prepare(xid);
            System.out.println("PREPARED vote=" + vote + " xid=" + xid);
            System.out.println("READY_FOR_KILL phase=AFTER_PREPARE_ATOMIKOS xid=" + xid);
            System.out.flush();
            try {
                // Script Stop-Process; do not return into Atomikos commit phase.
                Thread.sleep(300_000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return vote;
        }

        @Override
        public void commit(final Xid xid, final boolean onePhase) throws XAException {
            delegate.commit(xid, onePhase);
        }

        @Override
        public void end(final Xid xid, final int flags) throws XAException {
            delegate.end(xid, flags);
        }

        @Override
        public void forget(final Xid xid) throws XAException {
            delegate.forget(xid);
        }

        @Override
        public int getTransactionTimeout() throws XAException {
            return delegate.getTransactionTimeout();
        }

        @Override
        public boolean isSameRM(final XAResource xaResource) throws XAException {
            if (xaResource instanceof KillAfterPrepareXAResource) {
                return delegate.isSameRM(((KillAfterPrepareXAResource) xaResource).delegate);
            }
            return delegate.isSameRM(xaResource);
        }

        @Override
        public Xid[] recover(final int flag) throws XAException {
            return delegate.recover(flag);
        }

        @Override
        public void rollback(final Xid xid) throws XAException {
            delegate.rollback(xid);
        }

        @Override
        public boolean setTransactionTimeout(final int seconds) throws XAException {
            return delegate.setTransactionTimeout(seconds);
        }

        @Override
        public void start(final Xid xid, final int flags) throws XAException {
            delegate.start(xid, flags);
        }
    }
}
