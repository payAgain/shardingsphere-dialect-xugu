package com.xugudb.shardingsphere.it;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dual-datasource database sharding CRUD + LIMIT via ShardingSphere JDBC (compatiblemode=NONE).
 */
class ShardingCrudIT {

    private static final String DB0 = "shard_ds0";
    private static final String DB1 = "shard_ds1";
    private static final String SCHEMA0 = "SS_SHARD_0";
    private static final String SCHEMA1 = "SS_SHARD_1";

    private Properties loadProps() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("it-xugu.properties")) {
            Assumptions.assumeTrue(in != null, "it-xugu.properties missing on classpath");
            props.load(in);
        }
        return props;
    }

    private void assumeHostReachable(final Properties props) throws Exception {
        Class.forName(props.getProperty("jdbc.driver"));
        try (Connection ignored = DriverManager.getConnection(
                props.getProperty("jdbc.url"),
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"))) {
            // reachable
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "XuGu IT host unreachable: " + ex.getMessage());
        }
    }

    /**
     * Probe multi-target strategy: prefer CREATE DATABASE, else CREATE SCHEMA + current_schema.
     *
     * @return probe log lines for acceptance evidence
     */
    private List<String> prepareShardTargets(final Properties props) throws Exception {
        List<String> log = new ArrayList<>();
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
        String baseUrl = props.getProperty("jdbc.url");

        try (Connection admin = DriverManager.getConnection(baseUrl, user, password);
             Statement st = admin.createStatement()) {
            boolean db0Ok = tryExecute(st, "CREATE DATABASE " + DB0, log, "CREATE_DATABASE_" + DB0);
            boolean db1Ok = tryExecute(st, "CREATE DATABASE " + DB1, log, "CREATE_DATABASE_" + DB1);
            // already-exists still counts as usable if connect works
            String url0 = rewriteDatabase(baseUrl, DB0);
            String url1 = rewriteDatabase(baseUrl, DB1);
            boolean connect0 = canConnect(url0, user, password, log, "CONNECT_" + DB0);
            boolean connect1 = canConnect(url1, user, password, log, "CONNECT_" + DB1);
            if ((db0Ok || connect0) && (db1Ok || connect1) && connect0 && connect1) {
                props.setProperty("jdbc.url.ds0", url0);
                props.setProperty("jdbc.url.ds1", url1);
                props.setProperty("shard.mode", "DATABASE");
                log.add("SHARD_MODE=DATABASE urls=" + url0 + " | " + url1);
                return log;
            }
            log.add("SHARD_MODE_DATABASE_UNAVAILABLE falling back to SCHEMA");
            tryExecute(st, "CREATE SCHEMA " + SCHEMA0, log, "CREATE_SCHEMA_" + SCHEMA0);
            tryExecute(st, "CREATE SCHEMA " + SCHEMA1, log, "CREATE_SCHEMA_" + SCHEMA1);
            String schemaUrl0 = withCurrentSchema(baseUrl, SCHEMA0);
            String schemaUrl1 = withCurrentSchema(baseUrl, SCHEMA1);
            Assumptions.assumeTrue(canConnect(schemaUrl0, user, password, log, "CONNECT_SCHEMA_" + SCHEMA0),
                    "cannot connect with current_schema=" + SCHEMA0);
            Assumptions.assumeTrue(canConnect(schemaUrl1, user, password, log, "CONNECT_SCHEMA_" + SCHEMA1),
                    "cannot connect with current_schema=" + SCHEMA1);
            props.setProperty("jdbc.url.ds0", schemaUrl0);
            props.setProperty("jdbc.url.ds1", schemaUrl1);
            props.setProperty("shard.mode", "SCHEMA");
            log.add("SHARD_MODE=SCHEMA urls=" + schemaUrl0 + " | " + schemaUrl1);
            return log;
        }
    }

    private static boolean tryExecute(final Statement st, final String sql, final List<String> log, final String tag) {
        try {
            st.execute(sql);
            log.add(tag + "=OK");
            return true;
        } catch (Exception ex) {
            log.add(tag + "=FAIL: " + ex.getMessage());
            return false;
        }
    }

    private static boolean canConnect(final String url, final String user, final String password,
                                     final List<String> log, final String tag) {
        try (Connection ignored = DriverManager.getConnection(url, user, password)) {
            log.add(tag + "=OK");
            return true;
        } catch (Exception ex) {
            log.add(tag + "=FAIL: " + ex.getMessage());
            return false;
        }
    }

    /** Replace database name in jdbc:xugu://host:port/DB?... */
    private static String rewriteDatabase(final String jdbcUrl, final String database) {
        int schemeEnd = jdbcUrl.indexOf("://");
        Assumptions.assumeTrue(schemeEnd > 0, "unexpected jdbc url: " + jdbcUrl);
        int pathStart = jdbcUrl.indexOf('/', schemeEnd + 3);
        Assumptions.assumeTrue(pathStart > 0, "jdbc url missing database path: " + jdbcUrl);
        int queryStart = jdbcUrl.indexOf('?', pathStart);
        if (queryStart < 0) {
            return jdbcUrl.substring(0, pathStart + 1) + database;
        }
        return jdbcUrl.substring(0, pathStart + 1) + database + jdbcUrl.substring(queryStart);
    }

    private static String withCurrentSchema(final String jdbcUrl, final String schema) {
        String marker = "current_schema=";
        int idx = jdbcUrl.toLowerCase(Locale.ROOT).indexOf(marker);
        if (idx >= 0) {
            int valueStart = idx + marker.length();
            int amp = jdbcUrl.indexOf('&', valueStart);
            if (amp < 0) {
                return jdbcUrl.substring(0, valueStart) + schema;
            }
            return jdbcUrl.substring(0, valueStart) + schema + jdbcUrl.substring(amp);
        }
        String sep = jdbcUrl.contains("?") ? "&" : "?";
        return jdbcUrl + sep + "current_schema=" + schema;
    }

    private byte[] loadYaml(final Properties props) throws Exception {
        String yaml;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("sharding-two-ds.yaml")) {
            Assumptions.assumeTrue(in != null, "sharding-two-ds.yaml missing on classpath");
            byte[] raw = new byte[in.available()];
            int read = in.read(raw);
            Assumptions.assumeTrue(read > 0, "empty sharding-two-ds.yaml");
            yaml = new String(raw, StandardCharsets.UTF_8);
        }
        yaml = yaml.replace("${jdbc.url.ds0}", props.getProperty("jdbc.url.ds0"))
                .replace("${jdbc.url.ds1}", props.getProperty("jdbc.url.ds1"))
                .replace("${jdbc.user}", props.getProperty("jdbc.user"))
                .replace("${jdbc.password}", props.getProperty("jdbc.password"));
        return yaml.getBytes(StandardCharsets.UTF_8);
    }

    private void ensurePhysicalTables(final Properties props) throws Exception {
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            try (Connection conn = DriverManager.getConnection(props.getProperty(urlKey), user, password);
                 Statement st = conn.createStatement()) {
                try {
                    st.execute("DROP TABLE T_ORDER");
                } catch (Exception ignored) {
                    // may not exist
                }
                // XuGu stores unquoted identifiers as UPPER_CASE (matches dialect IdentifierPatternType)
                st.execute("CREATE TABLE T_ORDER (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
            }
        }
    }

    private void dropPhysicalTables(final Properties props) {
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            try (Connection conn = DriverManager.getConnection(props.getProperty(urlKey), user, password);
                 Statement st = conn.createStatement()) {
                st.execute("DROP TABLE T_ORDER");
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        }
    }

    @Test
    void shardingCrudAndLimitThroughShardingSphereJdbc() throws Exception {
        Properties props = loadProps();
        assumeHostReachable(props);
        List<String> probeLog = prepareShardTargets(props);
        for (String line : probeLog) {
            System.out.println(line);
        }

        ensurePhysicalTables(props);
        DataSource dataSource = YamlShardingSphereDataSourceFactory.createDataSource(loadYaml(props));
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            try {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO t_order (id, user_id, status) VALUES (?, ?, ?)")) {
                    insert.setInt(1, 1);
                    insert.setInt(2, 1);
                    insert.setString(3, "odd");
                    assertEquals(1, insert.executeUpdate());

                    insert.setInt(1, 2);
                    insert.setInt(2, 2);
                    insert.setString(3, "even");
                    assertEquals(1, insert.executeUpdate());
                }

                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id, user_id, status FROM t_order WHERE id = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1));
                        assertEquals(1, rs.getInt(2));
                        assertEquals("odd", rs.getString(3));
                        assertFalse(rs.next());
                    }
                    select.setInt(1, 2);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(2, rs.getInt(1));
                        assertEquals(2, rs.getInt(2));
                        assertEquals("even", rs.getString(3));
                        assertFalse(rs.next());
                    }
                }

                // verify rows landed on expected physical shards (user_id % 2)
                assertEquals(1, countOn(props, "jdbc.url.ds1", 1));
                assertEquals(1, countOn(props, "jdbc.url.ds0", 2));

                int limitRows = 0;
                try (ResultSet rs = st.executeQuery("SELECT * FROM t_order LIMIT 5")) {
                    while (rs.next()) {
                        limitRows++;
                    }
                }
                assertEquals(2, limitRows);
            } finally {
                dropPhysicalTables(props);
            }
        } finally {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        }
    }

    private static int countOn(final Properties props, final String urlKey, final int userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                props.getProperty(urlKey), props.getProperty("jdbc.user"), props.getProperty("jdbc.password"));
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM T_ORDER WHERE USER_ID = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }
}
