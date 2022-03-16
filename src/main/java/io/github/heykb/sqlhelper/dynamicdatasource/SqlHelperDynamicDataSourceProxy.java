package io.github.heykb.sqlhelper.dynamicdatasource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * The type Sql helper dynamic data source proxy.
 */
public class SqlHelperDynamicDataSourceProxy extends SimpleProxyDatasource {
    private static final Log log = LogFactory.getLog(SqlHelperDynamicDataSourceProxy.class);
    private SqlHelperDsManager sqlHelperDsManager;

    public SqlHelperDynamicDataSourceProxy(DataSource primaryDs) {
       this(primaryDs,null);
    }

    /**
     * Instantiates a new Sql helper dynamic data source proxy.
     *
     * @param primaryDs         the primary ds
     * @param dsUpgradeCallback the ds upgrade callback
     */
    public SqlHelperDynamicDataSourceProxy(DataSource primaryDs, Function<DataSource, DataSource> dsUpgradeCallback) {
        super(primaryDs);
        this.sqlHelperDsManager = new SqlHelperDsManager(primaryDs, dsUpgradeCallback);
    }

    public SqlHelperDsManager getSqlHelperDsManager() {
        return sqlHelperDsManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        String switchedDsName = SqlHelperDsContextHolder.get();
        if (switchedDsName == null) {
            log.warn(Thread.currentThread().getName() + "线程使用主数据源");
            return sqlHelperDsManager.getPrimaryDs().getConnection();
        }
        log.warn(Thread.currentThread().getName() + "线程使用"+switchedDsName+"数据源");
        LogicDsMeta logicDsMeta = sqlHelperDsManager.getByLogicName(switchedDsName);
        DataSource dataSource = sqlHelperDsManager.getByDatasourceId(logicDsMeta.getDatasourceId());
        Connection connection = dataSource.getConnection();
        if (logicDsMeta.getSubspace() != null) {
            SupportedConnectionSubspaceChange.changeNamespaceIfSupport(connection, logicDsMeta.getSubspace(),logicDsMeta.getExpectedSubspaceType());
        }
        return connection;
    }


    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        String switchedDsName = SqlHelperDsContextHolder.get();
        if (switchedDsName == null) {
            return sqlHelperDsManager.getPrimaryDs().getConnection(username, password);
        }
        LogicDsMeta logicDsMeta = sqlHelperDsManager.getByLogicName(switchedDsName);
        DataSource dataSource = sqlHelperDsManager.getByDatasourceId(logicDsMeta.getDatasourceId());
        Connection connection = dataSource.getConnection(username, password);
        if (logicDsMeta.getSubspace() != null) {
            SupportedConnectionSubspaceChange.changeNamespaceIfSupport(connection, logicDsMeta.getSubspace(), logicDsMeta.getExpectedSubspaceType());
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
        if (switchedDsName == null) {
            return sqlHelperDsManager.getPrimaryDs();
        }
        LogicDsMeta logicDsMeta = sqlHelperDsManager.getByLogicName(switchedDsName);
        return sqlHelperDsManager.getByDatasourceId(logicDsMeta.getDatasourceId());
    }

}
