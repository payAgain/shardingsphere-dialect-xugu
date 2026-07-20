package com.xugudb.shardingsphere.database.exception.xugu.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.database.exception.core.exception.SQLDialectException;

/**
 * Statement / query timeout.
 */
@RequiredArgsConstructor
@Getter
public final class QueryTimeoutException extends SQLDialectException {
    
    private static final long serialVersionUID = 3021145788903344122L;
    
    private final int timeoutSeconds;
}
