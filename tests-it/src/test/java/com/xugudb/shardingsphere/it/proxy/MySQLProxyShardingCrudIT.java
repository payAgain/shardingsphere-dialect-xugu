package com.xugudb.shardingsphere.it.proxy;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G-007 P-003 — MySQL JDBC client → embedded Proxy (MySQL wire) → XuGu {@code compatiblemode=NONE}
 * sharded CRUD smoke on logic table {@code t_order}.
 *
 * <p>Approach: in-process {@code BootstrapInitializer} + {@code ShardingSphereProxy} (SS 5.5.3).
 * Lab XuGu unreachable → {@code assumeReachable} skips (evidence {@code BLOCKED_ENV}).</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySQLProxyShardingCrudIT {

    private static Properties props;

    private static int proxyPort = -1;

    private static boolean proxyStarted;

    @AfterAll
    static void stopProxy() {
        try {
            if (props != null && proxyStarted) {
                ProxySupport.cleanupPhysicalTables(props);
            }
        } finally {
            ProxySupport.stopQuietly();
            proxyStarted = false;
            proxyPort = -1;
        }
    }

    @Test
    @Order(1)
    void insertSelectUpdateDeleteOnLogicTable() throws Exception {
        ensureProxy();
        // user_id=1 → ds_1; user_id=2 → ds_0
        try (Connection conn = ProxySupport.openMySQLClient(proxyPort)) {
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO t_order (order_id, user_id, status) VALUES (?, ?, ?)")) {
                insert.setInt(1, 1001);
                insert.setInt(2, 1);
                insert.setString(3, "NEW");
                assertEquals(1, insert.executeUpdate());
            }

            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT order_id, user_id, status FROM t_order WHERE order_id = ?")) {
                select.setInt(1, 1001);
                try (ResultSet rs = select.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1001, rs.getInt(1));
                    assertEquals(1, rs.getInt(2));
                    assertEquals("NEW", rs.getString(3));
                    assertFalse(rs.next());
                }
            }

            assertEquals(1, BaselineSupport.countOn(props, "jdbc.url.ds1", ProxySupport.PHYSICAL_TABLE));
            assertEquals(0, BaselineSupport.countOn(props, "jdbc.url.ds0", ProxySupport.PHYSICAL_TABLE));

            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE t_order SET status = ? WHERE order_id = ? AND user_id = ?")) {
                update.setString(1, "PAID");
                update.setInt(2, 1001);
                update.setInt(3, 1);
                assertEquals(1, update.executeUpdate());
            }

            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT status FROM t_order WHERE order_id = ?")) {
                select.setInt(1, 1001);
                try (ResultSet rs = select.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("PAID", rs.getString(1));
                    assertFalse(rs.next());
                }
            }

            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM t_order WHERE order_id = ? AND user_id = ?")) {
                delete.setInt(1, 1001);
                delete.setInt(2, 1);
                assertEquals(1, delete.executeUpdate());
            }

            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT order_id FROM t_order WHERE order_id = ?")) {
                select.setInt(1, 1001);
                try (ResultSet rs = select.executeQuery()) {
                    assertFalse(rs.next());
                }
            }

            assertEquals(0, BaselineSupport.countOn(props, "jdbc.url.ds0", ProxySupport.PHYSICAL_TABLE));
            assertEquals(0, BaselineSupport.countOn(props, "jdbc.url.ds1", ProxySupport.PHYSICAL_TABLE));
        }
    }

    @Test
    @Order(2)
    void storageUrlsRemainCompatibleModeNone() throws Exception {
        ensureProxy();
        String ds0 = props.getProperty("jdbc.url.ds0", "");
        String ds1 = props.getProperty("jdbc.url.ds1", "");
        assertCompatibleModeNone(ds0);
        assertCompatibleModeNone(ds1);
        assertCompatibleModeNone(props.getProperty("jdbc.url"));
    }

    private static synchronized void ensureProxy() throws Exception {
        if (proxyStarted) {
            return;
        }
        props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        assertCompatibleModeNone(props.getProperty("jdbc.url"));
        proxyPort = ProxySupport.startForShardingCrud(props);
        assertTrue(proxyPort > 0, "proxy port must be bound");
        proxyStarted = true;
    }

    private static void assertCompatibleModeNone(final String jdbcUrl) {
        assertTrue(jdbcUrl != null && jdbcUrl.toLowerCase(Locale.ROOT).contains("compatiblemode=none"),
                "storage URL must use compatiblemode=NONE: " + jdbcUrl);
    }
}
