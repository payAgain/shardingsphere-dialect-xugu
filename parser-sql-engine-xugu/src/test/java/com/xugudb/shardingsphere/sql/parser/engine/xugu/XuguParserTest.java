package com.xugudb.shardingsphere.sql.parser.engine.xugu;

import org.apache.shardingsphere.sql.parser.engine.api.CacheOption;
import org.apache.shardingsphere.sql.parser.engine.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.engine.api.SQLStatementVisitorEngine;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.table.CreateTableStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.ddl.table.DropTableStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.DeleteStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.InsertStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.UpdateStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.tcl.CommitStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.tcl.RollbackStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class XuguParserTest {
    
    private static final String DATABASE_TYPE = "XuGu";
    
    private static final CacheOption CACHE_OPTION = new CacheOption(128, 1024L);
    
    @Test
    void assertWhitelistParses() {
        String[] sqls = {
                "SELECT 1 FROM DUAL",
                "SELECT id, name FROM t_order WHERE id = 1",
                "INSERT INTO t_order (id, name) VALUES (1, 'a')",
                "UPDATE t_order SET name = 'b' WHERE id = 1",
                "DELETE FROM t_order WHERE id = 1",
                "CREATE TABLE t_order (id INT PRIMARY KEY, name VARCHAR(64))",
                "DROP TABLE t_order",
                "COMMIT",
                "ROLLBACK",
                "SELECT 1 FROM DUAL LIMIT 1"
        };
        for (String each : sqls) {
            assertDoesNotThrow(() -> parse(each), () -> "failed to parse: " + each);
        }
    }
    
    @ParameterizedTest
    @MethodSource("typedWhitelist")
    void assertWhitelistStatementTypes(final String sql, final Class<? extends SQLStatement> expectedType) {
        SQLStatement statement = parse(sql);
        assertThat(sql, statement, instanceOf(expectedType));
    }
    
    private static Stream<Arguments> typedWhitelist() {
        return Stream.of(
                Arguments.of("SELECT 1 FROM DUAL", SelectStatement.class),
                Arguments.of("SELECT id, name FROM t_order WHERE id = 1", SelectStatement.class),
                Arguments.of("INSERT INTO t_order (id, name) VALUES (1, 'a')", InsertStatement.class),
                Arguments.of("UPDATE t_order SET name = 'b' WHERE id = 1", UpdateStatement.class),
                Arguments.of("DELETE FROM t_order WHERE id = 1", DeleteStatement.class),
                Arguments.of("CREATE TABLE t_order (id INT PRIMARY KEY, name VARCHAR(64))", CreateTableStatement.class),
                Arguments.of("DROP TABLE t_order", DropTableStatement.class),
                Arguments.of("COMMIT", CommitStatement.class),
                Arguments.of("ROLLBACK", RollbackStatement.class),
                Arguments.of("SELECT 1 FROM DUAL LIMIT 1", SelectStatement.class));
    }
    
    private static SQLStatement parse(final String sql) {
        ParseASTNode astNode = new SQLParserEngine(DATABASE_TYPE, CACHE_OPTION).parse(sql, false);
        return new SQLStatementVisitorEngine(DATABASE_TYPE).visit(astNode);
    }
}
