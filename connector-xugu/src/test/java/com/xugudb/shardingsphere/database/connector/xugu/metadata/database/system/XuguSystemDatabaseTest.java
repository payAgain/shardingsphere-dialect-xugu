package com.xugudb.shardingsphere.database.connector.xugu.metadata.database.system;

import org.apache.shardingsphere.database.connector.core.metadata.database.system.DialectSystemDatabase;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class XuguSystemDatabaseTest {
    
    private final DialectSystemDatabase systemDatabase = DatabaseTypedSPILoader.getService(
            DialectSystemDatabase.class, TypedSPILoader.getService(DatabaseType.class, "XuGu"));
    
    @Test
    void assertGetSystemDatabases() {
        assertThat(systemDatabase.getSystemDatabases(), is(new LinkedHashSet<>(Arrays.asList("SYSTEM", "sysdba", "shardingsphere"))));
    }
    
    @ParameterizedTest(name = "{0}")
    @MethodSource("getSystemSchemasByDatabaseNameArguments")
    void assertGetSystemSchemasByDatabaseName(final String name, final String databaseName, final Collection<String> expected) {
        assertThat(systemDatabase.getSystemSchemas(databaseName), is(expected));
    }
    
    private static Stream<Arguments> getSystemSchemasByDatabaseNameArguments() {
        return Stream.of(
                Arguments.of("SYSTEM database", "SYSTEM", Collections.singleton("SYSTEM")),
                Arguments.of("sysdba schema", "sysdba", Collections.singleton("sysdba")),
                Arguments.of("unknown database", "unknown_database", Collections.emptyList()));
    }
    
    @Test
    void assertGetSystemSchemas() {
        assertThat(systemDatabase.getSystemSchemas(), is(new LinkedHashSet<>(Arrays.asList("SYSTEM", "sysdba", "shardingsphere"))));
    }
}
