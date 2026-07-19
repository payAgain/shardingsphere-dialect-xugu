package com.xugudb.shardingsphere.database.connector.xugu.metadata.database;

import com.xugudb.shardingsphere.database.connector.xugu.metadata.database.option.XuguDataTypeOption;
import com.xugudb.shardingsphere.database.connector.xugu.metadata.database.option.XuguSchemaOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.enums.NullsOrderType;
import org.apache.shardingsphere.database.connector.core.metadata.database.enums.QuoteCharacter;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.DialectDatabaseMetaData;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.IdentifierPatternType;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.altertable.DialectAlterTableOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.connection.DialectConnectionOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.pagination.DialectPaginationOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.transaction.DialectTransactionOption;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XuguDatabaseMetaDataTest {
    
    private final DialectDatabaseMetaData dialectDatabaseMetaData = DatabaseTypedSPILoader.getService(
            DialectDatabaseMetaData.class, TypedSPILoader.getService(DatabaseType.class, "XuGu"));
    
    @Test
    void assertGetQuoteCharacter() {
        assertThat(dialectDatabaseMetaData.getQuoteCharacter(), is(QuoteCharacter.BACK_QUOTE));
    }
    
    @Test
    void assertGetIdentifierPatternType() {
        assertThat(dialectDatabaseMetaData.getIdentifierPatternType(), is(IdentifierPatternType.UPPER_CASE));
    }
    
    @Test
    void assertGetDefaultNullsOrderType() {
        assertThat(dialectDatabaseMetaData.getDefaultNullsOrderType(), is(NullsOrderType.HIGH));
    }
    
    @Test
    void assertGetDataTypeOption() {
        assertThat(dialectDatabaseMetaData.getDataTypeOption(), isA(XuguDataTypeOption.class));
    }
    
    @Test
    void assertGetSchemaOption() {
        assertThat(dialectDatabaseMetaData.getSchemaOption(), isA(XuguSchemaOption.class));
        assertThat(dialectDatabaseMetaData.getSchemaOption().getDefaultSchema(), is(Optional.of("SYSDBA")));
    }
    
    @Test
    void assertGetIndexOption() {
        assertTrue(dialectDatabaseMetaData.getIndexOption().isSchemaUniquenessLevel());
    }
    
    @Test
    void assertGetConnectionOption() {
        DialectConnectionOption actualConnectionOption = dialectDatabaseMetaData.getConnectionOption();
        assertTrue(actualConnectionOption.isInstanceConnectionAvailable());
        assertFalse(actualConnectionOption.isSupportThreeTierStorageStructure());
    }
    
    @Test
    void assertGetTransactionOption() {
        DialectTransactionOption actualTransactionOption = dialectDatabaseMetaData.getTransactionOption();
        assertFalse(actualTransactionOption.isSupportGlobalCSN());
        assertFalse(actualTransactionOption.isDDLNeedImplicitCommit());
        assertFalse(actualTransactionOption.isSupportAutoCommitInNestedTransaction());
        assertFalse(actualTransactionOption.isSupportDDLInXATransaction());
        assertTrue(actualTransactionOption.isSupportMetaDataRefreshInTransaction());
        assertThat(actualTransactionOption.getDefaultIsolationLevel(), is(Connection.TRANSACTION_READ_COMMITTED));
        assertFalse(actualTransactionOption.isReturnRollbackStatementWhenCommitFailed());
        assertFalse(actualTransactionOption.isAllowCommitAndRollbackOnlyWhenTransactionFailed());
        assertThat(actualTransactionOption.getXaDriverClassNames(), is(Collections.emptySet()));
        assertFalse(actualTransactionOption.getXaDriverClassNames().stream().anyMatch(name -> name.contains("OracleXA")));
    }
    
    @Test
    void assertGetPaginationOption() {
        DialectPaginationOption actualPaginationOption = dialectDatabaseMetaData.getPaginationOption();
        assertTrue(actualPaginationOption.isContainsRowNumber());
        assertThat(actualPaginationOption.getRowNumberColumnName(), is("ROWNUM"));
        assertFalse(actualPaginationOption.isContainsTop());
    }
    
    @Test
    void assertGetAlterTableOption() {
        Optional<DialectAlterTableOption> actualAlterTableOption = dialectDatabaseMetaData.getAlterTableOption();
        assertTrue(actualAlterTableOption.isPresent());
        assertTrue(actualAlterTableOption.get().isSupportMergeDropColumns());
        assertTrue(actualAlterTableOption.get().isContainsParenthesesOnMergeDropColumns());
    }
}
