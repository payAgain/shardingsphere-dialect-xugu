package com.xugudb.shardingsphere.database.exception.xugu.mapper;

import com.xugudb.shardingsphere.database.exception.xugu.vendor.XuguVendorError;
import org.apache.shardingsphere.database.exception.core.exception.SQLDialectException;
import org.apache.shardingsphere.database.exception.core.exception.connection.AccessDeniedException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.database.UnknownDatabaseException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.sql.DialectSQLParsingException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.table.NoSuchTableException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.table.TableExistsException;
import org.apache.shardingsphere.database.exception.core.mapper.SQLDialectExceptionMapper;
import org.apache.shardingsphere.infra.exception.external.sql.vendor.VendorError;
import org.apache.shardingsphere.infra.exception.generic.UnknownSQLException;

import java.sql.SQLException;

/**
 * XuGu dialect exception mapper.
 */
public final class XuguSQLDialectExceptionMapper implements SQLDialectExceptionMapper {
    
    @Override
    public SQLException convert(final SQLDialectException sqlDialectException) {
        if (sqlDialectException instanceof UnknownDatabaseException) {
            String databaseName = ((UnknownDatabaseException) sqlDialectException).getDatabaseName();
            return null != databaseName
                    ? toSQLException(XuguVendorError.UNKNOWN_DATABASE, databaseName)
                    : toSQLException(XuguVendorError.NO_DATABASE_SELECTED);
        }
        if (sqlDialectException instanceof TableExistsException) {
            return toSQLException(XuguVendorError.TABLE_EXISTS, ((TableExistsException) sqlDialectException).getTableName());
        }
        if (sqlDialectException instanceof NoSuchTableException) {
            return toSQLException(XuguVendorError.NO_SUCH_TABLE, ((NoSuchTableException) sqlDialectException).getTableName());
        }
        if (sqlDialectException instanceof DialectSQLParsingException) {
            DialectSQLParsingException ex = (DialectSQLParsingException) sqlDialectException;
            return toSQLException(XuguVendorError.PARSE_ERROR, ex.getMessage(), ex.getSymbol(), ex.getLine());
        }
        if (sqlDialectException instanceof AccessDeniedException) {
            AccessDeniedException ex = (AccessDeniedException) sqlDialectException;
            return toSQLException(XuguVendorError.ACCESS_DENIED, ex.getUsername(), ex.getHostname(), ex.isUsingPassword() ? "YES" : "NO");
        }
        return new UnknownSQLException(sqlDialectException).toSQLException();
    }
    
    private SQLException toSQLException(final VendorError vendorError, final Object... messageArgs) {
        return new SQLException(String.format(vendorError.getReason(), messageArgs), vendorError.getSqlState().getValue(), vendorError.getVendorCode());
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
