package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B6 — encrypt rule on phone column (AES).
 */
class EncryptColumnIT {

    private static final String TABLE = "BASELINE_USER";

    private static final int CONCURRENCY_THREADS = 8;

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

    @Test
    void selectMissingUserReturnsEmptyAndDuplicateKeyFails() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0);
        ensurePhysicalTable(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-encrypt.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id, phone FROM baseline_user WHERE id = ?")) {
                    select.setInt(1, 404);
                    try (ResultSet rs = select.executeQuery()) {
                        assertFalse(rs.next(), "missing user should yield empty result");
                    }
                }

                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_user (id, phone) VALUES (?, ?)")) {
                    insert.setInt(1, 9);
                    insert.setString(2, "13900000000");
                    assertEquals(1, insert.executeUpdate());
                    insert.setInt(1, 9);
                    insert.setString(2, "13900000001");
                    assertThrows(SQLException.class, insert::executeUpdate);
                }
            } finally {
                BaselineSupport.dropTableQuietly(props, "jdbc.url.ds0", TABLE);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void concurrentEncryptInsertSelectSmoke() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0);
        ensurePhysicalTable(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-encrypt.yaml", props);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY_THREADS);
        AtomicInteger errors = new AtomicInteger();
        try {
            for (int t = 0; t < CONCURRENCY_THREADS; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        start.await(30, TimeUnit.SECONDS);
                        int id = 100 + threadId;
                        String phone = "13800138" + String.format("%03d", threadId);
                        try (Connection conn = dataSource.getConnection()) {
                            try (PreparedStatement insert = conn.prepareStatement(
                                    "INSERT INTO baseline_user (id, phone) VALUES (?, ?)")) {
                                insert.setInt(1, id);
                                insert.setString(2, phone);
                                assertEquals(1, insert.executeUpdate());
                            }
                            try (PreparedStatement select = conn.prepareStatement(
                                    "SELECT id, phone FROM baseline_user WHERE id = ?")) {
                                select.setInt(1, id);
                                try (ResultSet rs = select.executeQuery()) {
                                    assertTrue(rs.next());
                                    assertEquals(id, rs.getInt(1));
                                    assertEquals(phone, rs.getString(2));
                                }
                            }
                        }
                    } catch (Exception ex) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }, "b6-concurrent-" + t).start();
            }
            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "concurrency smoke timed out");
            assertEquals(0, errors.get(), "unexpected SQLException / assertion failures under concurrency");
            assertEquals(CONCURRENCY_THREADS, BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE));
        } finally {
            BaselineSupport.dropTableQuietly(props, "jdbc.url.ds0", TABLE);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static void ensurePhysicalTable(final Properties props) throws Exception {
        BaselineSupport.dropTableQuietly(props, "jdbc.url.ds0", TABLE);
        BaselineSupport.executeOn(props, "jdbc.url.ds0",
                "CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, PHONE_CIPHER VARCHAR(256))");
    }
}
