package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B1 — order + order_item database and table sharding.
 */
class OrderDbTableShardingIT {

    private static final String[] ORDER_TABLES = {
            "BASELINE_ORDER_0", "BASELINE_ORDER_1"
    };

    private static final String[] ITEM_TABLES = {
            "BASELINE_ORDER_ITEM_0", "BASELINE_ORDER_ITEM_1"
    };

    @Test
    void placeOrderQueryAndJoin() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-order-sharding.yaml", props);
        try (Connection conn = dataSource.getConnection()) {
            try {
                // user_id=1 → ds_1; order_id=10 → table _0
                try (PreparedStatement insertOrder = conn.prepareStatement(
                        "INSERT INTO baseline_order (order_id, user_id, status) VALUES (?, ?, ?)")) {
                    insertOrder.setInt(1, 10);
                    insertOrder.setInt(2, 1);
                    insertOrder.setString(3, "NEW");
                    assertEquals(1, insertOrder.executeUpdate());
                }
                try (PreparedStatement insertItem = conn.prepareStatement(
                        "INSERT INTO baseline_order_item (item_id, order_id, user_id, product) VALUES (?, ?, ?, ?)")) {
                    insertItem.setInt(1, 100);
                    insertItem.setInt(2, 10);
                    insertItem.setInt(3, 1);
                    insertItem.setString(4, "SKU-A");
                    assertEquals(1, insertItem.executeUpdate());
                }

                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT order_id, user_id, status FROM baseline_order WHERE order_id = ?")) {
                    select.setInt(1, 10);
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(10, rs.getInt(1));
                        assertEquals(1, rs.getInt(2));
                        assertEquals("NEW", rs.getString(3));
                        assertFalse(rs.next());
                    }
                }

                // two-query association (join may be federation-sensitive)
                try (PreparedStatement selectItem = conn.prepareStatement(
                        "SELECT item_id, product FROM baseline_order_item WHERE order_id = ? AND user_id = ?")) {
                    selectItem.setInt(1, 10);
                    selectItem.setInt(2, 1);
                    try (ResultSet rs = selectItem.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(100, rs.getInt(1));
                        assertEquals("SKU-A", rs.getString(2));
                        assertFalse(rs.next());
                    }
                }

                assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds1", "BASELINE_ORDER_0"));
                assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds1", "BASELINE_ORDER_ITEM_0"));
            } finally {
                cleanup(props);
            }
        } finally {
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static void ensurePhysicalTables(final Properties props) throws Exception {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            for (String table : ORDER_TABLES) {
                BaselineSupport.dropTableQuietly(props, urlKey, table);
                BaselineSupport.executeOn(props, urlKey,
                        "CREATE TABLE " + table + " (ORDER_ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
            }
            for (String table : ITEM_TABLES) {
                BaselineSupport.dropTableQuietly(props, urlKey, table);
                BaselineSupport.executeOn(props, urlKey,
                        "CREATE TABLE " + table
                                + " (ITEM_ID INT PRIMARY KEY, ORDER_ID INT NOT NULL, USER_ID INT NOT NULL, PRODUCT VARCHAR(64))");
            }
        }
    }

    private static void cleanup(final Properties props) {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            for (String table : ORDER_TABLES) {
                BaselineSupport.dropTableQuietly(props, urlKey, table);
            }
            for (String table : ITEM_TABLES) {
                BaselineSupport.dropTableQuietly(props, urlKey, table);
            }
        }
    }
}
