package io.github.heykb.sqlhelper.autoconfigure;


import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * @author heykb
 */
@Deprecated
@Configuration
@ConditionalOnClass(name="com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration")
@AutoConfigureBefore(name = "com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration")
@ImportAutoConfiguration(SqlHelperAutoConfiguration.class)
public class HasConflictAutoConfiguration {

}
