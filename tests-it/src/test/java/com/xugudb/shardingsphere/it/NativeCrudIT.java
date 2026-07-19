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
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end CRUD via ShardingSphere JDBC (single DS, compatiblemode=NONE).
 */
class NativeCrudIT {

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

    private byte[] loadYaml(final Properties props) throws Exception {
        String yaml;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("sharding-single-ds.yaml")) {
            Assumptions.assumeTrue(in != null, "sharding-single-ds.yaml missing on classpath");
            byte[] raw = new byte[in.available()];
            int read = in.read(raw);
            Assumptions.assumeTrue(read > 0, "empty sharding-single-ds.yaml");
            yaml = new String(raw, StandardCharsets.UTF_8);
        }
        yaml = yaml.replace("${jdbc.url}", props.getProperty("jdbc.url"))
                .replace("${jdbc.user}", props.getProperty("jdbc.user"))
                .replace("${jdbc.password}", props.getProperty("jdbc.password"));
        return yaml.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void crudThroughShardingSphereJdbc() throws Exception {
        Properties props = loadProps();
        assumeHostReachable(props);

        String table = "SS_XUGU_CRUD_IT_" + Long.toHexString(System.currentTimeMillis())
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        DataSource dataSource = YamlShardingSphereDataSourceFactory.createDataSource(loadYaml(props));
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            try {
                st.execute("CREATE TABLE " + table + " (ID INT PRIMARY KEY, NAME VARCHAR(64))");
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO " + table + " (ID, NAME) VALUES (?, ?)")) {
                    insert.setInt(1, 1);
                    insert.setString(2, "alice");
                    assertEquals(1, insert.executeUpdate());
                }
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT ID, NAME FROM " + table + " WHERE ID = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt("ID"));
                        assertEquals("alice", rs.getString("NAME"));
                        assertFalse(rs.next());
                    }
                }
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE " + table + " SET NAME = ? WHERE ID = ?")) {
                    update.setString(1, "bob");
                    update.setInt(2, 1);
                    assertEquals(1, update.executeUpdate());
                }
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT NAME FROM " + table + " WHERE ID = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals("bob", rs.getString("NAME"));
                    }
                }
                try (PreparedStatement delete = conn.prepareStatement(
                        "DELETE FROM " + table + " WHERE ID = ?")) {
                    delete.setInt(1, 1);
                    assertEquals(1, delete.executeUpdate());
                }
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT ID FROM " + table + " WHERE ID = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertFalse(rs.next());
                    }
                }
            } finally {
                try {
                    st.execute("DROP TABLE " + table);
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
        } finally {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        }
    }
}
