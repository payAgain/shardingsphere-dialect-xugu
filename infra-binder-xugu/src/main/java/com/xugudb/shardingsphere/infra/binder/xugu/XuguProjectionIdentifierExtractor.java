package com.xugudb.shardingsphere.infra.binder.xugu;

import org.apache.shardingsphere.infra.binder.context.segment.select.projection.extractor.DialectProjectionIdentifierExtractor;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.SubqueryProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.value.identifier.IdentifierValue;

/**
 * Projection identifier extractor for XuGu (UPPER_CASE, aligned with Oracle-style identifiers).
 */
public final class XuguProjectionIdentifierExtractor implements DialectProjectionIdentifierExtractor {
    
    @Override
    public String getIdentifierValue(final IdentifierValue identifierValue) {
        return identifierValue.getValue().toUpperCase();
    }
    
    @Override
    public String getColumnNameFromFunction(final String functionName, final String functionExpression) {
        return functionExpression.replace(" ", "").toUpperCase();
    }
    
    @Override
    public String getColumnNameFromExpression(final ExpressionSegment expressionSegment) {
        return expressionSegment.getText().replace(" ", "").toUpperCase();
    }
    
    @Override
    public String getColumnNameFromSubquery(final SubqueryProjectionSegment subquerySegment) {
        return subquerySegment.getText().replace(" ", "").toUpperCase();
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
