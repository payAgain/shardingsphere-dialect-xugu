package com.xugudb.shardingsphere.database.connector.xugu.metadata.database.option;

import com.cedarsoftware.util.CaseInsensitiveMap;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.datatype.DefaultDataTypeOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.datatype.DialectDataTypeOption;

import java.sql.Types;
import java.util.Map;
import java.util.Optional;

/**
 * Data type option for XuGu.
 */
public final class XuguDataTypeOption implements DialectDataTypeOption {
    
    private static final Map<String, Integer> EXTRA_DATA_TYPES;
    
    private final DialectDataTypeOption delegate = new DefaultDataTypeOption();
    
    static {
        EXTRA_DATA_TYPES = setUpExtraDataTypes();
    }
    
    private static Map<String, Integer> setUpExtraDataTypes() {
        Map<String, Integer> result = new CaseInsensitiveMap<>();
        result.put("TINYINT", Types.TINYINT);
        result.put("SMALLINT", Types.SMALLINT);
        result.put("SHORT", Types.SMALLINT);
        result.put("INT", Types.INTEGER);
        result.put("BINARY_INTEGER", Types.INTEGER);
        result.put("LONGINT", Types.BIGINT);
        result.put("TEXT", Types.LONGVARCHAR);
        result.put("CHARACTER", Types.CHAR);
        result.put("NCHAR", Types.CHAR);
        result.put("VARCHAR2", Types.VARCHAR);
        result.put("DATETIME", Types.TIMESTAMP);
        result.put("YEAR", Types.DATE);
        result.put("DOUBLE", Types.DOUBLE);
        result.put("FLOAT", Types.FLOAT);
        result.put("NUMBER", Types.NUMERIC);
        return result;
    }
    
    @Override
    public Map<String, Integer> getExtraDataTypes() {
        return EXTRA_DATA_TYPES;
    }
    
    @Override
    public Optional<Class<?>> findExtraSQLTypeClass(final int dataType, final boolean unsigned) {
        return Optional.empty();
    }
    
    @Override
    public boolean isIntegerDataType(final int sqlType) {
        return delegate.isIntegerDataType(sqlType);
    }
    
    @Override
    public boolean isStringDataType(final int sqlType) {
        return delegate.isStringDataType(sqlType);
    }
    
    @Override
    public boolean isBinaryDataType(final int sqlType) {
        return delegate.isBinaryDataType(sqlType);
    }
}
