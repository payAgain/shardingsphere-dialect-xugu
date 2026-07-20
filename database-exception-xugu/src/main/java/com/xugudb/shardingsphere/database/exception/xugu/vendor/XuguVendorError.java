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
 * exceptions; use XOpen SQLState with vendor code {@code 0} and clear reason text.
 */
@RequiredArgsConstructor
@Getter
public enum XuguVendorError implements VendorError {
    
    UNKNOWN_DATABASE(XOpenSQLState.INVALID_CATALOG_NAME, 0, "Unknown database '%s'"),
    
    NO_DATABASE_SELECTED(XOpenSQLState.INVALID_CATALOG_NAME, 0, "No database selected"),
    
    TABLE_EXISTS(XOpenSQLState.DUPLICATE, 0, "Table '%s' already exists"),
    
    NO_SUCH_TABLE(XOpenSQLState.NOT_FOUND, 0, "Table '%s' doesn't exist"),
    
    PARSE_ERROR(XOpenSQLState.SYNTAX_ERROR, 0, "%s near '%s' at line %d"),
    
    ACCESS_DENIED(XOpenSQLState.INVALID_AUTHORIZATION_SPECIFICATION, 0, "Access denied for user '%s'@'%s' (using password: %s)"),
    
    GENERAL_ERROR(XOpenSQLState.GENERAL_ERROR, 0, "%s");
    
    private final SQLState sqlState;
    
    private final int vendorCode;
    
    private final String reason;
}
