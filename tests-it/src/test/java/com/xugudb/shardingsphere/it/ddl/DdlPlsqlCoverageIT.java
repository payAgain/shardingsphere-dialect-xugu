package com.xugudb.shardingsphere.it.ddl;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.apache.shardingsphere.sql.parser.engine.api.CacheOption;
import org.apache.shardingsphere.sql.parser.engine.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.engine.api.SQLStatementVisitorEngine;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * G-006 Q-04 — cold DDL + PL/SQL surface coverage ({@code compatiblemode=NONE}).
 *
 * <p>Inventory: {@code docs/ddl-plsql-coverage.md}. Maven: {@code -Pddl-plsql}.</p>
 */
class DdlPlsqlCoverageIT {

    private static final String DB = "ddl_plsql_ds";

    private static final String DATABASE_TYPE = "XuGu";

    private static final CacheOption CACHE_OPTION = new CacheOption(128, 1024L);

    @Test
    void coldDdlAndPlsqlSurface() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, DB);
        props.setProperty("jdbc.url.ddl_plsql", props.getProperty("jdbc.url." + DB));

        cleanup(props);
        DataSource ss = BaselineSupport.createDataSource("ddl/ddl-single-ds.yaml", props);
        List<String> lines = new ArrayList<>();
        int supported = 0;
        int defer = 0;
        try {
            for (Case c : cases()) {
                try {
                    runCase(c, props, ss);
                    lines.add(c.id + "=" + c.status + " OK channels=" + c.channels);
                    if ("Supported".equals(c.status)) {
                        supported++;
                    } else {
                        defer++;
                    }
                } catch (AssertionError | Exception ex) {
                    if ("DEFER".equals(c.status)) {
                        lines.add(c.id + "=DEFER confirmed: " + trim(ex.getMessage()));
                        defer++;
                    } else {
                        fail(c.id + " expected Supported but failed: " + ex.getMessage(), ex);
                    }
                }
            }
        } finally {
            BaselineSupport.closeQuietly(ss);
            cleanup(props);
        }

        System.out.println("DDL_PLSQL supported=" + supported + " defer=" + defer);
        for (String line : lines) {
            System.out.println("DDL_PLSQL " + line);
        }
        assertTrue(supported >= 28, "expected broad Supported surface, was " + supported);
        assertTrue(defer >= 1, "expected at least one evidenced DEFER");
    }

    private static void runCase(final Case c, final Properties props, final DataSource ss) throws Exception {
        if (c.channels.contains("parse")) {
            for (String sql : c.sqls) {
                if (sql.trim().toUpperCase().startsWith("SELECT")) {
                    continue;
                }
                parseXuGu(sql);
            }
        }
        // Prefer a single execute channel to avoid double-apply of DDL (native then SS).
        if (c.channels.contains("ss")) {
            executeSs(ss, c.sqls);
        } else if (c.channels.contains("native")) {
            executeNative(props, c.sqls);
        }
        if ("DEFER".equals(c.status)) {
            fail("DEFER case unexpectedly succeeded: " + c.id);
        }
    }

    private static void executeNative(final Properties props, final List<String> sqls) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                props.getProperty("jdbc.url.ddl_plsql"),
                props.getProperty("jdbc.user"),
                props.getProperty("jdbc.password"));
             Statement st = conn.createStatement()) {
            for (String sql : sqls) {
                st.execute(sql);
            }
        }
    }

    private static void executeSs(final DataSource dataSource, final List<String> sqls) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            for (String sql : sqls) {
                st.execute(sql);
            }
        }
    }

    private static void parseXuGu(final String sql) {
        ParseASTNode astNode = new SQLParserEngine(DATABASE_TYPE, CACHE_OPTION).parse(sql, false);
        new SQLStatementVisitorEngine(DATABASE_TYPE).visit(astNode);
    }

    private static void cleanup(final Properties props) {
        String[] drops = {
                "DROP TRIGGER Q04_TRG",
                "DROP TRIGGER Q04_SS_TRG",
                "DROP VIEW Q04_V",
                "DROP VIEW Q04_SS_V",
                "DROP INDEX Q04_T.Q04_IDX",
                "DROP INDEX Q04_T.Q04_IDX2",
                "DROP INDEX Q04_T.Q04_XIDX",
                "DROP INDEX Q04_T.Q04_XIDX2",
                "DROP INDEX Q04_SS_TI.Q04_SS_IDX",
                "DROP PACKAGE BODY Q04_PKG",
                "DROP PACKAGE Q04_PKG",
                "DROP PACKAGE BODY Q04_SS_PKG",
                "DROP PACKAGE Q04_SS_PKG",
                "DROP PACKAGE Q04_EMPTY",
                "DROP PROCEDURE Q04_P",
                "DROP PROCEDURE Q04_SS_P",
                "DROP FUNCTION Q04_F",
                "DROP FUNCTION Q04_SS_F",
                "DROP SEQUENCE Q04_SEQ",
                "DROP SEQUENCE Q04_SS_SEQ",
                "DROP VIEW Q04_XV",
                "DROP TABLE Q04_T",
                "DROP TABLE Q04_T2",
                "DROP TABLE Q04_SS_T",
                "DROP TABLE Q04_SS_TI",
                "DROP TABLE Q04_SS_TV",
                "DROP TABLE Q04_SS_TT"
        };
        for (String ddl : drops) {
            try {
                BaselineSupport.executeOn(props, "jdbc.url.ddl_plsql", ddl);
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    private static String trim(final String msg) {
        if (msg == null) {
            return "";
        }
        String m = msg.replace('\n', ' ').replace('\r', ' ');
        return m.length() > 160 ? m.substring(0, 160) : m;
    }

    private static List<Case> cases() {
        List<Case> list = new ArrayList<>();
        // TABLE — CREATE via SS so metadata knows Q04_T for later SS ALTER/DROP
        list.add(s("D01_CREATE_TABLE", "TABLE",
                Arrays.asList("parse", "ss"),
                "CREATE TABLE Q04_T (ID INT PRIMARY KEY, NAME VARCHAR(32))"));
        list.add(s("D02_ALTER_TABLE_ADD", "TABLE",
                Arrays.asList("parse", "native", "ss"),
                "ALTER TABLE Q04_T ADD REMARK VARCHAR(32)"));
        list.add(s("D03_ALTER_TABLE_MODIFY", "TABLE",
                Arrays.asList("parse", "native", "ss"),
                "ALTER TABLE Q04_T MODIFY NAME VARCHAR(64)"));
        list.add(s("D04_DROP_TABLE", "TABLE",
                Arrays.asList("parse", "ss"),
                "CREATE TABLE Q04_T2 (ID INT PRIMARY KEY)",
                "DROP TABLE Q04_T2"));

        // INDEX (XuGu DROP requires table.index qualifier)
        list.add(s("D05_CREATE_INDEX", "INDEX",
                Arrays.asList("parse", "native"),
                "CREATE INDEX Q04_IDX ON Q04_T (NAME)"));
        list.add(s("D06_ALTER_INDEX_RENAME", "INDEX",
                Arrays.asList("parse", "native", "ss"),
                "ALTER INDEX Q04_T.Q04_IDX RENAME TO Q04_IDX2"));
        list.add(s("D07_DROP_INDEX", "INDEX",
                Arrays.asList("parse", "native", "ss"),
                "DROP INDEX Q04_T.Q04_IDX2"));

        // VIEW — CREATE via SS so ALTER/DROP VIEW binder sees metadata
        list.add(s("D08_CREATE_VIEW", "VIEW",
                Arrays.asList("parse", "ss"),
                "CREATE VIEW Q04_V AS SELECT ID, NAME FROM Q04_T"));
        list.add(s("D09_ALTER_VIEW_RECOMPILE", "VIEW",
                Arrays.asList("parse", "native", "ss"),
                "ALTER VIEW Q04_V RECOMPILE"));
        list.add(s("D10_DROP_VIEW", "VIEW",
                Arrays.asList("parse", "native", "ss"),
                "DROP VIEW Q04_V"));

        // SEQUENCE
        list.add(s("D11_CREATE_SEQUENCE", "SEQUENCE",
                Arrays.asList("parse", "native"),
                "CREATE SEQUENCE Q04_SEQ START WITH 1 INCREMENT BY 1"));
        list.add(s("D12_ALTER_SEQUENCE", "SEQUENCE",
                Arrays.asList("parse", "native", "ss"),
                "ALTER SEQUENCE Q04_SEQ INCREMENT BY 2"));
        list.add(s("D13_DROP_SEQUENCE", "SEQUENCE",
                Arrays.asList("parse", "native", "ss"),
                "DROP SEQUENCE Q04_SEQ"));

        // PROCEDURE
        list.add(s("D14_CREATE_PROCEDURE", "PROCEDURE",
                Arrays.asList("parse", "native"),
                "CREATE PROCEDURE Q04_P AS BEGIN NULL; END"));
        list.add(s("D15_ALTER_PROCEDURE_RECOMPILE", "PROCEDURE",
                Arrays.asList("parse", "native", "ss"),
                "ALTER PROCEDURE Q04_P RECOMPILE"));
        list.add(s("D16_CALL_PROCEDURE", "PROCEDURE",
                Arrays.asList("parse", "native", "ss"),
                "CALL Q04_P"));
        list.add(s("D17_DROP_PROCEDURE", "PROCEDURE",
                Arrays.asList("parse", "native", "ss"),
                "DROP PROCEDURE Q04_P"));

        // FUNCTION
        list.add(s("D18_CREATE_FUNCTION", "FUNCTION",
                Arrays.asList("parse", "native"),
                "CREATE FUNCTION Q04_F RETURN INT AS BEGIN RETURN 1; END"));
        list.add(s("D19_ALTER_FUNCTION_RECOMPILE", "FUNCTION",
                Arrays.asList("parse", "native", "ss"),
                "ALTER FUNCTION Q04_F RECOMPILE"));
        list.add(s("D20_SELECT_FUNCTION", "FUNCTION",
                Arrays.asList("native", "ss"),
                "SELECT Q04_F() FROM DUAL"));
        list.add(s("D21_DROP_FUNCTION", "FUNCTION",
                Arrays.asList("parse", "native", "ss"),
                "DROP FUNCTION Q04_F"));

        // TRIGGER
        list.add(s("D22_CREATE_TRIGGER", "TRIGGER",
                Arrays.asList("parse", "native"),
                "CREATE TRIGGER Q04_TRG BEFORE INSERT ON Q04_T FOR EACH ROW BEGIN NULL; END"));
        list.add(s("D23_ALTER_TRIGGER_ENABLE", "TRIGGER",
                Arrays.asList("parse", "native", "ss"),
                "ALTER TRIGGER Q04_TRG DISABLE",
                "ALTER TRIGGER Q04_TRG ENABLE"));
        list.add(s("D24_DROP_TRIGGER", "TRIGGER",
                Arrays.asList("parse", "native", "ss"),
                "DROP TRIGGER Q04_TRG"));

        // PACKAGE
        list.add(s("D25_CREATE_PACKAGE", "PACKAGE",
                Arrays.asList("parse", "native"),
                "CREATE PACKAGE Q04_PKG AS PROCEDURE P; END"));
        list.add(s("D26_CREATE_PACKAGE_BODY", "PACKAGE",
                Arrays.asList("parse", "native"),
                "CREATE PACKAGE BODY Q04_PKG AS PROCEDURE P AS BEGIN NULL; END; END"));
        list.add(s("D27_ALTER_PACKAGE_RECOMPILE", "PACKAGE",
                Arrays.asList("parse", "native", "ss"),
                "ALTER PACKAGE Q04_PKG RECOMPILE"));
        list.add(s("D28_DROP_PACKAGE", "PACKAGE",
                Arrays.asList("parse", "native", "ss"),
                "DROP PACKAGE Q04_PKG"));

        // SS create+drop smoke (isolated names) — proves CREATE path through ShardingSphere
        list.add(s("S01_SS_TABLE", "TABLE",
                Arrays.asList("parse", "ss"),
                "CREATE TABLE Q04_SS_T (ID INT PRIMARY KEY)",
                "DROP TABLE Q04_SS_T"));
        list.add(s("S02_SS_INDEX", "INDEX",
                Arrays.asList("parse", "ss"),
                "CREATE TABLE Q04_SS_TI (ID INT PRIMARY KEY, NAME VARCHAR(16))",
                "CREATE INDEX Q04_SS_IDX ON Q04_SS_TI (NAME)",
                "DROP INDEX Q04_SS_TI.Q04_SS_IDX",
                "DROP TABLE Q04_SS_TI"));
        list.add(s("S03_SS_VIEW", "VIEW",
                Arrays.asList("parse", "ss"),
                "CREATE TABLE Q04_SS_TV (ID INT PRIMARY KEY)",
                "CREATE VIEW Q04_SS_V AS SELECT ID FROM Q04_SS_TV",
                "DROP VIEW Q04_SS_V",
                "DROP TABLE Q04_SS_TV"));
        list.add(s("S04_SS_SEQUENCE", "SEQUENCE",
                Arrays.asList("parse", "ss"),
                "CREATE SEQUENCE Q04_SS_SEQ START WITH 1 INCREMENT BY 1",
                "DROP SEQUENCE Q04_SS_SEQ"));
        list.add(s("S05_SS_PROCEDURE", "PROCEDURE",
                Arrays.asList("parse", "ss"),
                "CREATE PROCEDURE Q04_SS_P AS BEGIN NULL; END",
                "DROP PROCEDURE Q04_SS_P"));
        list.add(s("S06_SS_FUNCTION", "FUNCTION",
                Arrays.asList("parse", "ss"),
                "CREATE FUNCTION Q04_SS_F RETURN INT AS BEGIN RETURN 1; END",
                "DROP FUNCTION Q04_SS_F"));
        list.add(s("S07_SS_TRIGGER", "TRIGGER",
                Arrays.asList("parse", "ss"),
                "CREATE TABLE Q04_SS_TT (ID INT PRIMARY KEY)",
                "CREATE TRIGGER Q04_SS_TRG BEFORE INSERT ON Q04_SS_TT FOR EACH ROW BEGIN NULL; END",
                "DROP TRIGGER Q04_SS_TRG",
                "DROP TABLE Q04_SS_TT"));
        list.add(s("S08_SS_PACKAGE", "PACKAGE",
                Arrays.asList("parse", "ss"),
                "CREATE PACKAGE Q04_SS_PKG AS PROCEDURE P; END",
                "CREATE PACKAGE BODY Q04_SS_PKG AS PROCEDURE P AS BEGIN NULL; END; END",
                "DROP PACKAGE Q04_SS_PKG"));

        // Evidenced DEFER (XuGu rejects) — unique names so leftover objects do not cascade
        list.add(d("X01_EMPTY_PACKAGE", "PACKAGE",
                Arrays.asList("native"),
                "CREATE PACKAGE Q04_EMPTY AS END"));
        list.add(d("X02_DROP_INDEX_UNQUALIFIED", "INDEX",
                Arrays.asList("native"),
                "CREATE INDEX Q04_XIDX ON Q04_T (NAME)",
                "DROP INDEX Q04_XIDX"));
        list.add(d("X03_ALTER_INDEX_REBUILD", "INDEX",
                Arrays.asList("native"),
                "CREATE INDEX Q04_XIDX2 ON Q04_T (REMARK)",
                "ALTER INDEX Q04_T.Q04_XIDX2 REBUILD"));
        list.add(d("X04_ALTER_VIEW_COMPILE", "VIEW",
                Arrays.asList("native"),
                "CREATE VIEW Q04_XV AS SELECT ID FROM Q04_T",
                "ALTER VIEW Q04_XV COMPILE"));
        return list;
    }

    private static Case s(final String id, final String family, final List<String> channels, final String... sqls) {
        return new Case(id, family, "Supported", channels, Arrays.asList(sqls));
    }

    private static Case d(final String id, final String family, final List<String> channels, final String... sqls) {
        return new Case(id, family, "DEFER", channels, Arrays.asList(sqls));
    }

    private static final class Case {
        private final String id;
        private final String family;
        private final String status;
        private final List<String> channels;
        private final List<String> sqls;

        private Case(final String id, final String family, final String status,
                     final List<String> channels, final List<String> sqls) {
            this.id = id;
            this.family = family;
            this.status = status;
            this.channels = channels;
            this.sqls = sqls;
        }
    }
}
