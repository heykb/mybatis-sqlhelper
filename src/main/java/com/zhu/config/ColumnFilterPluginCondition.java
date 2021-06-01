package com.zhu.config;

import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/**
 * @author heykb
 */
public class ColumnFilterPluginCondition extends AnyNestedCondition {

    ColumnFilterPluginCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }
    @ConditionalOnBean(ColumnFilterInfoHandler.class)
    static class OnColumnFilterInfoHandlerBean{}

    @ConditionalOnBean(DynamicFindColumnFilterHandler.class)
    static class OnDynamicFindColumnFilterHandlerBean {}

}
