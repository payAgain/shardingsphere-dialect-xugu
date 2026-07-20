package com.xugudb.shardingsphere.database.exception.xugu.mapper;

import com.xugudb.shardingsphere.database.exception.xugu.exception.ConnectionFailedException;
import com.xugudb.shardingsphere.database.exception.xugu.exception.DuplicateKeyException;
import com.xugudb.shardingsphere.database.exception.xugu.exception.NullNotAllowedException;
import com.xugudb.shardingsphere.database.exception.xugu.exception.QueryTimeoutException;
import com.xugudb.shardingsphere.database.exception.xugu.vendor.XuguVendorError;
import org.apache.shardingsphere.database.exception.core.exception.SQLDialectException;
import org.apache.shardingsphere.database.exception.core.exception.connection.AccessDeniedException;
import org.apache.shardingsphere.database.exception.core.exception.connection.TooManyConnectionsException;
import org.apache.shardingsphere.database.exception.core.exception.data.InsertColumnsAndValuesMismatchedException;
import org.apache.shardingsphere.database.exception.core.exception.data.InvalidParameterValueException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.column.ColumnNotFoundException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.database.DatabaseCreateExistsException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.database.DatabaseDropNotExistsException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.database.NoDatabaseSelectedException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.database.UnknownDatabaseException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.sql.DialectSQLParsingException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.table.NoSuchTableException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.table.TableExistsException;
import org.apache.shardingsphere.database.exception.core.exception.transaction.InTransactionException;
import org.apache.shardingsphere.database.exception.core.exception.transaction.TableModifyInTransactionException;
import org.apache.shardingsphere.database.exception.core.mapper.SQLDialectExceptionMapper;
import org.apache.shardingsphere.infra.exception.external.sql.vendor.VendorError;
import org.apache.shardingsphere.infra.exception.generic.UnknownSQLException;

import java.sql.SQLException;

/**
 * XuGu dialect exception mapper.
 *
 * <p>Converts ShardingSphere {@link SQLDialectException} types to JDBC {@link SQLException}
 * using {@link XuguVendorError} (XOpen SQLState, vendor code 0).
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
        if (sqlDialectException instanceof NoDatabaseSelectedException) {
            return toSQLException(XuguVendorError.NO_DATABASE_SELECTED);
        }
        if (sqlDialectException instanceof DatabaseCreateExistsException) {
            return toSQLException(XuguVendorError.DATABASE_EXISTS, ((DatabaseCreateExistsException) sqlDialectException).getDatabaseName());
        }
        if (sqlDialectException instanceof DatabaseDropNotExistsException) {
            return toSQLException(XuguVendorError.DATABASE_DROP_NOT_EXISTS, ((DatabaseDropNotExistsException) sqlDialectException).getDatabaseName());
        }
        if (sqlDialectException instanceof TableExistsException) {
            return toSQLException(XuguVendorError.TABLE_EXISTS, ((TableExistsException) sqlDialectException).getTableName());
        }
        if (sqlDialectException instanceof NoSuchTableException) {
            return toSQLException(XuguVendorError.NO_SUCH_TABLE, ((NoSuchTableException) sqlDialectException).getTableName());
        }
        if (sqlDialectException instanceof ColumnNotFoundException) {
            ColumnNotFoundException ex = (ColumnNotFoundException) sqlDialectException;
            return toSQLException(XuguVendorError.COLUMN_NOT_FOUND, ex.getColumnName(), ex.getTableName());
        }
        if (sqlDialectException instanceof DialectSQLParsingException) {
            DialectSQLParsingException ex = (DialectSQLParsingException) sqlDialectException;
            return toSQLException(XuguVendorError.PARSE_ERROR, ex.getMessage(), ex.getSymbol(), ex.getLine());
        }
        if (sqlDialectException instanceof InsertColumnsAndValuesMismatchedException) {
            return toSQLException(XuguVendorError.INSERT_COLUMNS_VALUES_MISMATCH,
                    ((InsertColumnsAndValuesMismatchedException) sqlDialectException).getMismatchedRowNumber());
        }
        if (sqlDialectException instanceof InvalidParameterValueException) {
            InvalidParameterValueException ex = (InvalidParameterValueException) sqlDialectException;
            return toSQLException(XuguVendorError.INVALID_PARAMETER_VALUE, ex.getParameterName(), ex.getParameterValue());
        }
        if (sqlDialectException instanceof DuplicateKeyException) {
            return toSQLException(XuguVendorError.DUPLICATE_KEY, ((DuplicateKeyException) sqlDialectException).getConstraintName());
        }
        if (sqlDialectException instanceof NullNotAllowedException) {
            return toSQLException(XuguVendorError.NULL_NOT_ALLOWED, ((NullNotAllowedException) sqlDialectException).getColumnName());
        }
        if (sqlDialectException instanceof AccessDeniedException) {
            AccessDeniedException ex = (AccessDeniedException) sqlDialectException;
            return toSQLException(XuguVendorError.ACCESS_DENIED, ex.getUsername(), ex.getHostname(), ex.isUsingPassword() ? "YES" : "NO");
        }
        if (sqlDialectException instanceof TooManyConnectionsException) {
            return toSQLException(XuguVendorError.TOO_MANY_CONNECTIONS);
        }
        if (sqlDialectException instanceof ConnectionFailedException) {
            ConnectionFailedException ex = (ConnectionFailedException) sqlDialectException;
            return ex.isCommunicationLinkFailure()
                    ? toSQLException(XuguVendorError.COMMUNICATION_LINK_FAILURE, ex.getDetail())
                    : toSQLException(XuguVendorError.CONNECTION_FAILURE, ex.getDetail());
        }
        if (sqlDialectException instanceof QueryTimeoutException) {
            return toSQLException(XuguVendorError.QUERY_TIMEOUT, ((QueryTimeoutException) sqlDialectException).getTimeoutSeconds());
        }
        if (sqlDialectException instanceof InTransactionException) {
            return toSQLException(XuguVendorError.TRANSACTION_STATE_INVALID);
        }
        if (sqlDialectException instanceof TableModifyInTransactionException) {
            return toSQLException(XuguVendorError.TABLE_MODIFY_IN_TRANSACTION,
                    ((TableModifyInTransactionException) sqlDialectException).getTableName());
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
