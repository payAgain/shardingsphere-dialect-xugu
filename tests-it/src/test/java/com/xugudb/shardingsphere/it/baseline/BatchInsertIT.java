package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B4 — batch insert across shard keys.
 */
class BatchInsertIT {

    private static final String TABLE = "BASELINE_TX_ORDER";

    private static final int CONCURRENCY_THREADS = 8;

    @Test
    void batchInsertAcrossShards() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                    for (int i = 1; i <= 20; i++) {
                        insert.setInt(1, i);
                        insert.setInt(2, i);
                        insert.setString(3, "B" + i);
                        insert.addBatch();
                    }
                    int[] results = insert.executeBatch();
                    assertEquals(20, results.length);
                }

                int even = BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE);
                int odd = BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE);
                assertEquals(10, even);
                assertEquals(10, odd);
                assertEquals(20, even + odd);
            } finally {
                cleanup(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void emptyBatchIsNoOpAndDuplicateKeyInBatchFails() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                    int[] empty = insert.executeBatch();
                    assertEquals(0, empty.length);
                }

                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                    insert.setInt(1, 1);
                    insert.setInt(2, 1);
                    insert.setString(3, "ONCE");
                    insert.addBatch();
                    insert.setInt(1, 1);
                    insert.setInt(2, 1);
                    insert.setString(3, "DUP");
                    insert.addBatch();
                    assertThrows(SQLException.class, insert::executeBatch);
                }

                int total = BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE)
                        + BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE);
                assertTrue(total <= 1, "duplicate batch should not leave more than one surviving row, got " + total);
            } finally {
                cleanup(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    void concurrentBatchInsertSmoke() throws Exception {
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
                        try (Connection conn = dataSource.getConnection();
                             PreparedStatement insert = conn.prepareStatement(
                                     "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                            int base = 1000 + threadId * 10;
                            for (int i = 0; i < 2; i++) {
                                int id = base + i;
                                insert.setInt(1, id);
                                insert.setInt(2, id);
                                insert.setString(3, "CB" + threadId);
                                insert.addBatch();
                            }
                            int[] results = insert.executeBatch();
                            assertEquals(2, results.length);
                        }
                    } catch (Exception ex) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }, "b4-concurrent-" + t).start();
            }
            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "concurrency smoke timed out");
            assertEquals(0, errors.get(), "unexpected SQLException / assertion failures under concurrency");
            int total = BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE)
                    + BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE);
            assertEquals(CONCURRENCY_THREADS * 2, total);
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
