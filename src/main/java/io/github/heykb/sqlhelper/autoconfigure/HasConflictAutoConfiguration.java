package io.github.heykb.sqlhelper.autoconfigure;


import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * @author heykb
 */
@Configuration
@ConditionalOnClass(PageHelperAutoConfiguration.class)
@AutoConfigureBefore(PageHelperAutoConfiguration.class)
@ImportAutoConfiguration(SqlHelperAutoConfiguration.class)
public class HasConflictAutoConfiguration {

}
