package com.xugudb.shardingsphere.it.baseline;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.junit.jupiter.api.Assumptions;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Shared fixture for baseline IT scenarios (compatiblemode=NONE).
 *
 * <p>Default lab props: {@code it-xugu.properties}. Alternate same-host namespace (G-004 P1-3 env2):
 * system property {@code it.xugu.properties=it-xugu-env2.properties} (Maven {@code -Penv2} /
 * {@code -Pit-xugu-env2}). Optional overrides: {@code db.ds0}/{@code db.ds1}/{@code db.write}/
 * {@code db.read0}/{@code db.read1}.</p>
 */
public final class BaselineSupport {

    public static final String DEFAULT_DB0 = "shard_ds0";

    public static final String DEFAULT_DB1 = "shard_ds1";

    public static final String DEFAULT_DB_WRITE = "baseline_write";

    public static final String DEFAULT_DB_READ0 = "baseline_read0";

    public static final String DEFAULT_DB_READ1 = "baseline_read1";

    /** Active DATABASE name for ds0 (mutable when env2 props override {@code db.ds0}). */
    public static String DB0 = DEFAULT_DB0;

    public static String DB1 = DEFAULT_DB1;

    public static String DB_WRITE = DEFAULT_DB_WRITE;

    public static String DB_READ0 = DEFAULT_DB_READ0;

    public static String DB_READ1 = DEFAULT_DB_READ1;

    /** Lab read-only user for same-host topology deepen (T3=A); SELECT-only, not a replica. */
    public static final String DEFAULT_READ_USER = "ss_ro_reader";

    public static final String DEFAULT_READ_PASSWORD = "ss_ro_reader";

    /** {@code OK} when restricted read user is ready; {@code BLOCKED_ENV} when CREATE/GRANT failed. */
    public static final String PROP_READONLY_STATUS = "topology.readonly.status";

    private BaselineSupport() {
    }

    public static Properties loadProps() throws Exception {
        Properties props = new Properties();
        String resource = System.getProperty("it.xugu.properties", "it-xugu.properties");
        try (InputStream in = BaselineSupport.class.getClassLoader().getResourceAsStream(resource)) {
            Assumptions.assumeTrue(in != null, resource + " missing on classpath");
            props.load(in);
        }
        applyDatabaseNames(props);
        return props;
    }

    /**
     * Apply optional {@code db.*} overrides so env2 can use alternate DATABASE names
     * on the same XuGu host without colliding with the primary lab namespace.
     */
    static void applyDatabaseNames(final Properties props) {
        DB0 = props.getProperty("db.ds0", DEFAULT_DB0);
        DB1 = props.getProperty("db.ds1", DEFAULT_DB1);
        DB_WRITE = props.getProperty("db.write", DEFAULT_DB_WRITE);
        DB_READ0 = props.getProperty("db.read0", DEFAULT_DB_READ0);
        DB_READ1 = props.getProperty("db.read1", DEFAULT_DB_READ1);
    }

