package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B3 — local transaction with savepoint rollback.
 */
class LocalTransactionSavepointIT {

    private static final String TABLE = "BASELINE_TX_ORDER";

    private static final int CONCURRENCY_THREADS = 8;

    @Test
    void rollbackToSavepointKeepsEarlierRows() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                    insert.setInt(1, 1);
                    insert.setInt(2, 1);
                    insert.setString(3, "KEEP");
                    assertEquals(1, insert.executeUpdate());

                    Savepoint sp = conn.setSavepoint("sp1");

                    insert.setInt(1, 2);
                    insert.setInt(2, 1);
                    insert.setString(3, "DROP");
                    assertEquals(1, insert.executeUpdate());

                    conn.rollback(sp);
                    conn.commit();
                } finally {
                    conn.setAutoCommit(true);
                }

                // user_id=1 → ds_1
                assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE));
                assertEquals(0, BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE));
            } finally {
                cleanup(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void fullRollbackLeavesNoRows() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                    insert.setInt(1, 11);
                    insert.setInt(2, 1);
                    insert.setString(3, "ABORT");
                    assertEquals(1, insert.executeUpdate());
                    insert.setInt(1, 12);
                    insert.setInt(2, 2);
                    insert.setString(3, "ABORT");
                    assertEquals(1, insert.executeUpdate());
                    conn.rollback();
                } finally {
                    conn.setAutoCommit(true);
                }

                assertEquals(0, BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE));
                assertEquals(0, BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE));
            } finally {
                cleanup(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void concurrentLocalTxSmoke() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY_THREADS);
        AtomicInteger errors = new AtomicInteger();
        try {
            for (int t = 0; t < CONCURRENCY_THREADS; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        start.await(30, TimeUnit.SECONDS);
                        int id = 200 + threadId;
                        int userId = threadId + 1;
                        try (Connection conn = dataSource.getConnection()) {
                            conn.setAutoCommit(false);
                            try (PreparedStatement insert = conn.prepareStatement(
                                    "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                                insert.setInt(1, id);
                                insert.setInt(2, userId);
                                insert.setString(3, "TX" + threadId);
                                assertEquals(1, insert.executeUpdate());
                                conn.commit();
                            } catch (Exception ex) {
                                conn.rollback();
                                throw ex;
                            } finally {
                                conn.setAutoCommit(true);
                            }
                        }
                    } catch (Exception ex) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }, "b3-concurrent-" + t).start();
            }
            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "concurrency smoke timed out");
            assertEquals(0, errors.get(), "unexpected SQLException / assertion failures under concurrency");
            int total = BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE)
                    + BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE);
            assertEquals(CONCURRENCY_THREADS, total);
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
