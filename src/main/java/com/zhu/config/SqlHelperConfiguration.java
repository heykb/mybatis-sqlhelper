package com.zhu.config;


import com.zhu.enums.ColumnFilterType;
import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.abstractor.LogicDeleteInfoHandler;
import com.zhu.handler.abstractor.TenantInfoHanlder;
import com.zhu.handler.defaultimpl.DefaultLogicDeleteInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import com.zhu.handler.dynamic.DynamicFindInjectInfoHandler;
import com.zhu.interceptor.ColumnFilterPlugin;
import com.zhu.interceptor.InjectColumnPlugin;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author heykb
 */
@Configuration
@ConditionalOnProperty(value = "sqlhelper.enable",havingValue = "true",matchIfMissing = true)
public class SqlHelperConfiguration implements ApplicationContextAware {

    @Value("${sqlhelper.columnFilterType:smarter}")
    private ColumnFilterType columnFilterType;

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
    @Bean
    @Conditional(InjectColumnPluginBeanCondition.class)
    public InjectColumnPlugin injectColumnPlugin(){
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
        InjectColumnPlugin re = new InjectColumnPlugin(sqlHelperConfig.getDbtype(),handlers);
        re.setTbAliasPrefix(sqlHelperConfig.getTbAliasPrefix());
        re.setDynamicFindInjectInfoHandler(dynamicFindInjectInfoHandler);
        if(ColumnFilterType.sql != columnFilterType){
            re.setColumnFilterInfoHandlers(columnFilterInfoHandlers);
            re.setDynamicFindColumnFilterHandler(dynamicFindColumnFilterHandler);
        }
        return re;
    }

    @Bean
    @Conditional({ColumnFilterPluginBeanCondition.class,ColumnFilterPluginPropertyCondition.class})
    public ColumnFilterPlugin columnFilterPlugin(){
        ColumnFilterPlugin re =  new ColumnFilterPlugin(this.columnFilterInfoHandlers);
        re.setColumnFilterInfoHandlers(columnFilterInfoHandlers);
        re.setDynamicFindColumnFilterHandler(dynamicFindColumnFilterHandler);
        return re;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        injectColumnInfoHandlers = applicationContext.getBeansOfType(InjectColumnInfoHandler.class).values();
        columnFilterInfoHandlers = applicationContext.getBeansOfType(ColumnFilterInfoHandler.class).values();
    }

}
