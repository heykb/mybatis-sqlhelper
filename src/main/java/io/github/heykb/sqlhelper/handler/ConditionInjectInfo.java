package io.github.heykb.sqlhelper.handler;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLExpr;
import io.github.heykb.sqlhelper.helper.Configuration;

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
