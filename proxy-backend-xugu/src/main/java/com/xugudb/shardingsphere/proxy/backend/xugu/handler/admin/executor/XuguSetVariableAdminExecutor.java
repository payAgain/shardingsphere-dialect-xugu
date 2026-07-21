package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminUpdateExecutor;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.variable.charset.CharsetSetExecutor;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.variable.session.SessionVariableRecordExecutor;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dal.VariableAssignSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.SetStatement;

/**
 * Set variable admin executor for XuGu.
 */
@RequiredArgsConstructor
public final class XuguSetVariableAdminExecutor implements DatabaseAdminUpdateExecutor {

    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");

    private final SetStatement setStatement;

    @Override
    public void execute(final ConnectionSession connectionSession, final ShardingSphereMetaData metaData) {
        VariableAssignSegment variableAssignSegment = setStatement.getVariableAssigns().iterator().next();
        String variableName = variableAssignSegment.getVariable().getVariable().toLowerCase();
        String assignValue = variableAssignSegment.getAssignValue();
        new CharsetSetExecutor(databaseType, connectionSession).set(variableName, assignValue);
        new SessionVariableRecordExecutor(databaseType, connectionSession).recordVariable(variableName, assignValue);
    }
}
