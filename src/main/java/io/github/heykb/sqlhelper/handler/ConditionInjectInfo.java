package io.github.heykb.sqlhelper.handler;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLExpr;

import java.util.Map;

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
     * @return the sql expr
     */
    SQLExpr toConditionSQLExpr(String tableAlias, DbType dbType, Map<String, String> columnAliasMap, boolean isMapUnderscoreToCamelCase);
}
