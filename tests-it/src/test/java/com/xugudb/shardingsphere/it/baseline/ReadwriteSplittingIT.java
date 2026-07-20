package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B2 — readwrite-splitting smoke (write + 2 reads; sql-show enabled).
 *
 * <p>IT maps three logical DS names; read URLs may share the write physical DB
 * so insert+select works without replica lag.</p>
 */
class ReadwriteSplittingIT {

    private static final String TABLE = "BASELINE_RW_ORDER";

    private static final int CONCURRENCY_THREADS = 8;

    @Test
    void writeThenReadSmoke() throws Exception {
        Properties props = prepareProps();
        ensureTable(props, "jdbc.url.write");

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-readwrite.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_rw_order (id, status) VALUES (?, ?)")) {
                    insert.setInt(1, 1);
                    insert.setString(2, "OK");
                    assertEquals(1, insert.executeUpdate());
                }
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id, status FROM baseline_rw_order WHERE id = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1));
                        assertEquals("OK", rs.getString(2));
                    }
                }
                assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.write", TABLE));
            } finally {
                BaselineSupport.dropTableQuietly(props, "jdbc.url.write", TABLE);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void selectMissingIdReturnsEmptyAndDuplicateKeyFails() throws Exception {
        Properties props = prepareProps();
        ensureTable(props, "jdbc.url.write");

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-readwrite.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id FROM baseline_rw_order WHERE id = ?")) {
                    select.setInt(1, 404);
                    try (ResultSet rs = select.executeQuery()) {
                        assertFalse(rs.next(), "missing id should yield empty result");
                    }
                }

                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_rw_order (id, status) VALUES (?, ?)")) {
                    insert.setInt(1, 7);
                    insert.setString(2, "ONCE");
                    assertEquals(1, insert.executeUpdate());
                    insert.setInt(1, 7);
                    insert.setString(2, "AGAIN");
                    assertThrows(SQLException.class, insert::executeUpdate);
                }
            } finally {
                BaselineSupport.dropTableQuietly(props, "jdbc.url.write", TABLE);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void concurrentWriteReadSmoke() throws Exception {
        Properties props = prepareProps();
        ensureTable(props, "jdbc.url.write");

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-readwrite.yaml", props);
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
                        try (Connection conn = dataSource.getConnection()) {
                            try (PreparedStatement insert = conn.prepareStatement(
                                    "INSERT INTO baseline_rw_order (id, status) VALUES (?, ?)")) {
                                insert.setInt(1, id);
                                insert.setString(2, "RW" + threadId);
                                assertEquals(1, insert.executeUpdate());
                            }
                            try (PreparedStatement select = conn.prepareStatement(
                                    "SELECT id, status FROM baseline_rw_order WHERE id = ?")) {
                                select.setInt(1, id);
                                try (ResultSet rs = select.executeQuery()) {
                                    assertTrue(rs.next());
                                    assertEquals(id, rs.getInt(1));
                                }
                            }
                        }
                    } catch (Exception ex) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }, "b2-concurrent-" + t).start();
            }
            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "concurrency smoke timed out");
            assertEquals(0, errors.get(), "unexpected SQLException / assertion failures under concurrency");
            assertEquals(CONCURRENCY_THREADS, BaselineSupport.countOn(props, "jdbc.url.write", TABLE));
        } finally {
            BaselineSupport.dropTableQuietly(props, "jdbc.url.write", TABLE);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static Properties prepareProps() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props,
                BaselineSupport.DB_WRITE, BaselineSupport.DB_READ0, BaselineSupport.DB_READ1);
        String writeUrl = props.getProperty("jdbc.url.write");
        props.setProperty("jdbc.url.read0", writeUrl);
        props.setProperty("jdbc.url.read1", writeUrl);
        return props;
    }

    private static void ensureTable(final Properties props, final String urlKey) throws Exception {
        BaselineSupport.dropTableQuietly(props, urlKey, TABLE);
        BaselineSupport.executeOn(props, urlKey,
                "CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, STATUS VARCHAR(32))");
    }
}
