package com.xugudb.shardingsphere.database.exception.xugu.vendor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.exception.external.sql.sqlstate.SQLState;
import org.apache.shardingsphere.infra.exception.external.sql.sqlstate.XOpenSQLState;
import org.apache.shardingsphere.infra.exception.external.sql.vendor.VendorError;

/**
 * XuGu vendor error.
 *
 * <p>XuGu does not publish a stable public vendor-code catalog aligned with ShardingSphere dialect
 * exceptions. Mirror the PostgreSQL dialect pattern: XOpen SQLState + vendor code {@code 0} and
 * clear reason text. Driver-local {@code E5xxxx}/{@code E51xxx} codes are JDBC-client diagnostics
 * and are documented separately in {@code docs/error-code-map.md}.
 */
@RequiredArgsConstructor
@Getter
public enum XuguVendorError implements VendorError {
    
    UNKNOWN_DATABASE(XOpenSQLState.INVALID_CATALOG_NAME, 0, "Unknown database '%s'"),
    
    NO_DATABASE_SELECTED(XOpenSQLState.INVALID_CATALOG_NAME, 0, "No database selected"),
    
    DATABASE_EXISTS(XOpenSQLState.GENERAL_ERROR, 0, "Can't create database '%s'; database exists"),
    
    DATABASE_DROP_NOT_EXISTS(XOpenSQLState.GENERAL_ERROR, 0, "Can't drop database '%s'; database doesn't exist"),
    
    TABLE_EXISTS(XOpenSQLState.DUPLICATE, 0, "Table '%s' already exists"),
    
    NO_SUCH_TABLE(XOpenSQLState.NOT_FOUND, 0, "Table '%s' doesn't exist"),
    
    COLUMN_NOT_FOUND(XOpenSQLState.NOT_FOUND, 0, "Unknown column '%s' in table '%s'"),
    
    PARSE_ERROR(XOpenSQLState.SYNTAX_ERROR, 0, "%s near '%s' at line %d"),
    
    INSERT_COLUMNS_VALUES_MISMATCH(XOpenSQLState.MISMATCH_INSERT_VALUES_AND_COLUMNS, 0,
            "Column count doesn't match value count at row %d"),
    
    INVALID_PARAMETER_VALUE(XOpenSQLState.INVALID_PARAMETER_VALUE, 0, "Invalid value for parameter '%s': '%s'"),
    
    DUPLICATE_KEY(XOpenSQLState.INTEGRITY_CONSTRAINT_VIOLATION, 0, "Duplicate key value violates unique constraint '%s'"),
    
    NULL_NOT_ALLOWED(XOpenSQLState.INTEGRITY_CONSTRAINT_VIOLATION, 0, "NULL not allowed for column '%s'"),
    
    ACCESS_DENIED(XOpenSQLState.INVALID_AUTHORIZATION_SPECIFICATION, 0, "Access denied for user '%s'@'%s' (using password: %s)"),
    
    TOO_MANY_CONNECTIONS(XOpenSQLState.DATA_SOURCE_REJECTED_CONNECTION_ATTEMPT, 0, "Too many connections"),
    
    CONNECTION_FAILURE(XOpenSQLState.CONNECTION_EXCEPTION, 0, "Connection failure: %s"),
    
    COMMUNICATION_LINK_FAILURE(XOpenSQLState.COMMUNICATION_LINK_FAILURE, 0, "Communication link failure: %s"),
    
    QUERY_TIMEOUT(XOpenSQLState.GENERAL_ERROR, 0, "Query timed out after %d second(s)"),
    
    TRANSACTION_STATE_INVALID(XOpenSQLState.INVALID_TRANSACTION_STATE, 0, "There is already a transaction in progress"),
    
    TABLE_MODIFY_IN_TRANSACTION(XOpenSQLState.INVALID_TRANSACTION_STATE, 0,
            "Table '%s' cannot be modified in the current transaction state"),
    
    GENERAL_ERROR(XOpenSQLState.GENERAL_ERROR, 0, "%s");
    
    private final SQLState sqlState;
    
    private final int vendorCode;
    
    private final String reason;
}
