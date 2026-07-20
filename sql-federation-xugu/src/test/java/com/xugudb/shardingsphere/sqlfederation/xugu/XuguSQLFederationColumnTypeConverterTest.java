package com.xugudb.shardingsphere.sqlfederation.xugu;

import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sqlfederation.resultset.converter.DialectSQLFederationColumnTypeConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNull;

class XuguSQLFederationColumnTypeConverterTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectSQLFederationColumnTypeConverter converter =
            DatabaseTypedSPILoader.getService(DialectSQLFederationColumnTypeConverter.class, databaseType);
    
    @Test
    void assertSPILoads() {
        assertThat(converter, isA(XuguSQLFederationColumnTypeConverter.class));
        assertThat(converter.getDatabaseType(), is("XuGu"));
    }
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("convertValueSource")
    void assertConvertColumnValue(final String name, final Object input, final Object expected) {
        if (null == expected) {
            assertNull(converter.convertColumnValue(input));
            return;
        }
        assertThat(converter.convertColumnValue(input), is(expected));
    }
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("convertTypeSource")
    void assertConvertColumnType(final String name, final SqlTypeName sqlTypeName, final int expected) {
        assertThat(converter.convertColumnType(sqlTypeName), is(expected));
    }
    
    private static Iterable<Arguments> convertValueSource() {
        return Arrays.asList(
                Arguments.arguments("booleanTrueConvertedToOne", Boolean.TRUE, 1),
                Arguments.arguments("booleanFalseConvertedToZero", Boolean.FALSE, 0),
                Arguments.arguments("nonBooleanValueUntouched", "text", "text"),
                Arguments.arguments("nullRemainsNull", null, null));
    }
    
    private static Iterable<Arguments> convertTypeSource() {
        return Arrays.asList(
                Arguments.arguments("booleanMapsToVarchar", SqlTypeName.BOOLEAN, SqlTypeName.VARCHAR.getJdbcOrdinal()),
                Arguments.arguments("anyMapsToVarchar", SqlTypeName.ANY, SqlTypeName.VARCHAR.getJdbcOrdinal()),
                Arguments.arguments("otherTypesUnchanged", SqlTypeName.INTEGER, SqlTypeName.INTEGER.getJdbcOrdinal()));
    }
}