    public static void assumeReachable(final Properties props) throws Exception {
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
     * Ensure DATABASE targets exist and set jdbc.url.* properties.
     *
     * @param props loaded IT properties
     * @param databases database names to create / connect
     * @return probe log lines
     */
    public static List<String> ensureDatabases(final Properties props, final String... databases) throws Exception {
        List<String> log = new ArrayList<>();
        String user = props.getProperty("jdbc.user");
        String password = props.getProperty("jdbc.password");
        String baseUrl = props.getProperty("jdbc.url");
        try (Connection admin = DriverManager.getConnection(baseUrl, user, password);
             Statement st = admin.createStatement()) {
            for (String database : databases) {
                tryExecute(st, "CREATE DATABASE " + database, log, "CREATE_DATABASE_" + database);
                String url = rewriteDatabase(baseUrl, database);
                Assumptions.assumeTrue(canConnect(url, user, password, log, "CONNECT_" + database),
                        "cannot connect to database " + database);
                props.setProperty("jdbc.url." + database, url);
            }
        }
        // convenience aliases used by YAML templates
        if (props.containsKey("jdbc.url." + DB0)) {
            props.setProperty("jdbc.url.ds0", props.getProperty("jdbc.url." + DB0));
        }
        if (props.containsKey("jdbc.url." + DB1)) {
            props.setProperty("jdbc.url.ds1", props.getProperty("jdbc.url." + DB1));
        }
        if (props.containsKey("jdbc.url." + DB_WRITE)) {
            props.setProperty("jdbc.url.write", props.getProperty("jdbc.url." + DB_WRITE));
        }
        if (props.containsKey("jdbc.url." + DB_READ0)) {
            props.setProperty("jdbc.url.read0", props.getProperty("jdbc.url." + DB_READ0));
        }
        if (props.containsKey("jdbc.url." + DB_READ1)) {
            props.setProperty("jdbc.url.read1", props.getProperty("jdbc.url." + DB_READ1));
        }
        return log;
    }

    /**
     * Create/grant a restricted XuGu user on each read DATABASE and bind it to read DS URLs.
     *
     * <p>XuGu users are per-DATABASE. New users start with no privileges; we grant
     * {@code SELECT ANY TABLE} only (INSERT must fail). Read JDBC URLs get
     * {@code current_schema=SYSDBA} so unqualified table names resolve to SYSDBA-owned
     * baseline tables. This deepens same-host privilege isolation — <b>not</b> a physical
     * read replica.</p>
     *
     * @return {@code true} when user+grant+URL rewrite succeeded for every read DATABASE
     */
    public static boolean ensureReadOnlyUser(final Properties props, final String... readDatabases) throws Exception {
        String readUser = props.getProperty("jdbc.user.read", DEFAULT_READ_USER);
        String readPassword = props.getProperty("jdbc.password.read", DEFAULT_READ_PASSWORD);
        String adminUser = props.getProperty("jdbc.user");
        String adminPassword = props.getProperty("jdbc.password");
        List<String> log = new ArrayList<>();
        boolean allOk = readDatabases != null && readDatabases.length > 0;
        for (String database : readDatabases) {
            String url = props.getProperty("jdbc.url." + database);
            if (url == null || url.isEmpty()) {
                log.add("RO_URL_" + database + "=MISSING");
                allOk = false;
                continue;
            }
            try (Connection admin = DriverManager.getConnection(url, adminUser, adminPassword);
                 Statement st = admin.createStatement()) {
                tryExecute(st, "CREATE USER " + readUser + " IDENTIFIED BY '" + readPassword + "'",
                        log, "CREATE_USER_" + database);
                // User may already exist from a prior run — GRANT is the decisive step.
                if (!tryExecute(st, "GRANT SELECT ANY TABLE TO " + readUser, log, "GRANT_SELECT_" + database)) {
                    allOk = false;
                    continue;
                }
                if (!canConnect(withCurrentSchema(url, "SYSDBA"), readUser, readPassword, log, "CONNECT_RO_" + database)) {
                    allOk = false;
                    continue;
                }
                String schemaUrl = withCurrentSchema(url, "SYSDBA");
                props.setProperty("jdbc.url." + database, schemaUrl);
            } catch (Exception ex) {
                log.add("RO_SETUP_" + database + "=FAIL: " + ex.getMessage());
                allOk = false;
            }
        }
        if (allOk) {
            props.setProperty("jdbc.user.read", readUser);
            props.setProperty("jdbc.password.read", readPassword);
            if (props.containsKey("jdbc.url." + DB_READ0)) {
                props.setProperty("jdbc.url.read0", props.getProperty("jdbc.url." + DB_READ0));
            }
            if (props.containsKey("jdbc.url." + DB_READ1)) {
                props.setProperty("jdbc.url.read1", props.getProperty("jdbc.url." + DB_READ1));
            }
            props.setProperty(PROP_READONLY_STATUS, "OK");
        } else {
            // Strongest safe fallback: keep admin credentials on read URLs (B2 different-DATABASE only).
            props.setProperty("jdbc.user.read", adminUser);
            props.setProperty("jdbc.password.read", adminPassword);
            props.setProperty(PROP_READONLY_STATUS, "BLOCKED_ENV");
        }
        props.setProperty("topology.readonly.log", String.join(" | ", log));
        return allOk;
    }

    public static boolean isReadOnlyUserReady(final Properties props) {
        return "OK".equalsIgnoreCase(props.getProperty(PROP_READONLY_STATUS, ""));
    }

    public static byte[] loadYaml(final String resourceName, final Properties props) throws Exception {
        return loadYaml(resourceName, props, Collections.emptyMap());
    }

    public static byte[] loadYaml(final String resourceName, final Properties props,
                                  final Map<String, String> extra) throws Exception {
        String yaml;
        try (InputStream in = BaselineSupport.class.getClassLoader().getResourceAsStream(resourceName)) {
            Assumptions.assumeTrue(in != null, resourceName + " missing on classpath");
            byte[] raw = new byte[in.available()];
            int read = in.read(raw);
            Assumptions.assumeTrue(read > 0, "empty " + resourceName);
            yaml = new String(raw, StandardCharsets.UTF_8);
        }
        Map<String, String> substitutions = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            substitutions.put("${" + key + "}", props.getProperty(key));
        }
        substitutions.putAll(extra);
        for (Map.Entry<String, String> entry : substitutions.entrySet()) {
            if (entry.getValue() != null) {
                yaml = yaml.replace(entry.getKey(), entry.getValue());
            }
        }
        return yaml.getBytes(StandardCharsets.UTF_8);
    }

    public static DataSource createDataSource(final String yamlResource, final Properties props) throws Exception {
        return YamlShardingSphereDataSourceFactory.createDataSource(loadYaml(yamlResource, props));
    }

    public static void closeQuietly(final DataSource dataSource) {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    public static void executeOn(final Properties props, final String urlKey, final String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                props.getProperty(urlKey), props.getProperty("jdbc.user"), props.getProperty("jdbc.password"));
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public static void dropTableQuietly(final Properties props, final String urlKey, final String table) {
        try {
            executeOn(props, urlKey, "DROP TABLE " + table);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    public static int countOn(final Properties props, final String urlKey, final String table) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                props.getProperty(urlKey), props.getProperty("jdbc.user"), props.getProperty("jdbc.password"));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            Assumptions.assumeTrue(rs.next());
            return rs.getInt(1);
        }
    }

    public static String rewriteDatabase(final String jdbcUrl, final String database) {
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

    /**
     * Ensure JDBC URL carries {@code current_schema=} so a restricted user can resolve
     * SYSDBA-owned unqualified table names on the same DATABASE.
     */
    public static String withCurrentSchema(final String jdbcUrl, final String schema) {
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
}
