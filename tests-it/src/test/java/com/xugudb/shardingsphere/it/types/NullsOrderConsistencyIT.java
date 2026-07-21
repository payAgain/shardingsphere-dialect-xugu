package com.xugudb.shardingsphere.it.types;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.apache.shardingsphere.database.connector.core.metadata.database.enums.NullsOrderType;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.DialectDatabaseMetaData;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * G-T5 — NULLS sort behavior vs {@link DialectDatabaseMetaData#getDefaultNullsOrderType()} = HIGH.
 *
 * <p>HIGH means nulls sort after non-nulls on ASC (and before on DESC) when no explicit NULLS clause.</p>
 */
class NullsOrderConsistencyIT {

    private static final String DB = "nulls_rt_ds0";

    private static final String TABLE = "NULLS_RT";

    private static Properties props;

    private static DataSource ssSingle;

    @BeforeAll
    static void setUp() throws Exception {
        props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, DB);
        props.setProperty("jdbc.url.nulls", props.getProperty("jdbc.url." + DB));
        dropQuietly();
        try (Connection conn = nativeConn(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, VAL INT)");
            st.executeUpdate("INSERT INTO " + TABLE + " (ID, VAL) VALUES (1, 10)");
            st.executeUpdate("INSERT INTO " + TABLE + " (ID, VAL) VALUES (2, NULL)");
            st.executeUpdate("INSERT INTO " + TABLE + " (ID, VAL) VALUES (3, 20)");
        }
        ssSingle = createSsDataSource(props);
    }

    @AfterAll
    static void tearDown() {
        BaselineSupport.closeQuietly(ssSingle);
        dropQuietly();
    }

    @Test
    void metadataDeclaresNullsHigh() {
        DialectDatabaseMetaData meta = DatabaseTypedSPILoader.getService(
                DialectDatabaseMetaData.class, TypedSPILoader.getService(DatabaseType.class, "XuGu"));
        assertEquals(NullsOrderType.HIGH, meta.getDefaultNullsOrderType());
    }

    @Test
    void nativeAscDefaultPutsNullLastConsistentWithHigh() throws Exception {
        try (Connection conn = nativeConn()) {
            List<Integer> vals = readVals(conn, "SELECT VAL FROM " + TABLE + " ORDER BY VAL ASC, ID ASC");
            assertEquals(Integer.valueOf(10), vals.get(0));
            assertEquals(Integer.valueOf(20), vals.get(1));
            assertNull(vals.get(2), "HIGH → ASC default should place NULL last");
        }
    }

    @Test
    void nativeDescDefaultPutsNullFirstConsistentWithHigh() throws Exception {
        try (Connection conn = nativeConn()) {
            List<Integer> vals = readVals(conn, "SELECT VAL FROM " + TABLE + " ORDER BY VAL DESC, ID ASC");
            assertNull(vals.get(0), "HIGH → DESC default should place NULL first");
            assertEquals(Integer.valueOf(20), vals.get(1));
            assertEquals(Integer.valueOf(10), vals.get(2));
        }
    }

    @Test
    void explicitNullsFirstLastHonored() throws Exception {
        try (Connection conn = nativeConn()) {
            List<Integer> first = readVals(conn,
                    "SELECT VAL FROM " + TABLE + " ORDER BY VAL ASC NULLS FIRST, ID ASC");
            assertNull(first.get(0));
            List<Integer> last = readVals(conn,
                    "SELECT VAL FROM " + TABLE + " ORDER BY VAL ASC NULLS LAST, ID ASC");
            assertNull(last.get(2));
        }
    }

    @Test
    void ssAscDefaultMatchesNativeHighSemantics() throws Exception {
        try (Connection conn = ssSingle.getConnection()) {
            List<Integer> vals = readVals(conn, "SELECT VAL FROM " + TABLE + " ORDER BY VAL ASC, ID ASC");
            assertEquals(Integer.valueOf(10), vals.get(0));
            assertEquals(Integer.valueOf(20), vals.get(1));
            assertNull(vals.get(2));
        }
    }

    private static List<Integer> readVals(final Connection conn, final String sql) throws Exception {
        List<Integer> result = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int v = rs.getInt(1);
                result.add(rs.wasNull() ? null : v);
            }
        }
        return result;
    }

    private static Connection nativeConn() throws Exception {
        return DriverManager.getConnection(
                props.getProperty("jdbc.url.nulls"),
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"));
    }

    private static DataSource createSsDataSource(final Properties props) throws Exception {
        String yaml;
        try (InputStream in = NullsOrderConsistencyIT.class.getClassLoader()
                .getResourceAsStream("sharding-single-ds.yaml")) {
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        yaml = yaml.replace("${jdbc.url}", props.getProperty("jdbc.url.nulls"))
                .replace("${jdbc.user}", props.getProperty("jdbc.user"))
                .replace("${jdbc.password}", props.getProperty("jdbc.password"));
        return YamlShardingSphereDataSourceFactory.createDataSource(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static void dropQuietly() {
        if (props == null || !props.containsKey("jdbc.url.nulls")) {
            return;
        }
        try {
            BaselineSupport.executeOn(props, "jdbc.url.nulls", "DROP TABLE " + TABLE);
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
