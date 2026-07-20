package com.xugudb.shardingsphere.sqlfederation.xugu;

import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.fun.SqlLibrary;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.shardingsphere.sqlfederation.compiler.context.connection.config.DialectSQLFederationConnectionConfigBuilder;

import java.util.Properties;

/**
 * SQL federation connection config builder for XuGu.
 *
 * <p>NONE compatible mode is Oracle-like; reuse Calcite Lex.ORACLE / ORACLE_12 / SqlLibrary.ORACLE.
 */
public final class XuguSQLFederationConnectionConfigBuilder implements DialectSQLFederationConnectionConfigBuilder {
    
    @Override
    public CalciteConnectionConfig build() {
        Properties result = new Properties();
        result.setProperty(CalciteConnectionProperty.LEX.camelName(), Lex.ORACLE.name());
        result.setProperty(CalciteConnectionProperty.CONFORMANCE.camelName(), SqlConformanceEnum.ORACLE_12.name());
        result.setProperty(CalciteConnectionProperty.FUN.camelName(), SqlLibrary.ORACLE.fun);
        result.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), String.valueOf(Lex.ORACLE.caseSensitive));
        return new CalciteConnectionConfigImpl(result);
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
