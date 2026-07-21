package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor;

import lombok.Getter;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.raw.metadata.RawQueryResultColumnMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.raw.metadata.RawQueryResultMetaData;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.merge.result.impl.local.LocalDataMergedResult;
import org.apache.shardingsphere.infra.merge.result.impl.local.LocalDataQueryResultRow;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutor;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminQueryExecutor;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dal.VariableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ExpressionProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Answers MySQL-client {@code SELECT @@...} handshake queries without hitting XuGu storage.
 *
 * <p>Wire protocol is MySQL while storage is XuGu {@code compatiblemode=NONE}; Connector/J sends
 * session-variable probes that XuGu cannot execute.</p>
 */
public final class XuguMySQLSystemVariableQueryExecutor implements DatabaseAdminQueryExecutor {

    private static final Pattern ALIAS_PATTERN = Pattern.compile(
            "(?i)@@(?:session\\.|global\\.)?([a-z0-9_]+)\\s+AS\\s+([a-z0-9_]+)");

    private static final Map<String, String> DEFAULT_VALUES;

    static {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("auto_increment_increment", "1");
        values.put("character_set_client", "utf8mb4");
        values.put("character_set_connection", "utf8mb4");
        values.put("character_set_results", "utf8mb4");
        values.put("character_set_server", "utf8mb4");
        values.put("collation_server", "utf8mb4_0900_ai_ci");
        values.put("collation_connection", "utf8mb4_0900_ai_ci");
        values.put("init_connect", "");
        values.put("interactive_timeout", "28800");
        values.put("license", "GPL");
        values.put("lower_case_table_names", "0");
        values.put("max_allowed_packet", "67108864");
        values.put("net_write_timeout", "60");
        values.put("performance_schema", "0");
        values.put("query_cache_size", "0");
        values.put("query_cache_type", "OFF");
        values.put("sql_mode", "STRICT_TRANS_TABLES");
        values.put("system_time_zone", "UTC");
        values.put("time_zone", "UTC");
        values.put("transaction_isolation", "REPEATABLE-READ");
        values.put("tx_isolation", "REPEATABLE-READ");
        values.put("wait_timeout", "28800");
        DEFAULT_VALUES = Collections.unmodifiableMap(values);
    }

    private final List<String> columnLabels;

    private final List<String> values;

    @Getter
    private QueryResultMetaData queryResultMetaData;

    @Getter
    private MergedResult mergedResult;

    private XuguMySQLSystemVariableQueryExecutor(final List<String> columnLabels, final List<String> values) {
        this.columnLabels = columnLabels;
        this.values = values;
    }

    /**
     * Create executor when the SELECT is a MySQL system-variable probe (no FROM, only {@code @@} projections).
     */
    public static Optional<DatabaseAdminExecutor> tryCreate(final SelectStatement selectStatement, final String sql) {
        Optional<DatabaseAdminExecutor> fromAst = tryCreateFromAst(selectStatement);
        if (fromAst.isPresent()) {
            return fromAst;
        }
        return tryCreateFromSql(sql);
    }

    private static Optional<DatabaseAdminExecutor> tryCreateFromAst(final SelectStatement selectStatement) {
        if (selectStatement == null || selectStatement.getFrom().isPresent()) {
            return Optional.empty();
        }
        ProjectionsSegment projectionsSegment = selectStatement.getProjections();
        if (projectionsSegment == null || projectionsSegment.getProjections().isEmpty()) {
            return Optional.empty();
        }
        List<String> labels = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (ProjectionSegment projection : projectionsSegment.getProjections()) {
            if (!(projection instanceof ExpressionProjectionSegment)) {
                return Optional.empty();
            }
            ExpressionProjectionSegment expressionProjection = (ExpressionProjectionSegment) projection;
            if (!(expressionProjection.getExpr() instanceof VariableSegment)) {
                return Optional.empty();
            }
            VariableSegment variable = (VariableSegment) expressionProjection.getExpr();
            String variableName = variable.getVariable().toLowerCase(Locale.ROOT);
            String label = expressionProjection.getAliasName().orElseGet(() -> "@@" + variableName);
            labels.add(label);
            values.add(DEFAULT_VALUES.getOrDefault(variableName, ""));
        }
        return Optional.of(new XuguMySQLSystemVariableQueryExecutor(labels, values));
    }

    private static Optional<DatabaseAdminExecutor> tryCreateFromSql(final String sql) {
        if (sql == null || sql.isEmpty()) {
            return Optional.empty();
        }
        String stripped = sql.replaceAll("(?s)/\\*.*?\\*/", " ").trim();
        String lower = stripped.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("select") || !stripped.contains("@@") || lower.contains(" from ")) {
            return Optional.empty();
        }
        Matcher matcher = ALIAS_PATTERN.matcher(stripped);
        List<String> labels = new ArrayList<>();
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            String variableName = matcher.group(1).toLowerCase(Locale.ROOT);
            labels.add(matcher.group(2));
            values.add(DEFAULT_VALUES.getOrDefault(variableName, ""));
        }
        if (labels.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new XuguMySQLSystemVariableQueryExecutor(labels, values));
    }

    @Override
    public void execute(final ConnectionSession connectionSession, final ShardingSphereMetaData metaData) {
        List<RawQueryResultColumnMetaData> columns = new ArrayList<>(columnLabels.size());
        for (String label : columnLabels) {
            columns.add(new RawQueryResultColumnMetaData("", label, label, Types.VARCHAR, "VARCHAR", 1024, 0));
        }
        queryResultMetaData = new RawQueryResultMetaData(columns);
        mergedResult = new LocalDataMergedResult(Collections.singletonList(new LocalDataQueryResultRow(values.toArray())));
    }
}
