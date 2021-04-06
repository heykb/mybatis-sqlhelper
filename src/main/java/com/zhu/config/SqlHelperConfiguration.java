package com.zhu.config;


import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.LogicDeleteInfoHandler;
import com.zhu.handler.TenantInfoHanlder;
import com.zhu.handler.defaultimpl.DefaultLogicDeleteInfoHandler;
import com.zhu.interceptor.InjectColumnPlugin;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author heykb
 */
@Configuration
@ConditionalOnProperty("sqlhelper.enable")
public class SqlHelperConfiguration implements ApplicationContextAware {

    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;
    @Bean
    @ConditionalOnMissingBean(LogicDeleteInfoHandler.class)
    public DefaultLogicDeleteInfoHandler defaultLogicDeleteInfoHandler(){
        return new DefaultLogicDeleteInfoHandler();
    }
    @Bean
    public SqlHelperConfig sqlHelperConfig(){
        return new SqlHelperConfig();
    }
    @Bean
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
        return new InjectColumnPlugin(sqlHelperConfig.getDbtype(),handlers);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        injectColumnInfoHandlers = applicationContext.getBeansOfType(InjectColumnInfoHandler.class).values();
    }
}
