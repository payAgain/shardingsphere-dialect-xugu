package com.xugudb.shardingsphere.database.exception.xugu.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.database.exception.core.exception.SQLDialectException;

/**
 * Connection establishment or I/O failure.
 */
@RequiredArgsConstructor
@Getter
public final class ConnectionFailedException extends SQLDialectException {
    
    private static final long serialVersionUID = 8844221103345566771L;
    
    private final String detail;
    
    private final boolean communicationLinkFailure;
}
