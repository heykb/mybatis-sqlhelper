package com.zhu.config;

import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import com.zhu.handler.dynamic.DynamicFindInjectInfoHandler;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;

/**
 * @author heykb
 */
public class InjectColumnPluginCondition extends AnyNestedCondition {

    InjectColumnPluginCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }
    @ConditionalOnBean(InjectColumnInfoHandler.class)
    static class OnInjectColumnInfoHandlerBean{}

    @ConditionalOnBean(DynamicFindInjectInfoHandler.class)
    static class OnDynamicFindInjectInfoHandlerBean {}

}
