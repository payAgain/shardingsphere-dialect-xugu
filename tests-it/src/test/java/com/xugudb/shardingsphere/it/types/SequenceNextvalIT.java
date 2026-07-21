package com.xugudb.shardingsphere.it.types;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
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
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G-T4 — SEQUENCE NEXTVAL / CURRVAL path (native + SS single-DS).
 */
class SequenceNextvalIT {

    private static final String DB = "seq_rt_ds0";

    private static Properties props;

    private static DataSource ssSingle;

    private static String seqName;

    @BeforeAll
    static void setUp() throws Exception {
        props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, DB);
        props.setProperty("jdbc.url.seq", props.getProperty("jdbc.url." + DB));
        seqName = "SEQ_P0_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        dropQuietly(seqName);
        try (Connection conn = nativeConn(); Statement st = conn.createStatement()) {
            st.execute("CREATE SEQUENCE " + seqName
                    + " START WITH 10 INCREMENT BY 5 MINVALUE 1 MAXVALUE 1000000 NO CYCLE");
        }
        ssSingle = createSsDataSource(props);
    }

    @AfterAll
    static void tearDown() {
        BaselineSupport.closeQuietly(ssSingle);
        dropQuietly(seqName);
    }

    @Test
    void nativeNextvalIncrementsByStep() throws Exception {
        try (Connection conn = nativeConn()) {
            long first = scalarLong(conn, "SELECT " + seqName + ".NEXTVAL FROM DUAL");
            long second = scalarLong(conn, "SELECT " + seqName + ".NEXTVAL FROM DUAL");
            assertEquals(first + 5, second, "NEXTVAL should advance by INCREMENT BY 5");
        }
    }

    @Test
    void nativeCurrvalMatchesLastNextval() throws Exception {
        // XuGu NONE: CURRVAL is a function CURRVAL('SEQ'), not SEQ.CURRVAL (Oracle form).
        try (Connection conn = nativeConn()) {
            long next = scalarLong(conn, "SELECT " + seqName + ".NEXTVAL FROM DUAL");
            long curr = scalarLong(conn, "SELECT CURRVAL('" + seqName + "') FROM DUAL");
            assertEquals(next, curr, "CURRVAL('seq') should equal last NEXTVAL in session");
        }
    }

    @Test
    void ssNextvalReturnsNumeric() throws Exception {
        try (Connection conn = ssSingle.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT " + seqName + ".NEXTVAL FROM DUAL")) {
            assertTrue(rs.next());
            long value = rs.getLong(1);
            assertTrue(value >= 10, "SS NEXTVAL should be >= START WITH, was " + value);
        }
    }

    @Test
    void nativeCreateDropSequenceRoundtrip() throws Exception {
        String ephemeral = "SEQ_EP_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        try (Connection conn = nativeConn(); Statement st = conn.createStatement()) {
            st.execute("CREATE SEQUENCE " + ephemeral + " START WITH 1 INCREMENT BY 1");
            long v = scalarLong(conn, "SELECT " + ephemeral + ".NEXTVAL FROM DUAL");
            assertEquals(1L, v);
            st.execute("DROP SEQUENCE " + ephemeral);
        } finally {
            dropQuietly(ephemeral);
        }
    }

    private static long scalarLong(final Connection conn, final String sql) throws Exception {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getLong(1);
        }
    }

    private static Connection nativeConn() throws Exception {
        return DriverManager.getConnection(
                props.getProperty("jdbc.url.seq"),
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"));
    }

    private static DataSource createSsDataSource(final Properties props) throws Exception {
        String yaml;
        try (InputStream in = SequenceNextvalIT.class.getClassLoader().getResourceAsStream("sharding-single-ds.yaml")) {
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        yaml = yaml.replace("${jdbc.url}", props.getProperty("jdbc.url.seq"))
                .replace("${jdbc.user}", props.getProperty("jdbc.user"))
                .replace("${jdbc.password}", props.getProperty("jdbc.password"));
        return YamlShardingSphereDataSourceFactory.createDataSource(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static void dropQuietly(final String name) {
        if (name == null || props == null || !props.containsKey("jdbc.url.seq")) {
            return;
        }
        try {
            BaselineSupport.executeOn(props, "jdbc.url.seq", "DROP SEQUENCE " + name);
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
