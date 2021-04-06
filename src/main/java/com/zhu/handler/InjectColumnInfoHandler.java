package com.zhu.handler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBooleanExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;

/**
 * @author heykb
 */
public interface InjectColumnInfoHandler {

    int CONDITION = 1;
    int INSERT = 1<<1;
    int UPDATE = 1<<2;
    String getColumnName();

    Object getValue();

    int getInjectTypes();

    default boolean isExistSkip(){
        return false;
    }

    default boolean ignoreTable(String tableName){
        return false;
    }
    default boolean ignoreMapperId(String mapperId){
        return false;
    }
    default boolean isMethod(){
        return false;
    }
    default SQLExpr toSQLExpr(){
        if(getValue() instanceof String){
            return isMethod()? new SQLMethodInvokeExpr((String) getValue()):new SQLCharExpr((String) getValue());
        }
        if(getValue() instanceof Number){
            return new SQLIntegerExpr((Number) getValue());
        }
        if(getValue() instanceof Boolean){
            return new SQLBooleanExpr((Boolean) getValue());
        }
        return new SQLCharExpr(( getValue().toString()));
    }
}
