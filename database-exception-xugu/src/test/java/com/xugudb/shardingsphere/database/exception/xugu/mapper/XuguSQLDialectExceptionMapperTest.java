package com.xugudb.shardingsphere.database.exception.xugu.mapper;

import com.xugudb.shardingsphere.database.exception.xugu.exception.ConnectionFailedException;
import com.xugudb.shardingsphere.database.exception.xugu.exception.DuplicateKeyException;
import com.xugudb.shardingsphere.database.exception.xugu.exception.NullNotAllowedException;
import com.xugudb.shardingsphere.database.exception.xugu.exception.QueryTimeoutException;
import com.xugudb.shardingsphere.database.exception.xugu.vendor.XuguVendorError;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
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
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.sql.SQLException;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

class XuguSQLDialectExceptionMapperTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final SQLDialectExceptionMapper mapper =
            DatabaseTypedSPILoader.getService(SQLDialectExceptionMapper.class, databaseType);
    
    @Test
    void assertSPILoads() {
        assertThat(mapper, isA(XuguSQLDialectExceptionMapper.class));
        assertThat(mapper.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertConvertUnknownDatabaseWithoutName() {
        SQLException actual = mapper.convert(new UnknownDatabaseException(null));
        assertThat(actual.getSQLState(), is(XuguVendorError.NO_DATABASE_SELECTED.getSqlState().getValue()));
        assertThat(actual.getErrorCode(), is(0));
    }
    
    @Test
    void assertConvertUnknownDatabaseWithName() {
        SQLException actual = mapper.convert(new UnknownDatabaseException("demo"));
        assertThat(actual.getSQLState(), is(XuguVendorError.UNKNOWN_DATABASE.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("demo"));
    }
    
    @Test
    void assertConvertDuplicateKey() {
        SQLException actual = mapper.convert(new DuplicateKeyException("pk_order"));
        assertThat(actual.getSQLState(), is(XuguVendorError.DUPLICATE_KEY.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("pk_order"));
        assertThat(actual.getErrorCode(), is(0));
    }
    
    @Test
    void assertConvertNullNotAllowed() {
        SQLException actual = mapper.convert(new NullNotAllowedException("user_id"));
        assertThat(actual.getSQLState(), is(XuguVendorError.NULL_NOT_ALLOWED.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("user_id"));
    }
    
    @Test
    void assertConvertQueryTimeout() {
        SQLException actual = mapper.convert(new QueryTimeoutException(30));
        assertThat(actual.getSQLState(), is(XuguVendorError.QUERY_TIMEOUT.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("30"));
    }
    
    @Test
    void assertConvertConnectionFailure() {
        SQLException actual = mapper.convert(new ConnectionFailedException("refused", false));
        assertThat(actual.getSQLState(), is(XuguVendorError.CONNECTION_FAILURE.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("refused"));
    }
    
    @Test
    void assertConvertCommunicationLinkFailure() {
        SQLException actual = mapper.convert(new ConnectionFailedException("broken pipe", true));
        assertThat(actual.getSQLState(), is(XuguVendorError.COMMUNICATION_LINK_FAILURE.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("broken pipe"));
    }
    
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ExceptionArgumentsProvider.class)
    void assertConvertCommonExceptions(final SQLDialectException exception, final VendorError vendorError) {
        SQLException actual = mapper.convert(exception);
        assertThat(actual.getSQLState(), is(vendorError.getSqlState().getValue()));
        assertThat(actual.getErrorCode(), is(vendorError.getVendorCode()));
    }
    
    private static final class ExceptionArgumentsProvider implements ArgumentsProvider {
        
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new NoDatabaseSelectedException(), XuguVendorError.NO_DATABASE_SELECTED),
                    Arguments.of(new DatabaseCreateExistsException("db"), XuguVendorError.DATABASE_EXISTS),
                    Arguments.of(new DatabaseDropNotExistsException("db"), XuguVendorError.DATABASE_DROP_NOT_EXISTS),
                    Arguments.of(new TableExistsException("t"), XuguVendorError.TABLE_EXISTS),
                    Arguments.of(new NoSuchTableException("t"), XuguVendorError.NO_SUCH_TABLE),
                    Arguments.of(new ColumnNotFoundException("t", "c"), XuguVendorError.COLUMN_NOT_FOUND),
                    Arguments.of(new DialectSQLParsingException("bad", "x", 1), XuguVendorError.PARSE_ERROR),
                    Arguments.of(new InsertColumnsAndValuesMismatchedException(2), XuguVendorError.INSERT_COLUMNS_VALUES_MISMATCH),
                    Arguments.of(new InvalidParameterValueException("p", "v"), XuguVendorError.INVALID_PARAMETER_VALUE),
                    Arguments.of(new AccessDeniedException("u", "h", true), XuguVendorError.ACCESS_DENIED),
                    Arguments.of(new TooManyConnectionsException(), XuguVendorError.TOO_MANY_CONNECTIONS),
                    Arguments.of(new InTransactionException(), XuguVendorError.TRANSACTION_STATE_INVALID),
                    Arguments.of(new TableModifyInTransactionException("t"), XuguVendorError.TABLE_MODIFY_IN_TRANSACTION));
        }
    }
}
