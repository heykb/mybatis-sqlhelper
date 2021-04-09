package com.zhu.handler;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

/**
 * 实现InjectColumnInfoHandler自动注入配置接口，代表逻辑删除自动注入配置
 * 配置逻辑删除的字段名称、字段值、以及逻辑删除语法样例（其中表名和where条件会在运行中被替换）
 * 使用时继承该类，并注入为一个bean
 * @author heykb
 *  sqlDemo: update xx set status = false where id = 'xx'
 *     columnName: status
 *     notDeleteValue: true
 */

public abstract class LogicDeleteInfoHandler implements InjectColumnInfoHandler{
    /**
     * 设置逻辑删除实例：update xx set status = false where id = 'xx'
     *（其中表名和where条件会在运行中被替换）
     * @return
     */
    public abstract String getSqlDemo();

    /**
     * 设置正常未被删除时，逻辑删除字段的value
     * @return
     */
    public abstract Object getNotDeletedValue();

    @Override
    public Object getValue() {
        return getNotDeletedValue();
    }

    /**
     * 为所有子查询添加逻辑删除条件
     * @return
     */
    @Override
    public int getInjectTypes() {
        return CONDITION;
    }

}
