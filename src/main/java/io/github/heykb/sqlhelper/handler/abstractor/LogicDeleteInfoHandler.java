package io.github.heykb.sqlhelper.handler.abstractor;

import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import org.apache.ibatis.mapping.SqlCommandType;

/**
 * 实现InjectColumnInfoHandler自动注入配置接口，代表逻辑删除自动注入配置
 * 配置逻辑删除的字段名称、字段值、以及逻辑删除语法样例（其中表名和where条件会在运行中被替换）
 * 使用时继承该类，并注入为一个bean
 *
 * @author heykb   sqlDemo: update xx set status = false where id = 'xx'     columnName: status     notDeleteValue: true
 */
public abstract class LogicDeleteInfoHandler implements InjectColumnInfoHandler {
    /**
     * 设置逻辑删除实例：update xx set status = false
     * （其中表名和where条件会在运行中被替换）
     *
     * @return sql demo
     */
    public abstract String getDeleteSqlDemo();

    /**
     * 设置正常未被删除时，逻辑删除字段的value
     *
     * @return not deleted value
     */
    public abstract String getNotDeletedValue();

    @Override
    public String getValue() {
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

    @Override
    public boolean checkCommandType(SqlCommandType commandType) {
        return SqlCommandType.DELETE != commandType;
    }
}
