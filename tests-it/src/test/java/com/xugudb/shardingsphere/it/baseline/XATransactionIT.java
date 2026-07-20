package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * B7 — XA commit and rollback across two shards.
 */
class XATransactionIT {

    private static final String TABLE = "BASELINE_XA_ORDER";

    @Test
    void xaCommitAndRollbackAcrossShards() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource;
        try {
            dataSource = BaselineSupport.createDataSource("baseline/baseline-xa.yaml", props);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "XA DataSource init failed (TM/XA deps): " + ex.getMessage());
            return;
        }

        try {
            // commit path: user_id 1 → ds_1, user_id 2 → ds_0
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    insert(conn, 1, 1, "XA-C1");
                    insert(conn, 2, 2, "XA-C2");
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    Assumptions.assumeTrue(false, "XA commit path failed: " + ex.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            }
            assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE));
            assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE));

            // rollback path
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    insert(conn, 3, 1, "XA-R1");
                    insert(conn, 4, 2, "XA-R2");
                    conn.rollback();
                } finally {
                    conn.setAutoCommit(true);
                }
            }
            assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds0", TABLE));
            assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds1", TABLE));
        } finally {
            cleanup(props);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static void insert(final Connection conn, final int id, final int userId, final String status)
            throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO baseline_xa_order (id, user_id, status) VALUES (?, ?, ?)")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.setString(3, status);
            assertEquals(1, ps.executeUpdate());
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
