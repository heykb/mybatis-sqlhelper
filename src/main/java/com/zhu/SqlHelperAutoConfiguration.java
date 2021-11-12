package com.zhu;


import com.zhu.config.SqlHelperConfig;
import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.abstractor.LogicDeleteInfoHandler;
import com.zhu.handler.abstractor.TenantInfoHanlder;
import com.zhu.handler.defaultimpl.DefaultLogicDeleteInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import com.zhu.handler.dynamic.DynamicFindInjectInfoHandler;
import com.zhu.interceptor.SqlHelperPlugin;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author heykb
 */
@Configuration
@ConditionalOnProperty(value = "sqlhelper.enable",havingValue = "true",matchIfMissing = true)
public class SqlHelperAutoConfiguration implements ApplicationContextAware {
    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;
    @Autowired(required = false)
    private DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler;
    @Autowired(required = false)
    private DynamicFindInjectInfoHandler dynamicFindInjectInfoHandler;

    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;
    private Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    @Bean
    @ConditionalOnMissingBean(LogicDeleteInfoHandler.class)
    public DefaultLogicDeleteInfoHandler defaultLogicDeleteInfoHandler(){
        return new DefaultLogicDeleteInfoHandler();
    }
    @Bean
    public SqlHelperConfig sqlHelperConfig(){
        return  new SqlHelperConfig();
    }
    @PostConstruct
    public void addMyInterceptor() {
        if(CollectionUtils.isEmpty(injectColumnInfoHandlers) && CollectionUtils.isEmpty(columnFilterInfoHandlers) && dynamicFindInjectInfoHandler==null && dynamicFindColumnFilterHandler==null){
            return;
        }
        List<InjectColumnInfoHandler> handlers = new ArrayList<>();
        SqlHelperConfig sqlHelperConfig = sqlHelperConfig();
        if(sqlHelperConfig.isEnable()){
            for(InjectColumnInfoHandler injectColumnInfoHandler:injectColumnInfoHandlers){
                if(injectColumnInfoHandler instanceof LogicDeleteInfoHandler && !sqlHelperConfig.isLogicDeleteEnable()){
                    continue;
                }else if(injectColumnInfoHandler instanceof TenantInfoHanlder && !sqlHelperConfig.isMultiTenantEnable()){
                    continue;
                }
                handlers.add(injectColumnInfoHandler);
            }
        }
        SqlHelperPlugin re = new SqlHelperPlugin(sqlHelperConfig.getDbtype(),handlers);
        re.setTbAliasPrefix(sqlHelperConfig.getTbAliasPrefix());
        re.setDynamicFindInjectInfoHandler(dynamicFindInjectInfoHandler);

        re.setColumnFilterInfoHandlers(columnFilterInfoHandlers);
        re.setDynamicFindColumnFilterHandler(dynamicFindColumnFilterHandler);

        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(re);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        injectColumnInfoHandlers = applicationContext.getBeansOfType(InjectColumnInfoHandler.class).values();
        columnFilterInfoHandlers = applicationContext.getBeansOfType(ColumnFilterInfoHandler.class).values();
    }

}
