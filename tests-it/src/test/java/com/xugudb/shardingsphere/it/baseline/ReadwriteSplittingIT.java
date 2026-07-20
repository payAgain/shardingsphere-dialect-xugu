package com.xugudb.shardingsphere.it.baseline;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B2 — readwrite-splitting smoke (write + 2 reads; sql-show enabled).
 *
 * <p>IT maps three logical DS names; read URLs may share the write physical DB
 * so insert+select works without replica lag.</p>
 */
class ReadwriteSplittingIT {

    private static final String TABLE = "BASELINE_RW_ORDER";

    @Test
    void writeThenReadSmoke() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props,
                BaselineSupport.DB_WRITE, BaselineSupport.DB_READ0, BaselineSupport.DB_READ1);

        // Share physical write DB for read DS so SELECT sees committed rows without replication.
        String writeUrl = props.getProperty("jdbc.url.write");
        props.setProperty("jdbc.url.read0", writeUrl);
        props.setProperty("jdbc.url.read1", writeUrl);

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

    private static void ensureTable(final Properties props, final String urlKey) throws Exception {
        BaselineSupport.dropTableQuietly(props, urlKey, TABLE);
        BaselineSupport.executeOn(props, urlKey,
                "CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, STATUS VARCHAR(32))");
    }
}
