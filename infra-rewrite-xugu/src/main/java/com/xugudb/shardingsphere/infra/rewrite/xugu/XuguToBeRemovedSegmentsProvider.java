package com.xugudb.shardingsphere.infra.rewrite.xugu;

import org.apache.shardingsphere.infra.rewrite.sql.token.common.generator.generic.DialectToBeRemovedSegmentsProvider;
import org.apache.shardingsphere.sql.parser.statement.core.segment.SQLSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

import java.util.Collection;
import java.util.Collections;

/**
 * Dialect to-be-removed segments provider for XuGu.
 * Minimal stub: XuGu has no MySQL-style SHOW ... FROM database segments to strip.
 */
public final class XuguToBeRemovedSegmentsProvider implements DialectToBeRemovedSegmentsProvider {
    
    @Override
    public Collection<SQLSegment> getToBeRemovedSQLSegments(final SQLStatement sqlStatement) {
        return Collections.emptyList();
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
