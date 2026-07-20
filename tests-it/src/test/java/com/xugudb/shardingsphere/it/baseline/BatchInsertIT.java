package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * B4 — batch insert across shard keys.
 */
class BatchInsertIT {

    private static final String TABLE = "BASELINE_TX_ORDER";

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
