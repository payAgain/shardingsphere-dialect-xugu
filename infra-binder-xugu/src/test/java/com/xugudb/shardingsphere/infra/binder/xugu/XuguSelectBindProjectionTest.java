package com.xugudb.shardingsphere.infra.binder.xugu;

import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.database.connector.core.type.DatabaseTypeRegistry;
import org.apache.shardingsphere.infra.binder.context.segment.select.projection.Projection;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.type.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.binder.engine.SQLBindEngine;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.hint.HintValueContext;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.resource.ResourceMetaData;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereColumn;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereTable;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sql.parser.engine.api.CacheOption;
import org.apache.shardingsphere.sql.parser.engine.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.engine.api.SQLStatementVisitorEngine;
import org.apache.shardingsphere.sql.parser.engine.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Parse + bind path: XuGu parser → SQLBindEngine → projections use UPPER_CASE identifiers.
 */
class XuguSelectBindProjectionTest {
    
    private static final String DATABASE_TYPE = "XuGu";
    
    private static final CacheOption CACHE_OPTION = new CacheOption(128, 1024L);
    
    @Test
    void assertSelectProjectionsUpperCaseAfterBind() {
        String sql = "SELECT id, name FROM t_order WHERE id = 1";
        SQLStatement sqlStatement = parse(sql);
        sqlStatement.buildAttributes();
        
        DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, DATABASE_TYPE);
        SQLStatementContext statementContext = new SQLBindEngine(mockMetaData(databaseType), "foo_db", new HintValueContext()).bind(sqlStatement);
        
        assertThat(statementContext, instanceOf(SelectStatementContext.class));
        Collection<String> labels = ((SelectStatementContext) statementContext).getProjectionsContext().getProjections().stream()
                .map(Projection::getColumnLabel)
                .collect(Collectors.toList());
        assertThat(labels, containsInAnyOrder("ID", "NAME"));
    }
    
    private static SQLStatement parse(final String sql) {
        ParseASTNode astNode = new SQLParserEngine(DATABASE_TYPE, CACHE_OPTION).parse(sql, false);
        return new SQLStatementVisitorEngine(DATABASE_TYPE).visit(astNode);
    }
    
    private static ShardingSphereMetaData mockMetaData(final DatabaseType databaseType) {
        String defaultSchemaName = new DatabaseTypeRegistry(databaseType).getDefaultSchemaName("foo_db");
        ShardingSphereTable orderTable = new ShardingSphereTable("t_order", Arrays.asList(
                new ShardingSphereColumn("id", Types.INTEGER, true, false, false, true, false, false),
                new ShardingSphereColumn("name", Types.VARCHAR, false, false, false, true, false, false)),
                Collections.emptyList(), Collections.emptyList());
        ShardingSphereSchema schema = new ShardingSphereSchema(defaultSchemaName, databaseType,
                Collections.singletonList(orderTable), Collections.emptyList());
        ResourceMetaData resourceMetaData = new ResourceMetaData(Collections.emptyMap());
        RuleMetaData ruleMetaData = new RuleMetaData(Collections.emptyList());
        ShardingSphereDatabase database = new ShardingSphereDatabase("foo_db", databaseType,
                resourceMetaData, ruleMetaData, Collections.singletonList(schema));
        return new ShardingSphereMetaData(Collections.singleton(database), resourceMetaData,
                ruleMetaData, new ConfigurationProperties(new Properties()));
    }
}
