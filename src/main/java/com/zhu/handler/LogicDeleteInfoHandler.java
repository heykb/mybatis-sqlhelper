package com.zhu.handler;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

/**
 * @author heykb
 *  sqlDemo: update xx set status = false where id = 'xx'
 *     columnName: status
 *     notDeleteValue: true
 */

public abstract class LogicDeleteInfoHandler implements InjectColumnInfoHandler{
    public abstract String getSqlDemo();

    public abstract Object getNotDeletedValue();

    @Override
    public Object getValue() {
        return getNotDeletedValue();
    }
    @Override
    public int getInjectTypes() {
        return CONDITION;
    }

}
