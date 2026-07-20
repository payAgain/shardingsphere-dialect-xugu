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
 */
public final class BaselineSupport {

    public static final String DB0 = "shard_ds0";

    public static final String DB1 = "shard_ds1";

    public static final String DB_WRITE = "baseline_write";

    public static final String DB_READ0 = "baseline_read0";

    public static final String DB_READ1 = "baseline_read1";

    private BaselineSupport() {
    }

    public static Properties loadProps() throws Exception {
        Properties props = new Properties();
        try (InputStream in = BaselineSupport.class.getClassLoader().getResourceAsStream("it-xugu.properties")) {
            Assumptions.assumeTrue(in != null, "it-xugu.properties missing on classpath");
            props.load(in);
        }
        return props;
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

    /** Unused helper retained for schema-mode experiments. */
    @SuppressWarnings("unused")
    static String withCurrentSchema(final String jdbcUrl, final String schema) {
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
