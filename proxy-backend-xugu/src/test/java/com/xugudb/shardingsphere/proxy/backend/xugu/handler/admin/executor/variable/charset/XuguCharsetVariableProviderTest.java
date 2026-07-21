package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.variable.charset;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.database.exception.core.exception.data.InvalidParameterValueException;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.variable.charset.CharsetVariableProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XuguCharsetVariableProviderTest {

    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");

    private final CharsetVariableProvider provider = DatabaseTypedSPILoader.getService(CharsetVariableProvider.class, databaseType);

    @Test
    void assertGetDatabaseType() {
        assertThat(new XuguCharsetVariableProvider().getDatabaseType(), is("XuGu"));
    }

    @Test
    void assertGetCharsetVariables() {
        assertThat(provider.getCharsetVariables(), is(Collections.singleton("client_encoding")));
    }

    @Test
    void assertParseDefaultCharset() {
        assertThat(provider.parseCharset(" default "), is(Charset.defaultCharset()));
    }

    @Test
    void assertParseKnownCharset() {
        assertThat(provider.parseCharset("utf8"), is(StandardCharsets.UTF_8));
    }

    @Test
    void assertParseInvalidCharset() {
        InvalidParameterValueException ex = assertThrows(InvalidParameterValueException.class, () -> provider.parseCharset("unknown_charset"));
        assertThat(ex.getParameterName(), is("client_encoding"));
        assertThat(ex.getParameterValue(), is("unknown_charset"));
    }
}
