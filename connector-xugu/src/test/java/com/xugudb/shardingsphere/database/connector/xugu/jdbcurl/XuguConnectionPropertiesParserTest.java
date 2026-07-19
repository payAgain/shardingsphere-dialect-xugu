package com.xugudb.shardingsphere.database.connector.xugu.jdbcurl;

import org.apache.shardingsphere.database.connector.core.jdbcurl.parser.ConnectionProperties;
import org.apache.shardingsphere.database.connector.core.jdbcurl.parser.ConnectionPropertiesParser;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class XuguConnectionPropertiesParserTest {

    private final ConnectionPropertiesParser parser = DatabaseTypedSPILoader.getService(
            ConnectionPropertiesParser.class, TypedSPILoader.getService(DatabaseType.class, "XuGu"));

    @Test
    void assertParseDefaultPortAndSchemaFromUser() {
        ConnectionProperties actual = parser.parse(
                "jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8", "SYSDBA", null);
        assertThat(actual.getHostname(), is("192.168.2.239"));
        assertThat(actual.getPort(), is(5138));
        assertThat(actual.getCatalog(), is("SYSTEM"));
        assertThat(actual.getSchema(), is("SYSDBA"));
        assertThat(actual.getQueryProperties().getProperty("compatiblemode"), is("NONE"));
    }

    @Test
    void assertParseCurrentSchemaQueryParam() {
        ConnectionProperties actual = parser.parse(
                "jdbc:xugu://127.0.0.1:5138/SYSTEM?current_schema=APP", "SYSDBA", null);
        assertThat(actual.getSchema(), is("APP"));
    }
}
