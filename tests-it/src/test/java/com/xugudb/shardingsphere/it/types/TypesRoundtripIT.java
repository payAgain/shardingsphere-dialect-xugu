package com.xugudb.shardingsphere.it.types;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G-T1 — JDBC/XuGu built-in type insert→select round-trip ({@code compatiblemode=NONE}).
 *
 * <p>Native path always; selected types also via ShardingSphere single-DS. Maven: {@code -Pit-xugu}.</p>
 */
class TypesRoundtripIT {

    private static final String DB = "types_rt_ds0";

    private static final String TABLE = "TYPES_RT";

    private static Properties props;

    private static DataSource ssSingle;

    @BeforeAll
    static void setUp() throws Exception {
        props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, DB);
        props.setProperty("jdbc.url.types", props.getProperty("jdbc.url." + DB));
        dropQuietly();
        try (Connection conn = nativeConn(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE " + TABLE + " ("
                    + "ID INT PRIMARY KEY,"
                    + "C_BOOL BOOLEAN,"
                    + "C_TINYINT TINYINT,"
                    + "C_SMALLINT SMALLINT,"
                    + "C_INT INT,"
                    + "C_BIGINT BIGINT,"
                    + "C_FLOAT FLOAT,"
                    + "C_DOUBLE DOUBLE,"
                    + "C_NUM NUMBER(18,4),"
                    + "C_CHAR CHAR(8),"
                    + "C_VARCHAR VARCHAR(64),"
                    + "C_TEXT TEXT,"
                    + "C_BINARY BINARY(4),"
                    + "C_DATE DATE,"
                    + "C_TS DATETIME,"
                    + "C_GUID CHAR(36)"
                    + ")");
        }
        ssSingle = createSsDataSource(props);
    }

    @AfterAll
    static void tearDown() {
        BaselineSupport.closeQuietly(ssSingle);
        dropQuietly();
    }

