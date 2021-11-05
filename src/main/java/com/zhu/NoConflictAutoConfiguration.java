package com.zhu;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;

/**
 * @author heykb
 */
@ConditionalOnMissingClass("com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration")
@ImportAutoConfiguration(SqlHelperAutoConfiguration.class)
public class NoConflictAutoConfiguration {

}
