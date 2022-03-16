package io.github.heykb.sqlhelper.dynamicdatasource;

import io.github.heykb.sqlhelper.config.SqlHelperException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The type Sqlhelper ds manager.
 */
public class SqlHelperDsManager {
    public static final String PRIMARY_DATASOURCE_ID = "SQLHELP_PRIMARY_DS";
    private static final Log log = LogFactory.getLog(SqlHelperDsManager.class);
    private final Map<String, LogicDsMeta> logicDatasourceCache = new ConcurrentHashMap<>();

    private final Map<String, DataSource> physicsDatasourceCache = new ConcurrentHashMap<>();

    /**
     * 当新增加的数据源存在共用情况时，可通过该方法注入升级数据源的逻辑，如可以升级数据源的连接池大小
     */
    private Function<DataSource, DataSource> dsUpgradeCallback;

    private PrimaryDatasource primaryDs;

    /**
     * Instantiates a new Sqlhelper ds manager.
     *
     * @param primaryDs         the primary ds
     * @param dsUpgradeCallback the ds upgrade callback
     */
    public SqlHelperDsManager(DataSource primaryDs, Function<DataSource, DataSource> dsUpgradeCallback) {
        physicsDatasourceCache.put(PRIMARY_DATASOURCE_ID,primaryDs);
        this.primaryDs = new PrimaryDatasource(primaryDs);
        this.dsUpgradeCallback = dsUpgradeCallback;
    }

    /**
     * Instantiates a new Sqlhelper ds manager.
     *
     * @param primaryDs the primary ds
     */
    public SqlHelperDsManager(DataSource primaryDs) {
        this(primaryDs, null);
    }


    /**
     * Gets primary ds.
     *
     * @return the primary ds
     */
    public PrimaryDatasource getPrimaryDs() {
        return primaryDs;
    }

    /**
     * Put.
     *
     * @param logicName the logic name
     * @param dsMeta    the ds meta
     */
    public void put(String logicName, LogicDsMeta dsMeta) {
        log.warn("添加逻辑数据源" + logicName);
        if (logicDatasourceCache.containsKey(logicName)) {
            throw new SqlHelperException("数据源名称已存在");
        }
        DataSource newDatasource = null;
        if (physicsDatasourceCache.get(dsMeta.getDatasourceId()) == null) {
            if (dsMeta.getCreateFunc() == null) {
                throw new SqlHelperException("缺少createFunc数据源创建方法，无法初始化" + dsMeta.getDatasourceId() + "数据源");
            }
            log.warn("为逻辑数据源 " + logicName + " 初始化新的数据源 " + dsMeta.getDatasourceId());
            newDatasource = dsMeta.getCreateFunc().apply(null);
            if(dsMeta.getSubspace()==null){
                try(Connection connection = newDatasource.getConnection()){
                    log.warn("逻辑数据源"+logicName+"未设置subspace尝试从连接中获取");
                    dsMeta.setSubspace(SupportedConnectionSubspaceChange.getCurrentSubspaceIfSupport(connection,dsMeta.getExpectedSubspaceType()));
                    log.warn("从连接中获取成功，设置subspace为"+dsMeta.getSubspace());
                } catch (SQLException e) {
                    throw new SqlHelperException(e);
                }
            }
        } else {
            log.warn("逻辑数据源" + logicName + "复用已存在的数据源ID:" + dsMeta.getDatasourceId());
            if(dsMeta.getSubspace()==null){
                throw new SqlHelperException("复用已有数据源,subspace不能为null");
            }
            newDatasource = upgradeDatasourceByDatasourceId(dsMeta.getDatasourceId());
        }
        physicsDatasourceCache.put(dsMeta.getDatasourceId(), newDatasource);
        // 保存逻辑数据源信息
        logicDatasourceCache.put(logicName, dsMeta);
    }

    /**
     * Gets by datasource id.
     *
     * @param datasourceId the datasource id
     * @return the by datasource id
     */
    public DataSource getByDatasourceId(String datasourceId) {
        DataSource dataSource = physicsDatasourceCache.get(datasourceId);
        if (dataSource == null) {
            throw new SqlHelperException("datasourceId为" + datasourceId + "的数据源不存在");
        }
        return dataSource;
    }

    /**
     * Gets by logic name.
     *
     * @param logicName the logic name
     * @return the by logic name
     */
    public LogicDsMeta getByLogicName(String logicName) {
        LogicDsMeta re = logicDatasourceCache.get(logicName);
        if (re == null) {
            throw new SqlHelperException("逻辑数据源" + logicName + "不存在");
        }
        return re;
    }

    /**
     * Upgrade datasource by datasource id data source.
     *
     * @param datasourceId the datasource id
     * @return the data source
     */
    DataSource upgradeDatasourceByDatasourceId(String datasourceId) {
        DataSource dataSource = getByDatasourceId(datasourceId);
        if (dsUpgradeCallback != null) {
            log.warn("升级数据源 " + datasourceId);
            return physicsDatasourceCache.put(datasourceId, dsUpgradeCallback.apply(dataSource));
        }
        return dataSource;
    }

}
