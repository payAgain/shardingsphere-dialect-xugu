package com.xugudb.shardingsphere.transaction.xugu.xa;

import lombok.SneakyThrows;
import org.apache.shardingsphere.transaction.xa.jta.connection.XAConnectionWrapper;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * XA connection wrapper for XuGu.
 *
 * <p>Wraps a physical {@code com.xugu.cloudjdbc.Connection} into {@code com.xugu.xa.XAConnectionImp}
 * via its protected {@code (Connection)} constructor.
 */
public final class XuguXAConnectionWrapper implements XAConnectionWrapper {
    
    private Class<Connection> jdbcConnectionClass;
    
    private Constructor<?> xaConnectionConstructor;
    
    @Override
    public XAConnection wrap(final XADataSource xaDataSource, final Connection connection) throws SQLException {
        return createXAConnection(connection.unwrap(jdbcConnectionClass));
    }
    
    @Override
    public void init(final Properties props) {
        loadReflection();
    }
    
    private void loadReflection() {
        jdbcConnectionClass = getJDBCConnectionClass();
        xaConnectionConstructor = getXAConnectionConstructor();
    }
    
    @SuppressWarnings("unchecked")
    @SneakyThrows(ReflectiveOperationException.class)
    private Class<Connection> getJDBCConnectionClass() {
        return (Class<Connection>) Class.forName("com.xugu.cloudjdbc.Connection");
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    private Constructor<?> getXAConnectionConstructor() {
        Constructor<?> result = Class.forName("com.xugu.xa.XAConnectionImp").getDeclaredConstructor(Connection.class);
        result.setAccessible(true);
        return result;
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    private XAConnection createXAConnection(final Connection connection) {
        return (XAConnection) xaConnectionConstructor.newInstance(connection);
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
