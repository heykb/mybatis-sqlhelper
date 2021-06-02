package com.zhu.handler.abstractor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.util.StringUtils;
import com.zhu.handler.ConditionInjectInfo;
import com.zhu.helper.Configuration;
import com.zhu.utils.CommonUtils;

/**
 * The type Condition inject info handler.
 *
 * @author heykb
 */
public abstract class ConditionInjectInfoHandler implements ConditionInjectInfo {

    @Override
    abstract public String getColumnName();

    @Override
    abstract public String getValue();

    @Override
    public SQLExpr toConditionSQLExpr(String tableAlias, DbType dbType, Configuration configuration) {
        String columnName = CommonUtils.adaptePropertieName(getColumnName(),configuration);
        String aliasFieldName = StringUtils.isEmpty(tableAlias) ? columnName : tableAlias + "." + columnName;
        StringBuilder conditionSql = new StringBuilder(aliasFieldName);
        conditionSql.append(" ").append(op()).append(" ").append(getValue());
        return SQLUtils.toSQLExpr(conditionSql.toString());
    }


}
