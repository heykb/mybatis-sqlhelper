package io.github.heykb.sqlhelper.spring.dynamicds;

import io.github.heykb.sqlhelper.dynamicdatasource.DefaultSqlHelperDsManager;
import io.github.heykb.sqlhelper.dynamicdatasource.LogicDsMeta;
import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDsManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.util.List;

public class SpringSqlHelperDsManager implements BeanPostProcessor, SqlHelperDsManager {
    private SqlHelperDsManager sqlHelperDsManager;
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof DataSource){
            if(bean instanceof SqlHelperDynamicDataSourceProxy){
                this.sqlHelperDsManager = ((SqlHelperDynamicDataSourceProxy)bean).getSqlHelperDsManager();
                return bean;
            }else{
                this.sqlHelperDsManager = new DefaultSqlHelperDsManager((DataSource) bean);
                io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDynamicDataSourceProxy dataSource = new SqlHelperDynamicDataSourceProxy(this.sqlHelperDsManager);
                return dataSource;
            }
        }
        return bean;
    }

    @Override
    public void put(String logicName, LogicDsMeta dsMeta) {
        sqlHelperDsManager.put(logicName,dsMeta);
    }

    @Override
    public DataSource remove(String logicName) {
        return sqlHelperDsManager.remove(logicName);
    }

    @Override
    public boolean contains(String logicName) {
        return sqlHelperDsManager.contains(logicName);
    }

    @Override
    public boolean containsId(String dsId) {
        return sqlHelperDsManager.contains(dsId);
    }

    @Override
    public DataSource getByName(String logicName) {
        return sqlHelperDsManager.getByName(logicName);
    }

    @Override
    public DataSource getById(String dsId) {
        return sqlHelperDsManager.getById(dsId);
    }

    @Override
    public LogicDsMeta getLogicDsMeta(String switchedDsName) {
        return sqlHelperDsManager.getLogicDsMeta(switchedDsName);
    }

    @Override
    public List<String> all() {
        return sqlHelperDsManager.all();
    }

    @Override
    public List<String> allDatasourceIds() {
        return sqlHelperDsManager.allDatasourceIds();
    }
}
