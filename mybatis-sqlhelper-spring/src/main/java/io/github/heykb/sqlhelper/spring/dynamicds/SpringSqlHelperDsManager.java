package io.github.heykb.sqlhelper.spring.dynamicds;

import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDsManager;
import io.github.heykb.sqlhelper.dynamicdatasource.LogicDsMeta;
import io.github.heykb.sqlhelper.dynamicdatasource.DefaultSqlHelperDsManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

public class SpringSqlHelperDsManager implements BeanPostProcessor, SqlHelperDsManager {
    private DefaultSqlHelperDsManager sqlHelperDsManager;
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof DataSource){
            if(bean instanceof SqlHelperDynamicDataSourceProxy){
                this.sqlHelperDsManager = ((SqlHelperDynamicDataSourceProxy)bean).getSqlHelperDsManager();
                return bean;
            }else{
                io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDynamicDataSourceProxy dataSource = new SqlHelperDynamicDataSourceProxy((DataSource) bean);
                this.sqlHelperDsManager = dataSource.getSqlHelperDsManager();
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
}
