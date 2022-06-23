package sample.mybatis.xml.config;

import io.github.heykb.sqlhelper.spring.dynamicds.SpringSqlHelperDsManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqlHelperManagerAutoConfiguration {

    @Bean
    public SpringSqlHelperDsManager springSqlHelperDsManager(){
        return new SpringSqlHelperDsManager();
    }
}