    static Stream<Arguments> scalarCases() {
        return Stream.of(
                Arguments.of("bool_true", "C_BOOL", (Binder) (ps, i) -> ps.setBoolean(i, true),
                        (Reader) (rs, col) -> assertTrue(rs.getBoolean(col))),
                Arguments.of("bool_false", "C_BOOL", (Binder) (ps, i) -> ps.setBoolean(i, false),
                        (Reader) (rs, col) -> assertFalse(rs.getBoolean(col))),
                Arguments.of("tinyint", "C_TINYINT", (Binder) (ps, i) -> ps.setByte(i, (byte) 42),
                        (Reader) (rs, col) -> assertEquals(42, rs.getInt(col))),
                Arguments.of("smallint", "C_SMALLINT", (Binder) (ps, i) -> ps.setShort(i, (short) 32000),
                        (Reader) (rs, col) -> assertEquals(32000, rs.getInt(col))),
                Arguments.of("int", "C_INT", (Binder) (ps, i) -> ps.setInt(i, 1_000_001),
                        (Reader) (rs, col) -> assertEquals(1_000_001, rs.getInt(col))),
                Arguments.of("bigint", "C_BIGINT", (Binder) (ps, i) -> ps.setLong(i, 9_000_000_000_001L),
                        (Reader) (rs, col) -> assertEquals(9_000_000_000_001L, rs.getLong(col))),
                Arguments.of("float", "C_FLOAT", (Binder) (ps, i) -> ps.setFloat(i, 1.25f),
                        (Reader) (rs, col) -> assertEquals(1.25f, rs.getFloat(col), 0.0001f)),
                Arguments.of("double", "C_DOUBLE", (Binder) (ps, i) -> ps.setDouble(i, 3.1415926535d),
                        (Reader) (rs, col) -> assertEquals(3.1415926535d, rs.getDouble(col), 1e-9)),
                Arguments.of("number", "C_NUM",
                        (Binder) (ps, i) -> ps.setBigDecimal(i, new BigDecimal("12345.6789")),
                        (Reader) (rs, col) -> assertEquals(0, new BigDecimal("12345.6789").compareTo(rs.getBigDecimal(col)))),
                Arguments.of("char", "C_CHAR", (Binder) (ps, i) -> ps.setString(i, "ABCD"),
                        (Reader) (rs, col) -> assertTrue(rs.getString(col).startsWith("ABCD"))),
                Arguments.of("varchar", "C_VARCHAR", (Binder) (ps, i) -> ps.setString(i, "xugu-roundtrip"),
                        (Reader) (rs, col) -> assertEquals("xugu-roundtrip", rs.getString(col))),
                Arguments.of("text", "C_TEXT", (Binder) (ps, i) -> ps.setString(i, "long-text-" + "x".repeat(200)),
                        (Reader) (rs, col) -> assertEquals("long-text-" + "x".repeat(200), rs.getString(col))),
                Arguments.of("binary", "C_BINARY", (Binder) (ps, i) -> ps.setBytes(i, new byte[]{1, 2, 3, 4}),
                        (Reader) (rs, col) -> assertArrayEquals(new byte[]{1, 2, 3, 4}, trimBinary(rs.getBytes(col), 4))),
                Arguments.of("date", "C_DATE", (Binder) (ps, i) -> ps.setDate(i, Date.valueOf("2026-07-21")),
                        (Reader) (rs, col) -> assertEquals(Date.valueOf("2026-07-21"), rs.getDate(col))),
                Arguments.of("datetime", "C_TS",
                        (Binder) (ps, i) -> ps.setTimestamp(i, Timestamp.valueOf("2026-07-21 15:30:45")),
                        (Reader) (rs, col) -> assertEquals(Timestamp.valueOf("2026-07-21 15:30:45"), rs.getTimestamp(col))),
                Arguments.of("guid", "C_GUID",
                        (Binder) (ps, i) -> ps.setString(i, "550e8400-e29b-41d4-a716-446655440000"),
                        (Reader) (rs, col) -> assertEquals("550e8400-e29b-41d4-a716-446655440000",
                                rs.getString(col).trim().toLowerCase()))
        );
    }

