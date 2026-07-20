package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
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
 * B2 — readwrite-splitting (write + 2 reads; sql-show enabled).
 *
 * <p><b>Topology (same-host simulation only):</b> {@code write_ds} and
 * {@code read_ds_*} point at different DATABASE names on the same XuGu lab
 * instance. This asserts ShardingSphere <em>routing</em> isolation, not
 * physical replica lag, streaming replication, or true read-only replica
 * semantics. Do not cite these tests as evidence of replica/physical isolation.</p>
 */
class ReadwriteSplittingIT {

    private static final String TABLE = "BASELINE_RW_ORDER";

    private static final int CONCURRENCY_THREADS = 8;

    private static final String READ_MARKER = "READ_ONLY_MARKER";

    private static final String WRITE_STATUS = "WRITTEN";

    @Test
    void writeThenReadSmoke() throws Exception {
        Properties props = prepareProps();
        ensureTablesOnAllDs(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-readwrite.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            // transactionalReadQueryStrategy=PRIMARY: in-TX SELECT hits write_ds so
            // read-after-write works without a physical replica.
            conn.setAutoCommit(false);
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
                conn.commit();
                assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.write", TABLE));
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                dropTablesOnAllDs(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void selectMissingIdReturnsEmptyAndDuplicateKeyFails() throws Exception {
        Properties props = prepareProps();
        ensureTablesOnAllDs(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-readwrite.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
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
                conn.rollback();
            } finally {
                dropTablesOnAllDs(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void concurrentWriteReadSmoke() throws Exception {
        Properties props = prepareProps();
        ensureTablesOnAllDs(props);

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
                            conn.setAutoCommit(false);
                            try {
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
                                conn.commit();
                            } catch (Exception ex) {
                                conn.rollback();
                                throw ex;
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
            dropTablesOnAllDs(props);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    /**
     * Same-host different-DATABASE routing isolation.
     *
     * <p>Asserts: INSERT lands only on write DATABASE; auto-commit SELECT is
     * served from read DATABASE(s); in-TX SELECT (PRIMARY) still sees write.
     * JDBC URLs must reference distinct database path segments.</p>
     *
     * <p><b>Limits:</b> not a physical replica; no lag/HA/read-only replica claim.</p>
     */
    @Test
    void sameHostReadDsRoutingIsolation() throws Exception {
        Properties props = prepareProps();
        assertSameHostDistinctDatabases(props);
        ensureTablesOnAllDs(props);

        // Markers exist only on read DBs — proves SELECT auto-commit routes to read_ds.
        seedStatus(props, "jdbc.url.read0", 99, READ_MARKER);
        seedStatus(props, "jdbc.url.read1", 99, READ_MARKER);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-readwrite.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(true);

                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_rw_order (id, status) VALUES (?, ?)")) {
                    insert.setInt(1, 1);
                    insert.setString(2, WRITE_STATUS);
                    assertEquals(1, insert.executeUpdate());
                }

                assertEquals(1, countById(props, "jdbc.url.write", 1), "INSERT must hit write_ds DATABASE");
                assertEquals(0, countById(props, "jdbc.url.read0", 1), "write row must not appear on read0");
                assertEquals(0, countById(props, "jdbc.url.read1", 1), "write row must not appear on read1");

                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id, status FROM baseline_rw_order WHERE id = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertFalse(rs.next(),
                                "auto-commit SELECT id=1 must miss write row (routed to read_ds, not replica sync)");
                    }
                }

                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id, status FROM baseline_rw_order WHERE id = ?")) {
                    select.setInt(1, 99);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next(), "auto-commit SELECT must hit read DATABASE marker");
                        assertEquals(99, rs.getInt(1));
                        assertEquals(READ_MARKER, rs.getString(2));
                    }
                }

                conn.setAutoCommit(false);
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT id, status FROM baseline_rw_order WHERE id = ?")) {
                    select.setInt(1, 1);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next(), "in-TX SELECT (PRIMARY) must see write_ds row");
                        assertEquals(WRITE_STATUS, rs.getString(2));
                    }
                }
                conn.rollback();
            } finally {
                dropTablesOnAllDs(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static Properties prepareProps() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props,
                BaselineSupport.DB_WRITE, BaselineSupport.DB_READ0, BaselineSupport.DB_READ1);
        // Keep distinct jdbc.url.read* (same host, different DATABASE). Do NOT alias to write.
        return props;
    }

    private static void assertSameHostDistinctDatabases(final Properties props) {
        String writeUrl = props.getProperty("jdbc.url.write");
        String read0Url = props.getProperty("jdbc.url.read0");
        String read1Url = props.getProperty("jdbc.url.read1");
        assertTrue(writeUrl != null && read0Url != null && read1Url != null, "write/read JDBC URLs required");
        assertTrue(writeUrl.toLowerCase(Locale.ROOT).contains("/" + BaselineSupport.DB_WRITE.toLowerCase(Locale.ROOT)),
                "write URL must target DATABASE " + BaselineSupport.DB_WRITE + ": " + writeUrl);
        assertTrue(read0Url.toLowerCase(Locale.ROOT).contains("/" + BaselineSupport.DB_READ0.toLowerCase(Locale.ROOT)),
                "read0 URL must target DATABASE " + BaselineSupport.DB_READ0 + ": " + read0Url);
        assertTrue(read1Url.toLowerCase(Locale.ROOT).contains("/" + BaselineSupport.DB_READ1.toLowerCase(Locale.ROOT)),
                "read1 URL must target DATABASE " + BaselineSupport.DB_READ1 + ": " + read1Url);
        assertNotEquals(writeUrl, read0Url, "read0 must not share write JDBC URL (same-host different DATABASE)");
        assertNotEquals(writeUrl, read1Url, "read1 must not share write JDBC URL (same-host different DATABASE)");
        assertHostPortEqual(writeUrl, read0Url);
        assertHostPortEqual(writeUrl, read1Url);
    }

    private static void assertHostPortEqual(final String left, final String right) {
        assertEquals(hostPort(left), hostPort(right),
                "read/write must stay on the same lab host (same-host simulation, not multi-machine replica)");
    }

    private static String hostPort(final String jdbcUrl) {
        int schemeEnd = jdbcUrl.indexOf("://");
        int pathStart = jdbcUrl.indexOf('/', schemeEnd + 3);
        return jdbcUrl.substring(schemeEnd + 3, pathStart).toLowerCase(Locale.ROOT);
    }

    private static void ensureTablesOnAllDs(final Properties props) throws Exception {
        for (String urlKey : new String[]{"jdbc.url.write", "jdbc.url.read0", "jdbc.url.read1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, TABLE);
            BaselineSupport.executeOn(props, urlKey,
                    "CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, STATUS VARCHAR(32))");
        }
    }

    private static void dropTablesOnAllDs(final Properties props) {
        BaselineSupport.dropTableQuietly(props, "jdbc.url.write", TABLE);
        BaselineSupport.dropTableQuietly(props, "jdbc.url.read0", TABLE);
        BaselineSupport.dropTableQuietly(props, "jdbc.url.read1", TABLE);
    }

    private static void seedStatus(final Properties props, final String urlKey,
                                   final int id, final String status) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                props.getProperty(urlKey), props.getProperty("jdbc.user"), props.getProperty("jdbc.password"));
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO " + TABLE + " (ID, STATUS) VALUES (?, ?)")) {
            ps.setInt(1, id);
            ps.setString(2, status);
            assertEquals(1, ps.executeUpdate());
        }
    }

    private static int countById(final Properties props, final String urlKey, final int id) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                props.getProperty(urlKey), props.getProperty("jdbc.user"), props.getProperty("jdbc.password"));
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM " + TABLE + " WHERE ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getInt(1);
            }
        }
    }
}
