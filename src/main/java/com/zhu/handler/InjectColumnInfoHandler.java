package com.zhu.handler;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBooleanExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;

/**
 * 代表一条自动注入配置，如配置字段名称、字段值、注入类型等
 * @author heykb
 */
public interface InjectColumnInfoHandler {

    /**
     * 注入到条件查询中
     */
    int CONDITION = 1;
    /**
     * 注入插入内容中
     */
    int INSERT = 1<<1;
    /**
     * 注入到更新内容中
     */
    int UPDATE = 1<<2;

    /**
     * 设置注入字段名称
     * @return
     */
    String getColumnName();

    /**
     * 设置注入字段的值
     * @return
     */
    Object getValue();

    /**
     * 设置注入类型 CONDITION|INSERT|UPDATE
     * @return
     */
    int getInjectTypes();

    /**
     * 当注入目标中已存在该字段时，true跳过，false替换
     * @return
     */
    default boolean isExistSkip(){
        return false;
    }

    /**
     * 设置表级别过滤逻辑
     * @param tableName
     * @return
     */
    default boolean ignoreTable(String tableName){
        return false;
    }
    /**
     * 设置mapperId方法级别过滤逻辑
     * @param mapperId
     * @return
     */
    default boolean ignoreMapperId(String mapperId){
        return false;
    }
    /**
     * 注入value是否是sql方法
     * @return
     */
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
