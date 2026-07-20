package com.xugudb.shardingsphere.database.exception.xugu.mapper;

import com.xugudb.shardingsphere.database.exception.xugu.vendor.XuguVendorError;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.database.exception.core.exception.connection.AccessDeniedException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.database.UnknownDatabaseException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.sql.DialectSQLParsingException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.table.NoSuchTableException;
import org.apache.shardingsphere.database.exception.core.exception.syntax.table.TableExistsException;
import org.apache.shardingsphere.database.exception.core.mapper.SQLDialectExceptionMapper;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

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
    void assertConvertTableExists() {
        SQLException actual = mapper.convert(new TableExistsException("t1"));
        assertThat(actual.getSQLState(), is(XuguVendorError.TABLE_EXISTS.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("t1"));
    }
    
    @Test
    void assertConvertNoSuchTable() {
        SQLException actual = mapper.convert(new NoSuchTableException("t1"));
        assertThat(actual.getSQLState(), is(XuguVendorError.NO_SUCH_TABLE.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("t1"));
    }
    
    @Test
    void assertConvertParseError() {
        SQLException actual = mapper.convert(new DialectSQLParsingException("bad", "x", 1));
        assertThat(actual.getSQLState(), is(XuguVendorError.PARSE_ERROR.getSqlState().getValue()));
    }
    
    @Test
    void assertConvertAccessDenied() {
        SQLException actual = mapper.convert(new AccessDeniedException("u", "h", true));
        assertThat(actual.getSQLState(), is(XuguVendorError.ACCESS_DENIED.getSqlState().getValue()));
        assertThat(actual.getMessage(), containsString("u"));
    }
}
