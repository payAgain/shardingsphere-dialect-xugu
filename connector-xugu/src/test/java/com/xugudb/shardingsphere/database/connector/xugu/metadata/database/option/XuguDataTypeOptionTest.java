package com.xugudb.shardingsphere.database.connector.xugu.metadata.database.option;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Types;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G-T2 — XuGu {@link XuguDataTypeOption} contract (no lab).
 */
class XuguDataTypeOptionTest {
    
    private final XuguDataTypeOption option = new XuguDataTypeOption();
    
    @ParameterizedTest(name = "{0} -> JDBC {1}")
    @CsvSource({
            "TINYINT, -6",
            "SMALLINT, 5",
            "SHORT, 5",
            "INT, 4",
            "BINARY_INTEGER, 4",
            "LONGINT, -5",
            "TEXT, -1",
            "CHARACTER, 1",
            "NCHAR, 1",
            "VARCHAR2, 12",
            "DATETIME, 93",
            "YEAR, 91",
            "DOUBLE, 8",
            "FLOAT, 6",
            "NUMBER, 2"
    })
    void assertExtraDataTypeMapping(final String typeName, final int expectedJdbcType) {
        Map<String, Integer> extras = option.getExtraDataTypes();
        assertThat(extras.get(typeName), is(expectedJdbcType));
        assertThat(extras.get(typeName.toLowerCase()), is(expectedJdbcType));
    }
    
    @Test
    void assertExtraDataTypesSizeAndKnownAliases() {
        Map<String, Integer> extras = option.getExtraDataTypes();
        assertThat(extras.size(), is(15));
        assertThat(extras.get("short"), is(Types.SMALLINT));
        assertThat(extras.get("varchar2"), is(Types.VARCHAR));
        assertThat(extras.get("datetime"), is(Types.TIMESTAMP));
        assertThat(extras.get("number"), is(Types.NUMERIC));
    }
    
    @Test
    void assertFindExtraSQLTypeClassAlwaysEmpty() {
        assertThat(option.findExtraSQLTypeClass(Types.INTEGER, false), is(Optional.empty()));
        assertThat(option.findExtraSQLTypeClass(Types.BIGINT, true), is(Optional.empty()));
    }
    
    @Test
    void assertIntegerStringBinaryDelegation() {
        assertTrue(option.isIntegerDataType(Types.INTEGER));
        assertTrue(option.isIntegerDataType(Types.BIGINT));
        assertFalse(option.isIntegerDataType(Types.VARCHAR));
        assertTrue(option.isStringDataType(Types.VARCHAR));
        assertTrue(option.isStringDataType(Types.CHAR));
        assertFalse(option.isStringDataType(Types.INTEGER));
        assertTrue(option.isBinaryDataType(Types.BINARY));
        assertTrue(option.isBinaryDataType(Types.VARBINARY));
        assertFalse(option.isBinaryDataType(Types.INTEGER));
    }
}
