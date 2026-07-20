package com.xugudb.shardingsphere.database.exception.xugu.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.database.exception.core.exception.SQLDialectException;

/**
 * Duplicate key / unique constraint violation.
 */
@RequiredArgsConstructor
@Getter
public final class DuplicateKeyException extends SQLDialectException {
    
    private static final long serialVersionUID = 4819203344556671201L;
    
    private final String constraintName;
}
