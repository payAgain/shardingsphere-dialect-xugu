package com.xugudb.shardingsphere.sqlfederation.xugu;

import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.shardingsphere.sqlfederation.resultset.converter.DialectSQLFederationColumnTypeConverter;

/**
 * SQL federation column type converter for XuGu.
 */
public final class XuguSQLFederationColumnTypeConverter implements DialectSQLFederationColumnTypeConverter {
    
    @Override
    public Object convertColumnValue(final Object columnValue) {
        if (columnValue instanceof Boolean) {
            return (Boolean) columnValue ? 1 : 0;
        }
        return columnValue;
    }
    
    @Override
    public int convertColumnType(final SqlTypeName sqlTypeName) {
        int result = sqlTypeName.getJdbcOrdinal();
        if (SqlTypeName.BOOLEAN.getJdbcOrdinal() == result || SqlTypeName.ANY.getJdbcOrdinal() == result) {
            return SqlTypeName.VARCHAR.getJdbcOrdinal();
        }
        return result;
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
