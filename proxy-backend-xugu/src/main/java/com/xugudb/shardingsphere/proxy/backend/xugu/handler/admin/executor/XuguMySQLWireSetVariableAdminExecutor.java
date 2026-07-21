package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminUpdateExecutor;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.SetStatement;

/**
 * Accepts MySQL-wire {@code SET ...} from Connector/J without forwarding to XuGu storage.
 *
 * <p>Example handshake: {@code SET character_set_results = NULL} is valid MySQL but not XuGu.</p>
 */
@RequiredArgsConstructor
public final class XuguMySQLWireSetVariableAdminExecutor implements DatabaseAdminUpdateExecutor {

    @SuppressWarnings("unused")
    private final SetStatement setStatement;

    @Override
    public void execute(final ConnectionSession connectionSession, final ShardingSphereMetaData metaData) {
        // no-op: record-only / ignore MySQL client session SETs for XuGu NONE storage
    }
}
