package io.github.heykb.sqlhelper.spring;

import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.handler.dynamic.DynamicFindColumnFilterHandler;
import io.github.heykb.sqlhelper.handler.dynamic.DynamicFindInjectInfoHandler;
import io.github.heykb.sqlhelper.interceptor.SqlHelperPlugin;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Properties;


public class SqlHelperPluginFactoryBean implements FactoryBean<SqlHelperPlugin> {
    @Autowired(required = false)
    private List<InjectColumnInfoHandler> injectColumnInfoHandlers;
    @Autowired(required = false)
    private List<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    @Autowired(required = false)
    private DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler;
    @Autowired(required = false)
    private DynamicFindInjectInfoHandler dynamicFindInjectInfoHandler;

    @Setter
    private Properties properties;

    @Override
    public SqlHelperPlugin getObject() throws Exception {
        SqlHelperPlugin re = new SqlHelperPlugin();
        re.setDynamicFindColumnFilterHandler(dynamicFindColumnFilterHandler);
        re.setDynamicFindInjectInfoHandler(dynamicFindInjectInfoHandler);
        re.setInjectColumnInfoHandlers(injectColumnInfoHandlers);
        re.setColumnFilterInfoHandlers(columnFilterInfoHandlers);
        re.setProperties(properties);
        return re;
    }


    @Override
    public Class<?> getObjectType() {
        return SqlHelperPlugin.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
