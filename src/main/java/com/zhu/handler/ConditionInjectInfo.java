package com.zhu.handler;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.util.StringUtils;
import com.zhu.helper.Configuration;
import com.zhu.utils.CommonUtils;

/**
 * The interface Condition inject info.
 *
 * @author heykb
 */
public interface ConditionInjectInfo extends InjectColumnInfoHandler{
    @Override
    default int getInjectTypes() {
        return CONDITION;
    }
    @Override
    default String getColumnName(){
        return null;
    }
    @Override
    default String getValue(){
        return null;
    }

    /**
     * To condition sql expr sql expr.
     *
     * @param tableAlias    the table alias
     * @param dbType        the db type
     * @param configuration the configuration
     * @return the sql expr
     */
    SQLExpr toConditionSQLExpr(String tableAlias,DbType dbType, Configuration configuration);
}
