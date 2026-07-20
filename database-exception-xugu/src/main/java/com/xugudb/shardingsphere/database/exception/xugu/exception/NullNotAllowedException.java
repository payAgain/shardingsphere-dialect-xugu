package com.xugudb.shardingsphere.database.exception.xugu.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.database.exception.core.exception.SQLDialectException;

/**
 * NOT NULL constraint violation.
 */
@RequiredArgsConstructor
@Getter
public final class NullNotAllowedException extends SQLDialectException {
    
    private static final long serialVersionUID = 7192844551023348890L;
    
    private final String columnName;
}
