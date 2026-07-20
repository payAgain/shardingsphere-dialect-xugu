package com.xugudb.shardingsphere.it.corpus;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.apache.shardingsphere.sql.parser.engine.api.CacheOption;
import org.apache.shardingsphere.sql.parser.engine.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.engine.api.SQLStatementVisitorEngine;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * G-005 T1 — SQL business corpus runner against XuGu lab ({@code compatiblemode=NONE}).
 *
 * <p>Uses isolated DATABASE names {@code corpus_ds0}/{@code corpus_ds1}. Catalog:
 * {@code corpus/corpus-cases.tsv} + {@code docs/sql-corpus-catalog.md}.</p>
 *
 * <p>Gates: ≥60 triaged, ≥40 PASS, 0 FAIL (DEFER allowed with reason). Maven: {@code -Psql-corpus}.</p>
 */
class SqlCorpusIT {

    private static final String DB0 = "corpus_ds0";

    private static final String DB1 = "corpus_ds1";

    private static final String TABLE = "CORPUS_T";

    private static final String SHARD_TABLE = "CORPUS_ORDER";

    private static final String DATABASE_TYPE = "XuGu";

    private static final CacheOption CACHE_OPTION = new CacheOption(128, 1024L);

    @Test
    void sqlCorpusGateAndExecutePassCases() throws Exception {
        List<SqlCorpusCase> catalog = SqlCorpusCatalogLoader.load();
        int triaged = catalog.size();
        int pass = 0;
        int defer = 0;
        int failStatus = 0;
        for (SqlCorpusCase c : catalog) {
            if (c.status() == SqlCorpusCase.Status.PASS) {
                pass++;
            } else if (c.status() == SqlCorpusCase.Status.DEFER) {
                defer++;
            } else {
                failStatus++;
            }
        }
        assertTrue(triaged >= 60, "triaged cases >= 60, was " + triaged);
        assertTrue(pass >= 40, "PASS cases >= 40, was " + pass);
        assertEquals(0, failStatus, "catalog must have 0 FAIL status rows");

        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, DB0, DB1);
        props.setProperty("jdbc.url.corpus_ds0", props.getProperty("jdbc.url." + DB0));
        props.setProperty("jdbc.url.corpus_ds1", props.getProperty("jdbc.url." + DB1));

        ensureFixtureTables(props);

        DataSource ssSingle = BaselineSupport.createDataSource("corpus/corpus-single-ds.yaml", props);
        DataSource ssShard = BaselineSupport.createDataSource("corpus/corpus-shard.yaml", props);
        List<String> executed = new ArrayList<>();
        try {
            seedRows(props, ssSingle);
            for (SqlCorpusCase c : catalog) {
                if (c.status() == SqlCorpusCase.Status.DEFER) {
                    continue;
                }
                try {
                    runPassCase(c, props, ssSingle, ssShard);
                    executed.add(c.id() + "=PASS");
                } catch (AssertionError | Exception ex) {
                    fail(c.id() + " expected PASS but failed: " + ex.getMessage(), ex);
                }
            }
        } finally {
            BaselineSupport.closeQuietly(ssShard);
            BaselineSupport.closeQuietly(ssSingle);
            cleanupFixture(props);
        }

