package io.github.heykb.sqlhelper.handler.abstractor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.util.StringUtils;
import io.github.heykb.sqlhelper.handler.ConditionInjectInfo;
import io.github.heykb.sqlhelper.helper.Configuration;
import io.github.heykb.sqlhelper.utils.CommonUtils;

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
        String columnName = CommonUtils.adaptePropertyName(getColumnName(),configuration);
        String aliasFieldName = StringUtils.isEmpty(tableAlias) ? columnName : tableAlias + "." + columnName;
        StringBuilder conditionSql = new StringBuilder(aliasFieldName);
        conditionSql.append(" ").append(op()).append(" ").append(getValue());
        return SQLUtils.toSQLExpr(conditionSql.toString());
    }


}
