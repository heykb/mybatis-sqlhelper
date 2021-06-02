package com.zhu.config;

import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.dynamic.DynamicFindInjectInfoHandler;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

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
