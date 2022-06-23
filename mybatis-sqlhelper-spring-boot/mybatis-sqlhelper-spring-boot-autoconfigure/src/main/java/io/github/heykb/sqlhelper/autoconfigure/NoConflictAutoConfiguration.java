package io.github.heykb.sqlhelper.autoconfigure;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * @author heykb
 */
@Deprecated
@ConditionalOnMissingClass("com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration")
@ImportAutoConfiguration(SqlHelperAutoConfiguration.class)
public class NoConflictAutoConfiguration {


}
