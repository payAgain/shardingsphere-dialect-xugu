package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.variable.charset;

import org.apache.shardingsphere.database.exception.core.exception.data.InvalidParameterValueException;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.variable.charset.CharsetVariableProvider;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Charset variable provider of XuGu.
 */
public final class XuguCharsetVariableProvider implements CharsetVariableProvider {

    private static final String CLIENT_ENCODING = "client_encoding";

    @Override
    public Collection<String> getCharsetVariables() {
        return Collections.singleton(CLIENT_ENCODING);
    }

    @Override
    public Charset parseCharset(final String variableValue) {
        String formattedValue = variableValue.trim().toLowerCase(Locale.ROOT);
        if ("default".equals(formattedValue)) {
            return Charset.defaultCharset();
        }
        if ("utf8".equals(formattedValue) || "utf-8".equals(formattedValue)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(formattedValue);
        } catch (final IllegalArgumentException ignored) {
            throw new InvalidParameterValueException(CLIENT_ENCODING, formattedValue);
        }
    }

    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
