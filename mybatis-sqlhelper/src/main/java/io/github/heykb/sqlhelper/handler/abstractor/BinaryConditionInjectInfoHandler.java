package io.github.heykb.sqlhelper.handler.abstractor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.utils.CommonUtils;

import java.util.Map;

/**
 * 复杂条件注入
 *
 * @author heykb
 */
public abstract class BinaryConditionInjectInfoHandler implements InjectColumnInfoHandler {


    @Override
    public int getInjectTypes() {
        return CONDITION;
    }
    /**
     * Gets left condition inject info.
     *
     * @return the left condition inject info
     */
    abstract public InjectColumnInfoHandler getLeftConditionInjectInfo();

    /**
     * Gets right condition inject info.
     *
     * @return the right condition inject info
     */
    abstract public InjectColumnInfoHandler getRightConditionInjectInfo();

    @Override
    public String op() {
        return "and";
    }

    @Override
    public SQLExpr toConditionSQLExpr(String tableAlias, DbType dbType, Map<String, String> columnAliasMap, boolean isMapUnderscoreToCamelCase) {
        SQLExpr left = null;
        SQLExpr right = null;
        if(getLeftConditionInjectInfo()!=null){
            left = getLeftConditionInjectInfo().toConditionSQLExpr(tableAlias, dbType, columnAliasMap, isMapUnderscoreToCamelCase);
        }
        if(getRightConditionInjectInfo()!=null){
            right = getRightConditionInjectInfo().toConditionSQLExpr(tableAlias, dbType, columnAliasMap, isMapUnderscoreToCamelCase);
        }
        if(left != null && right !=null){
            return new SQLBinaryOpExpr(left, right, CommonUtils.convert(op()));
        }else if(left == null){
            return right;
        }else if(right == null){
            return left;
        }
        return null;
    }
}
