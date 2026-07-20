package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B5 — pagination list with LIMIT across shards.
 */
class PaginationListIT {

    private static final String TABLE = "BASELINE_TX_ORDER";

    private static final int CONCURRENCY_THREADS = 8;

    @Test
    void limitReturnsAtMostFiveRows() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            try {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                    for (int i = 1; i <= 12; i++) {
                        insert.setInt(1, i);
                        insert.setInt(2, i);
                        insert.setString(3, "P" + i);
                        assertTrue(insert.executeUpdate() == 1);
                    }
                }

                int rows = 0;
                try (ResultSet rs = st.executeQuery(
                        "SELECT id, user_id, status FROM baseline_tx_order ORDER BY id LIMIT 5")) {
                    while (rs.next()) {
                        rows++;
                    }
                }
                assertTrue(rows <= 5, "expected <=5 rows, got " + rows);
                assertTrue(rows > 0, "expected at least one row");
            } finally {
                cleanup(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void limitOnEmptyTableReturnsZero() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            try {
                int rows = 0;
                try (ResultSet rs = st.executeQuery(
                        "SELECT id, user_id, status FROM baseline_tx_order ORDER BY id LIMIT 5")) {
                    while (rs.next()) {
                        rows++;
                    }
                }
                assertEquals(0, rows, "empty table LIMIT should return zero rows");
            } finally {
                cleanup(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void concurrentPaginationSmoke() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        try (Connection seed = dataSource.getConnection();
             PreparedStatement insert = seed.prepareStatement(
                     "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
            for (int i = 1; i <= 16; i++) {
                insert.setInt(1, i);
                insert.setInt(2, i);
                insert.setString(3, "CP" + i);
                assertEquals(1, insert.executeUpdate());
            }
        }

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY_THREADS);
        AtomicInteger errors = new AtomicInteger();
        try {
            for (int t = 0; t < CONCURRENCY_THREADS; t++) {
                new Thread(() -> {
                    try {
                        start.await(30, TimeUnit.SECONDS);
                        try (Connection conn = dataSource.getConnection();
                             Statement st = conn.createStatement();
                             ResultSet rs = st.executeQuery(
                                     "SELECT id FROM baseline_tx_order ORDER BY id LIMIT 3")) {
                            int rows = 0;
                            while (rs.next()) {
                                rows++;
                            }
                            assertTrue(rows <= 3 && rows > 0, "expected 1..3 rows, got " + rows);
                        }
                    } catch (Exception ex) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }, "b5-concurrent-" + t).start();
            }
            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "concurrency smoke timed out");
            assertEquals(0, errors.get(), "unexpected SQLException / assertion failures under concurrency");
            int total = BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE)
                    + BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE);
            assertEquals(16, total);
        } finally {
            cleanup(props);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static void ensurePhysicalTables(final Properties props) throws Exception {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, TABLE);
            BaselineSupport.executeOn(props, urlKey,
                    "CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
        }
    }

    private static void cleanup(final Properties props) {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, TABLE);
        }
    }
}
