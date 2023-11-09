package io.github.heykb.sqlhelper.dynamicdatasource;

import io.github.heykb.sqlhelper.config.SqlHelperException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * The type Sql helper dynamic data source proxy.
 */
public class SqlHelperDynamicDataSourceProxy implements DataSource {
    private static final Log log = LogFactory.getLog(SqlHelperDynamicDataSourceProxy.class);
    private SqlHelperDsManager sqlHelperDsManager;

    public SqlHelperDynamicDataSourceProxy(SqlHelperDsManager sqlHelperDsManager) {
        this.sqlHelperDsManager = sqlHelperDsManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        String switchedDsName = SqlHelperDsContextHolder.get();
        log.warn(Thread.currentThread().getName() + "线程使用"+(switchedDsName==null?"主":switchedDsName)+"数据源");
        DataSource dataSource = getDatasource();
        LogicDsMeta logicDsMeta = sqlHelperDsManager.getLogicDsMeta(switchedDsName);
        Connection connection = dataSource.getConnection();
        if(logicDsMeta.getSubspace() != null){
            SupportedConnectionSubspaceChange.changeSubspaceIfSupport(connection, logicDsMeta.getSubspace(),logicDsMeta.getExpectedSubspaceType());
        }
        return connection;
    }


    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        String switchedDsName = SqlHelperDsContextHolder.get();
        DataSource dataSource = getDatasource();
        LogicDsMeta logicDsMeta = sqlHelperDsManager.getLogicDsMeta(switchedDsName);
        Connection connection = dataSource.getConnection(username, password);
        if (logicDsMeta.getSubspace() != null) {
            SupportedConnectionSubspaceChange.changeSubspaceIfSupport(connection, logicDsMeta.getSubspace(), logicDsMeta.getExpectedSubspaceType());
        }
        return connection;
    }

    /**
     * Gets datasource.
     *
     * @return the datasource
     */
    protected DataSource getDatasource() {
        String switchedDsName = SqlHelperDsContextHolder.get();
        DataSource dataSource = sqlHelperDsManager.getByName(switchedDsName);
        if(dataSource == null){
            throw new SqlHelperException("逻辑数据源不存在："+switchedDsName);
        }
        return dataSource;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getDatasource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getDatasource().isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getDatasource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        getDatasource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getDatasource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getDatasource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getDatasource().getParentLogger();
    }

    public SqlHelperDsManager getSqlHelperDsManager() {
        return sqlHelperDsManager;
    }
}
