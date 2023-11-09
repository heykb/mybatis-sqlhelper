package io.github.heykb.sqlhelper.dynamicdatasource;

import io.github.heykb.sqlhelper.config.SqlHelperException;
import io.github.heykb.sqlhelper.utils.Asserts;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * The type Sqlhelper ds manager.
 */
public class DefaultSqlHelperDsManager implements SqlHelperDsManager {
    public static final String PRIMARY_DATASOURCE_ID = "SQLHELP_PRIMARY_DS";
    private static final Log log = LogFactory.getLog(DefaultSqlHelperDsManager.class);
    private final Map<String, LogicDsMeta> logicDsName2DsMeta = new HashMap<>();
    private final Map<String, DataSource> dsId2Datasource = new HashMap<>();
    private final Map<String, List<String>> dsId2LogicDsName = new HashMap<>();
    protected ReentrantLock lock = new ReentrantLock(true);
    /**
     * 当新增加的数据源存在共用情况时，可通过该方法注入升级数据源的逻辑，如可以升级数据源的连接池大小
     */
    private Function<DataSource, DataSource> dsUpgradeCallback;

    /**
     * Instantiates a new Sqlhelper ds manager.
     *
     * @param primaryDs         the primary ds
     * @param dsUpgradeCallback the ds upgrade callback
     */
    public DefaultSqlHelperDsManager(DataSource primaryDs, Function<DataSource, DataSource> dsUpgradeCallback) {
        put(null,LogicDsMeta.builder().datasourceId(PRIMARY_DATASOURCE_ID).createFunc(()->primaryDs).build());
        this.dsUpgradeCallback = dsUpgradeCallback;
    }

    /**
     * Instantiates a new Sqlhelper ds manager.
     *
     * @param primaryDs the primary ds
     */
    public DefaultSqlHelperDsManager(DataSource primaryDs) {
        this(primaryDs, null);
    }

    @Override
    public LogicDsMeta getLogicDsMeta(String switchedDsName) {
        return logicDsName2DsMeta.get(switchedDsName);
    }

    @Override
    public List<String> all() {
        return new ArrayList<>(logicDsName2DsMeta.keySet());
    }

    @Override
    public List<String> allDatasourceIds() {
        return new ArrayList<>(dsId2Datasource.keySet());
    }

    /**
     * Put.
     *
     * @param logicName the logic name
     * @param dsMeta    the ds meta
     */
    @Override
    synchronized public void put(String logicName, LogicDsMeta dsMeta) {
        lock.lock();
        try{
            log.warn("添加逻辑数据源" + logicName);
            if (logicDsName2DsMeta.containsKey(logicName)) {
                throw new SqlHelperException("数据源名称已存在");
            }
            DataSource newDatasource = null;
            if (dsId2Datasource.get(dsMeta.getDatasourceId()) == null) {
                if (dsMeta.getCreateFunc() == null) {
                    throw new SqlHelperException("缺少createFunc数据源创建方法，无法初始化" + dsMeta.getDatasourceId() + "数据源");
                }
                log.warn("为逻辑数据源 " + logicName + " 初始化新的数据源 " + dsMeta.getDatasourceId());
                try{
                    newDatasource = dsMeta.getCreateFunc().call();
                }catch (Exception e){
                    new SqlHelperException("初始化"+logicName+"数据源失败",e);
                }
            } else {
                log.warn("逻辑数据源" + logicName + "复用已存在的数据源ID:" + dsMeta.getDatasourceId());
//                if(dsMeta.getSubspace()==null){
//                    throw new SqlHelperException("复用已有数据源,subspace不能为null");
//                }
                newDatasource = upgradeDatasourceByDatasourceId(dsMeta.getDatasourceId());
            }
            dsId2Datasource.put(dsMeta.getDatasourceId(), newDatasource);
            // 保存逻辑数据源信息
            logicDsName2DsMeta.put(logicName, dsMeta);
            List<String> logicNames = dsId2LogicDsName.getOrDefault(dsMeta.getDatasourceId(),new ArrayList<>());
            if(logicNames.size()==1){
                // 复用数据源 但是原引用逻辑数据源没有设置子空间 自动为其设置
                LogicDsMeta logicDsMeta = logicDsName2DsMeta.get(logicNames.get(0));
                if(logicDsMeta.getSubspace() == null){
                    try(Connection connection = newDatasource.getConnection()){
                        log.warn("逻辑数据源"+logicDsMeta.getSubspace()+"与其他数据源共享但是未设置subspace，尝试从连接中自动获取");
                        logicDsMeta.setSubspace(SupportedConnectionSubspaceChange.getCurrentSubspaceIfSupport(connection,dsMeta.getExpectedSubspaceType()));
                        log.warn("从连接中获取成功，设置subspace为"+logicDsMeta.getSubspace());
                    } catch (SQLException e) {
                        throw new SqlHelperException(e);
                    }
                }
            }
            logicNames.add(logicName);
            dsId2LogicDsName.put(dsMeta.getDatasourceId(),logicNames);
        }finally {
            lock.unlock();
        }

    }


    @Override
    public DataSource remove(String logicName) {
        lock.lock();
        try{
            LogicDsMeta logicDsMeta = logicDsName2DsMeta.remove(logicName);
            if(logicDsMeta == null){
                throw new SqlHelperException(logicName+"数据源名称不存在");
            }
            if(!PRIMARY_DATASOURCE_ID.equals(logicDsMeta.getDatasourceId()) && dsId2Datasource.get(logicDsMeta.getDatasourceId())!=null ){
                for(LogicDsMeta item:logicDsName2DsMeta.values()){
                    if(logicDsMeta.getDatasourceId().equals(item.getDatasourceId())){
                        // 存在其他引用，不删除数据源id
                        return null;
                    }
                }
                return dsId2Datasource.remove(logicDsMeta.getDatasourceId());
            }
            return null;
        }finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(String logicName) {
        return logicDsName2DsMeta.containsKey(logicName);
    }

    @Override
    public boolean containsId(String dsId) {
        return dsId2Datasource.containsKey(dsId);
    }

    @Override
    public DataSource getByName(String logicName) {
        if(logicName == null){
            return getById(PRIMARY_DATASOURCE_ID);
        }
        LogicDsMeta logicDsMeta = logicDsName2DsMeta.get(logicName);
        if(logicDsMeta!=null){
            return getById(logicDsMeta.getDatasourceId());
        }
        return null;
    }

    @Override
    public DataSource getById(String dsId) {
        return dsId2Datasource.get(dsId);
    }


    /**
     * Gets by logic name.
     *
     * @param logicName the logic name
     * @return the by logic name
     */
    public LogicDsMeta getByLogicName(String logicName) {
        LogicDsMeta re = logicDsName2DsMeta.get(logicName);
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
        DataSource dataSource = getById(datasourceId);
        if (dsUpgradeCallback != null) {
            log.warn("升级数据源 " + datasourceId);
            return dsId2Datasource.put(datasourceId, dsUpgradeCallback.apply(dataSource));
        }
        return dataSource;
    }
}
