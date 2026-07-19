package com.xugudb.shardingsphere.database.connector.xugu.metadata.database;

import com.xugudb.shardingsphere.database.connector.xugu.metadata.database.option.XuguDataTypeOption;
import com.xugudb.shardingsphere.database.connector.xugu.metadata.database.option.XuguSchemaOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.enums.NullsOrderType;
import org.apache.shardingsphere.database.connector.core.metadata.database.enums.QuoteCharacter;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.DialectDatabaseMetaData;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.IdentifierPatternType;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.altertable.DialectAlterTableOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.connection.DialectConnectionOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.datatype.DialectDataTypeOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.index.DialectIndexOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.pagination.DialectPaginationOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.schema.DialectSchemaOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.transaction.DialectTransactionOption;

import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;

/**
 * Database meta data of XuGu.
 */
public final class XuguDatabaseMetaData implements DialectDatabaseMetaData {
    
    @Override
    public QuoteCharacter getQuoteCharacter() {
        return QuoteCharacter.BACK_QUOTE;
    }
    
    @Override
    public IdentifierPatternType getIdentifierPatternType() {
        return IdentifierPatternType.UPPER_CASE;
    }
    
    @Override
    public NullsOrderType getDefaultNullsOrderType() {
        return NullsOrderType.HIGH;
    }
    
    @Override
    public DialectDataTypeOption getDataTypeOption() {
        return new XuguDataTypeOption();
    }
    
    @Override
    public DialectSchemaOption getSchemaOption() {
        return new XuguSchemaOption();
    }
    
    @Override
    public DialectIndexOption getIndexOption() {
        return new DialectIndexOption(true);
    }
    
    @Override
    public DialectConnectionOption getConnectionOption() {
        return new DialectConnectionOption(true, false);
    }
    
    @Override
    public DialectTransactionOption getTransactionOption() {
        return new DialectTransactionOption(false, false, false, false, true,
                Connection.TRANSACTION_READ_COMMITTED, false, false, Collections.emptySet());
    }
    
    @Override
    public DialectPaginationOption getPaginationOption() {
        return new DialectPaginationOption(true, "ROWNUM", false);
    }
    
    @Override
    public Optional<DialectAlterTableOption> getAlterTableOption() {
        return Optional.of(new DialectAlterTableOption(true, true));
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
