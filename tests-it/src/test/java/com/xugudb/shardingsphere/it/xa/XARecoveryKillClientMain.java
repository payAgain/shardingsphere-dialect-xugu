package com.xugudb.shardingsphere.it.xa;

import com.xugu.xa.XADatasourceImp;
import com.xugu.xa.XAXid;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

/**
 * Client-side JVM kill target for P1-1 (optional script).
 *
 * <p>Flow: create table → XA start → INSERT → print {@code READY_FOR_KILL} → sleep →
 * attempt end/prepare/commit. A companion script may {@code Stop-Process} this JVM after
 * the marker; post-check counts residual rows and optionally calls {@code recover()}.
 *
 * <p>Usage (from repo root, after {@code mvn -pl tests-it -am test-compile -Pxa-recovery}):
 * see {@code scripts/xa-recovery-kill-client.ps1}.
 */
public final class XARecoveryKillClientMain {

    private static final String TABLE = "XA_RECOVERY_KILL";

    private static final String DB = "shard_ds0";

    private XARecoveryKillClientMain() {
    }

    public static void main(final String[] args) throws Exception {
        if (args.length > 0 && "probe".equalsIgnoreCase(args[0])) {
            probe();
            return;
        }
        long holdMs = args.length > 0 ? Long.parseLong(args[0]) : 20_000L;
        Properties props = loadProps();
        String baseUrl = props.getProperty("jdbc.url");
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
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
        Xid xid = new XAXid(
                ("kill-client-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8),
                "b0".getBytes(StandardCharsets.UTF_8),
                1);
        try {
            xaRes.start(xid, XAResource.TMNOFLAGS);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (ID, USER_ID, STATUS) VALUES (?, ?, ?)")) {
                ps.setInt(1, 9001);
                ps.setInt(2, 1);
                ps.setString(3, "KILL-CLIENT");
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("insert failed");
                }
            }
            System.out.println("READY_FOR_KILL xid=" + xid + " holdMs=" + holdMs);
            System.out.flush();
            Thread.sleep(holdMs);
            xaRes.end(xid, XAResource.TMSUCCESS);
            int vote = xaRes.prepare(xid);
            xaRes.commit(xid, false);
            System.out.println("COMMITTED vote=" + vote);
        } finally {
            try {
                conn.close();
            } catch (Exception ignored) {
                // best-effort
            }
            try {
                xaConn.close();
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    /** Post-kill probe: print COUNT(*) and recover() size. */
    private static void probe() throws Exception {
        Properties props = loadProps();
        String baseUrl = props.getProperty("jdbc.url");
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
        String dbUrl = rewriteDatabase(baseUrl, DB);
        int count;
        try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TABLE)) {
            rs.next();
            count = rs.getInt(1);
        } catch (Exception ex) {
            System.out.println("PROBE_COUNT=UNAVAILABLE msg=" + ex.getMessage());
            count = -1;
        }
        XADatasourceImp xaDs = new XADatasourceImp();
        xaDs.setUrl(dbUrl);
        xaDs.setUser(user);
        xaDs.setPassword(password);
        int recoverCount = -1;
        XAConnection xaConn = null;
        try {
            xaConn = xaDs.getXAConnection();
            XAResource xaRes = xaConn.getXAResource();
            Xid[] start = xaRes.recover(XAResource.TMSTARTRSCAN);
            Xid[] end = xaRes.recover(XAResource.TMENDRSCAN);
            recoverCount = (start == null ? 0 : start.length) + (end == null ? 0 : end.length);
        } catch (Exception ex) {
            System.out.println("PROBE_RECOVER=UNAVAILABLE msg=" + ex.getMessage());
        } finally {
            if (xaConn != null) {
                try {
                    xaConn.close();
                } catch (Exception ignored) {
                    // best-effort
                }
            }
        }
        System.out.println("PROBE_COUNT=" + count + " PROBE_RECOVER=" + recoverCount);
    }

    private static Properties loadProps() throws Exception {
        Properties props = new Properties();
        try (InputStream in = XARecoveryKillClientMain.class.getClassLoader()
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
}
