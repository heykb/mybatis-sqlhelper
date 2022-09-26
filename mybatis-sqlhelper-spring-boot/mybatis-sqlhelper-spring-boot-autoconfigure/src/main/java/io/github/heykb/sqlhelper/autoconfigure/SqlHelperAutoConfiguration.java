package io.github.heykb.sqlhelper.autoconfigure;

import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.spring.PropertyLogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.spring.SqlHelperPluginFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SqlHelperProperties.class)

public class SqlHelperAutoConfiguration {

    private final SqlHelperProperties properties;

    public SqlHelperAutoConfiguration(SqlHelperProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(LogicDeleteInfoHandler.class)
    PropertyLogicDeleteInfoHandler logicDeleteInfoHandler(){
        return properties.getLogicDelete();
    }

    @Bean
    SqlHelperPluginFactoryBean sqlHelperPluginFactoryBean(){
        SqlHelperPluginFactoryBean factoryBean =  new SqlHelperPluginFactoryBean();
        factoryBean.setProperties(properties.getProperties());
        return factoryBean;
    }

}