    @ParameterizedTest(name = "native {0}")
    @MethodSource("scalarCases")
    void nativeRoundtrip(final String name, final String column, final Binder binder, final Reader reader)
            throws Exception {
        int id = Math.abs(name.hashCode() % 1_000_000) + 1;
        try (Connection conn = nativeConn()) {
            deleteRow(conn, id);
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (ID, " + column + ") VALUES (?, ?)")) {
                insert.setInt(1, id);
                binder.bind(insert, 2);
                assertEquals(1, insert.executeUpdate());
            }
            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT " + column + " FROM " + TABLE + " WHERE ID = ?")) {
                select.setInt(1, id);
                try (ResultSet rs = select.executeQuery()) {
                    assertTrue(rs.next());
                    reader.read(rs, column);
                    assertFalse(rs.next());
                }
            }
        }
    }

    @ParameterizedTest(name = "ss {0}")
    @MethodSource("ssCases")
    void ssRoundtrip(final String name, final String column, final Binder binder, final Reader reader)
            throws Exception {
        int id = Math.abs(("ss-" + name).hashCode() % 1_000_000) + 2_000_000;
        try (Connection conn = ssSingle.getConnection()) {
            deleteRow(conn, id);
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (ID, " + column + ") VALUES (?, ?)")) {
                insert.setInt(1, id);
                binder.bind(insert, 2);
                assertEquals(1, insert.executeUpdate());
            }
            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT " + column + " FROM " + TABLE + " WHERE ID = ?")) {
                select.setInt(1, id);
                try (ResultSet rs = select.executeQuery()) {
                    assertTrue(rs.next());
                    reader.read(rs, column);
                    assertFalse(rs.next());
                }
            }
        }
    }

    static Stream<Arguments> ssCases() {
        return Stream.of(
                Arguments.of("int", "C_INT", (Binder) (ps, i) -> ps.setInt(i, 77),
                        (Reader) (rs, col) -> assertEquals(77, rs.getInt(col))),
                Arguments.of("varchar", "C_VARCHAR", (Binder) (ps, i) -> ps.setString(i, "via-ss"),
                        (Reader) (rs, col) -> assertEquals("via-ss", rs.getString(col))),
                Arguments.of("number", "C_NUM",
                        (Binder) (ps, i) -> ps.setBigDecimal(i, new BigDecimal("99.1200")),
                        (Reader) (rs, col) -> assertEquals(0, new BigDecimal("99.1200").compareTo(rs.getBigDecimal(col)))),
                Arguments.of("datetime", "C_TS",
                        (Binder) (ps, i) -> ps.setTimestamp(i, Timestamp.valueOf("2026-01-02 03:04:05")),
                        (Reader) (rs, col) -> assertEquals(Timestamp.valueOf("2026-01-02 03:04:05"), rs.getTimestamp(col)))
        );
    }

    @Test
    void nativeNullRoundtripAllNullableColumns() throws Exception {
        int id = 9_000_001;
        try (Connection conn = nativeConn()) {
            deleteRow(conn, id);
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO " + TABLE + " (ID, C_BOOL, C_INT, C_VARCHAR, C_NUM, C_DATE, C_TS) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                insert.setInt(1, id);
                insert.setNull(2, Types.BOOLEAN);
                insert.setNull(3, Types.INTEGER);
                insert.setNull(4, Types.VARCHAR);
                insert.setNull(5, Types.NUMERIC);
                insert.setNull(6, Types.DATE);
                insert.setNull(7, Types.TIMESTAMP);
                assertEquals(1, insert.executeUpdate());
            }
            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT C_BOOL, C_INT, C_VARCHAR, C_NUM, C_DATE, C_TS FROM " + TABLE + " WHERE ID = ?")) {
                select.setInt(1, id);
                try (ResultSet rs = select.executeQuery()) {
                    assertTrue(rs.next());
                    assertNull(rs.getObject(1));
                    assertNull(rs.getObject(2));
                    assertNull(rs.getObject(3));
                    assertNull(rs.getObject(4));
                    assertNull(rs.getObject(5));
                    assertNull(rs.getObject(6));
                }
            }
        }
    }

    @FunctionalInterface
    interface Binder {
        void bind(PreparedStatement ps, int index) throws Exception;
    }

    @FunctionalInterface
    interface Reader {
        void read(ResultSet rs, String column) throws Exception;
    }

    private static byte[] trimBinary(final byte[] bytes, final int len) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length == len) {
            return bytes;
        }
        byte[] trimmed = new byte[len];
        System.arraycopy(bytes, 0, trimmed, 0, Math.min(len, bytes.length));
        return trimmed;
    }

    private static Connection nativeConn() throws Exception {
        return DriverManager.getConnection(
                props.getProperty("jdbc.url.types"),
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"));
    }

    private static void deleteRow(final Connection conn, final int id) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM " + TABLE + " WHERE ID = " + id);
        }
    }

    private static DataSource createSsDataSource(final Properties props) throws Exception {
        String yaml;
        try (InputStream in = TypesRoundtripIT.class.getClassLoader().getResourceAsStream("sharding-single-ds.yaml")) {
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        yaml = yaml.replace("${jdbc.url}", props.getProperty("jdbc.url.types"))
                .replace("${jdbc.user}", props.getProperty("jdbc.user"))
                .replace("${jdbc.password}", props.getProperty("jdbc.password"));
        return YamlShardingSphereDataSourceFactory.createDataSource(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static void dropQuietly() {
        try {
            BaselineSupport.executeOn(props, "jdbc.url.types", "DROP TABLE " + TABLE);
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
