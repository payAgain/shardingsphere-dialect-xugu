package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B6 — encrypt rule on phone column (AES).
 */
class EncryptColumnIT {

    private static final String TABLE = "BASELINE_USER";

    @Test
    void insertPlaintextSelectDecrypted() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0);
        ensurePhysicalTable(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-encrypt.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                String plaintext = "13800138000";
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_user (id, phone) VALUES (?, ?)")) {
                    insert.setInt(1, 1);
                    insert.setString(2, plaintext);
                    assertEquals(1, insert.executeUpdate());
                }
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id, phone FROM baseline_user WHERE id = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1));
                        assertEquals(plaintext, rs.getString(2));
                    }
                }

                try (Connection physical = java.sql.DriverManager.getConnection(
                        props.getProperty("jdbc.url.ds0"),
                        props.getProperty("jdbc.user"),
                        props.getProperty("jdbc.password"));
                     Statement st = physical.createStatement();
                     ResultSet rs = st.executeQuery("SELECT PHONE_CIPHER FROM " + TABLE + " WHERE ID = 1")) {
                    assertTrue(rs.next());
                    String cipher = rs.getString(1);
                    assertNotEquals(plaintext, cipher);
                }
            } finally {
                BaselineSupport.dropTableQuietly(props, "jdbc.url.ds0", TABLE);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static void ensurePhysicalTable(final Properties props) throws Exception {
        BaselineSupport.dropTableQuietly(props, "jdbc.url.ds0", TABLE);
        BaselineSupport.executeOn(props, "jdbc.url.ds0",
                "CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, PHONE_CIPHER VARCHAR(256))");
    }
}
