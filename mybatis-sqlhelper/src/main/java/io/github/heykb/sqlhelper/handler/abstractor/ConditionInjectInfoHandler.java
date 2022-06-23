package io.github.heykb.sqlhelper.handler.abstractor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import io.github.heykb.sqlhelper.handler.ConditionInjectInfo;
import io.github.heykb.sqlhelper.utils.CommonUtils;

import java.util.Map;

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
    public SQLExpr toConditionSQLExpr(String tableAlias, DbType dbType, Map<String, String> columnAliasMap, boolean isMapUnderscoreToCamelCase) {
        String columnName = CommonUtils.adaptePropertyName(getColumnName(), columnAliasMap, isMapUnderscoreToCamelCase);
        String aliasFieldName = CommonUtils.isEmpty(tableAlias) ? columnName : tableAlias + "." + columnName;
        StringBuilder conditionSql = new StringBuilder(aliasFieldName);
        conditionSql.append(" ").append(op()).append(" ").append(getValue());
        return SQLUtils.toSQLExpr(conditionSql.toString());
    }


}
