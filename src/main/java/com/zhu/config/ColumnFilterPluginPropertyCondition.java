package com.zhu.config;

import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * @author heykb
 */
public class ColumnFilterPluginPropertyCondition extends AnyNestedCondition {

    ColumnFilterPluginPropertyCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }
    @ConditionalOnProperty(value = "sqlhelper.columnFilterType",havingValue = "result")
    static class OnResultType{}

    @ConditionalOnProperty(value = "sqlhelper.columnFilterType",havingValue = "smarter")
    static class OnSmarterType {}

}
