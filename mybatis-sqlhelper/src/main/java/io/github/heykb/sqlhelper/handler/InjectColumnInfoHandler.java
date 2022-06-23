package io.github.heykb.sqlhelper.handler;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.HashSet;
import java.util.Set;

/**
 * 代表一条自动注入配置，如配置字段名称、字段值、注入类型等
 *
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
     * 注入到查询内容中
     */
    int SELECT_ITEM=1<<3;

    /**
     * 设置注入字段名称
     *
     * @return column name
     */
    String getColumnName();

    /**
     * 设置注入字段的值,可以是值、方法甚至是子查询语句
     *
     * @return value
     */
    String getValue();


    /**
     * 设置注入类型 CONDITION|INSERT|UPDATE
     *
     * @return inject types
     */
    int getInjectTypes();

    /**
     * Op string.
     *
     * @return the string
     */
    default String op(){
        return "=";
    }

    /**
     * 当update和insert注入时已存在该字段时，true覆盖，false跳过。condition注入不做判断直接新增条件
     *
     * @return boolean
     */
    default boolean isExistOverride() {
        return true;
    }


    /**
     * 设置表级别过滤逻辑
     *
     * @param tableName the table name
     * @return boolean
     */
    default boolean checkTableName(String tableName){
        return true;
    }

    /**
     * 设置mapperId方法级别过滤逻辑
     *
     * @param mapperId the mapper id
     * @return boolean
     */
    default boolean checkMapperId(String mapperId){
        return true;
    }


    /**
     * 设置sql命令类型过滤逻辑
     * @param commandType
     * @return
     */
    default boolean checkCommandType(SqlCommandType commandType){
        Set<SqlCommandType> commandTypes = new HashSet<>();
        if ((getInjectTypes() & CONDITION) > 0) {
            return true;
        }
        if ((getInjectTypes() & INSERT) > 0) {
            commandTypes.add(SqlCommandType.INSERT);
        }
        if ((getInjectTypes() & UPDATE) > 0) {
            commandTypes.add(SqlCommandType.UPDATE);
        }
        return commandTypes.contains(commandType);
    }


    /**
     * To sql expr sql expr.
     *
     * @param dbType the db type
     * @return the sql expr
     */
    default SQLExpr toSQLExpr(DbType dbType){
        SQLExpr sqlExpr = SQLUtils.toSQLExpr((String) getValue(),dbType);
        return sqlExpr;
    }

}