        System.out.println("SQL_CORPUS triaged=" + triaged + " PASS=" + pass + " DEFER=" + defer
                + " executed=" + executed.size());
        for (String line : executed) {
            System.out.println("SQL_CORPUS " + line);
        }
        assertEquals(pass, executed.size(), "every PASS case must execute successfully");
    }

    private static void runPassCase(final SqlCorpusCase c, final Properties props,
                                    final DataSource ssSingle, final DataSource ssShard) throws Exception {
        if (c.expect() == SqlCorpusCase.Expect.PARSE || c.expect() == SqlCorpusCase.Expect.BOTH) {
            if (!c.isScenario()) {
                parseXuGu(c.sqlOrDesc());
            }
            if (c.expect() == SqlCorpusCase.Expect.PARSE) {
                return;
            }
        }
        if (c.isScenario()) {
            runScenario(c, props, ssSingle);
            return;
        }
        switch (c.channel()) {
            case PARSE:
                parseXuGu(c.sqlOrDesc());
                break;
            case NATIVE:
                executeNative(props, c.sqlOrDesc());
                break;
            case SS:
                executeSs(ssSingle, c.sqlOrDesc());
                break;
            case SS_SHARD:
                executeSs(ssShard, c.sqlOrDesc());
                break;
            default:
                fail("unknown channel " + c.channel());
        }
    }

    private static void runScenario(final SqlCorpusCase c, final Properties props,
                                    final DataSource ssSingle) throws Exception {
        String desc = c.sqlOrDesc();
        if (desc.contains("commit keeps row")) {
            try (Connection conn = DriverManager.getConnection(
                    props.getProperty("jdbc.url.corpus_ds0"),
                    props.getProperty("jdbc.user"),
                    props.getProperty("jdbc.password"))) {
                conn.setAutoCommit(false);
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(
                            "INSERT INTO CORPUS_T (ID, USER_ID, STATUS, AMT, NAME) VALUES (901, 1, 'TX', 1, 'c')");
                    conn.commit();
                } finally {
                    conn.setAutoCommit(true);
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM CORPUS_T WHERE ID = 901")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("DELETE FROM CORPUS_T WHERE ID = 901");
                }
            }
            return;
        }
        if (desc.contains("rollback drops row")) {
            try (Connection conn = DriverManager.getConnection(
                    props.getProperty("jdbc.url.corpus_ds0"),
                    props.getProperty("jdbc.user"),
                    props.getProperty("jdbc.password"))) {
                conn.setAutoCommit(false);
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(
                            "INSERT INTO CORPUS_T (ID, USER_ID, STATUS, AMT, NAME) VALUES (902, 1, 'TX', 1, 'r')");
                    conn.rollback();
                } finally {
                    conn.setAutoCommit(true);
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM CORPUS_T WHERE ID = 902")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            }
            return;
        }
        if (desc.contains("savepoint")) {
            try (Connection conn = ssSingle.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO CORPUS_T (ID, USER_ID, STATUS, AMT, NAME) VALUES (?, ?, ?, ?, ?)")) {
                    insert.setInt(1, 903);
                    insert.setInt(2, 1);
                    insert.setString(3, "KEEP");
                    insert.setInt(4, 1);
                    insert.setString(5, "sp");
                    assertEquals(1, insert.executeUpdate());
                    Savepoint sp = conn.setSavepoint("corpus_sp");
                    insert.setInt(1, 904);
                    insert.setInt(2, 1);
                    insert.setString(3, "DROP");
                    insert.setInt(4, 1);
                    insert.setString(5, "sp");
                    assertEquals(1, insert.executeUpdate());
                    conn.rollback(sp);
                    conn.commit();
                } finally {
                    conn.setAutoCommit(true);
                }
                try (PreparedStatement select = conn.prepareStatement(
                        "SELECT ID FROM CORPUS_T WHERE ID IN (903, 904) ORDER BY ID")) {
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(903, rs.getInt(1));
                        assertFalse(rs.next());
                    }
                }
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("DELETE FROM CORPUS_T WHERE ID IN (903, 904)");
                }
            }
            return;
        }
        if (desc.contains("batch insert")) {
            try (Connection conn = DriverManager.getConnection(
                    props.getProperty("jdbc.url.corpus_ds0"),
                    props.getProperty("jdbc.user"),
                    props.getProperty("jdbc.password"));
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO CORPUS_T (ID, USER_ID, STATUS, AMT, NAME) VALUES (?, ?, ?, ?, ?)")) {
                for (int i = 0; i < 3; i++) {
                    ps.setInt(1, 910 + i);
                    ps.setInt(2, 1);
                    ps.setString(3, "BATCH");
                    ps.setInt(4, i);
                    ps.setString(5, "b");
                    ps.addBatch();
                }
                int[] counts = ps.executeBatch();
                assertEquals(3, counts.length);
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("DELETE FROM CORPUS_T WHERE ID BETWEEN 910 AND 912");
                }
            }
            return;
        }
        fail("unknown scenario: " + desc);
    }

    private static void parseXuGu(final String sql) {
        ParseASTNode astNode = new SQLParserEngine(DATABASE_TYPE, CACHE_OPTION).parse(sql, false);
        new SQLStatementVisitorEngine(DATABASE_TYPE).visit(astNode);
    }

    private static void executeNative(final Properties props, final String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                props.getProperty("jdbc.url.corpus_ds0"),
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"));
             Statement st = conn.createStatement()) {
            executeStatement(st, sql);
        }
    }

    private static void executeSs(final DataSource dataSource, final String sql) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            executeStatement(st, sql);
        }
    }

    private static void executeStatement(final Statement st, final String sql) throws Exception {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        if (upper.startsWith("SELECT") || upper.startsWith("WITH")) {
            try (ResultSet rs = st.executeQuery(trimmed)) {
                // consume
                while (rs.next()) {
                    // ok
                }
            }
        } else {
            st.execute(trimmed);
        }
    }

    private static void ensureFixtureTables(final Properties props) throws Exception {
        for (String urlKey : new String[]{"jdbc.url.corpus_ds0", "jdbc.url.corpus_ds1"}) {
            dropQuietly(props, urlKey, TABLE);
            dropQuietly(props, urlKey, SHARD_TABLE);
            dropQuietly(props, urlKey, "CORPUS_SIMPLE");
            BaselineSupport.executeOn(props, urlKey,
                    "CREATE TABLE " + TABLE
                            + " (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32), AMT INT, NAME VARCHAR(64))");
            BaselineSupport.executeOn(props, urlKey,
                    "CREATE TABLE " + SHARD_TABLE
                            + " (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
        }
    }

    private static void seedRows(final Properties props, final DataSource ssSingle) throws Exception {
        // seed a few rows so SELECT/LIMIT/AGG cases see data (ids reserved away from DML cases)
        try (Connection conn = DriverManager.getConnection(
                props.getProperty("jdbc.url.corpus_ds0"),
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"));
             Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "INSERT INTO CORPUS_T (ID, USER_ID, STATUS, AMT, NAME) VALUES (50, 1, 'NEW', 10, 'alice')");
            st.executeUpdate(
                    "INSERT INTO CORPUS_T (ID, USER_ID, STATUS, AMT, NAME) VALUES (51, 2, 'NEW', 20, 'bob')");
            st.executeUpdate(
                    "INSERT INTO CORPUS_T (ID, USER_ID, STATUS, AMT, NAME) VALUES (52, 1, 'PAID', 30, 'amy')");
        }
        // warm SS single path
        try (Connection conn = ssSingle.getConnection(); Statement st = conn.createStatement()) {
            st.executeQuery("SELECT COUNT(*) FROM CORPUS_T").close();
        }
    }

    private static void cleanupFixture(final Properties props) {
        for (String urlKey : new String[]{"jdbc.url.corpus_ds0", "jdbc.url.corpus_ds1"}) {
            dropQuietly(props, urlKey, TABLE);
            dropQuietly(props, urlKey, SHARD_TABLE);
            dropQuietly(props, urlKey, "CORPUS_SIMPLE");
        }
    }

    private static void dropQuietly(final Properties props, final String urlKey, final String table) {
        try {
            BaselineSupport.executeOn(props, urlKey, "DROP TABLE " + table);
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
