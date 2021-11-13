package io.github.heykb.sqlhelper.autoconfigure;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * @author heykb
 */
@ConditionalOnMissingClass("com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration")
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@ImportAutoConfiguration(SqlHelperAutoConfiguration.class)
public class NoConflictAutoConfiguration {

}
