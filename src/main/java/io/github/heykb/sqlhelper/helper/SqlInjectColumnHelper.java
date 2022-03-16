package io.github.heykb.sqlhelper.helper;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.StringUtils;
import com.sun.istack.internal.Nullable;
import io.github.heykb.sqlhelper.config.SqlHelperException;
import io.github.heykb.sqlhelper.handler.ConditionInjectInfo;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.typeHandler.ColumnFilterTypeHandler;
import io.github.heykb.sqlhelper.utils.CommonUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.type.TypeHandler;

import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.druid.sql.ast.statement.SQLJoinTableSource.JoinType.*;

/**
 * The type Sql utils.
 *
 * @author heykb
 */
public class SqlInjectColumnHelper {
    private static final Log log = LogFactory.getLog(SqlInjectColumnHelper.class);
    public static final String SUB_QUERY_ALIAS = "_sql_help_";
    public static final String DEFAULT_TB_ALIAS_PREFIX = "inj_";

    private String tbPrefix;

    private String tbAliasPrefix;

    private DbType dbType;

    private Collection<InjectColumnInfoHandler> infoHandlers;

    private LogicDeleteInfoHandler logicDeleteInfoHandler;

    private Configuration configuration;

    /**
     * Instantiates a new Sql inject column helper.
     *
     * @param infoHandlers the inject items
     */
    public SqlInjectColumnHelper(DbType dbType, Collection<InjectColumnInfoHandler> infoHandlers, @Nullable String tbAliasPrefix) {
        if(tbAliasPrefix == null){
            this.tbAliasPrefix = DEFAULT_TB_ALIAS_PREFIX;
        }else{
            this.tbAliasPrefix = tbAliasPrefix;
        }
        this.dbType = dbType;
        this.infoHandlers = infoHandlers;
        if (this.infoHandlers == null) {
            this.infoHandlers = new ArrayList<>();
        }
        for (InjectColumnInfoHandler injectColumnInfoHandler : infoHandlers) {
            if (injectColumnInfoHandler instanceof LogicDeleteInfoHandler) {
                logicDeleteInfoHandler = (LogicDeleteInfoHandler) injectColumnInfoHandler;
                break;
            }
        }
    }

    public SqlInjectColumnHelper(DbType dbType, Collection<InjectColumnInfoHandler> infoHandlers) {
        this(dbType, infoHandlers, DEFAULT_TB_ALIAS_PREFIX);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * 为sql语句注入字段
     *
     * @param sql the sql
     * @return the string
     */
    public String handleSql(String sql) {
        return handleSql(sql, null, null);
    }
    /**
     * 为sql语句注入字段
     * 为select语句过滤结果字段
     * @param sql
     * @param filterColumns
     * @param parameterMappings
     * @return
     */
    public String handleSql(String sql, Collection<String> filterColumns, List<ParameterMapping> parameterMappings) {
        SQLStatement sqlStatement = SQLUtils.parseSingleStatement(sql, this.dbType);
        SqlCommandType commandType = null;
        if (sqlStatement instanceof SQLSelectStatement) {
            commandType = SqlCommandType.SELECT;
            addCondition2Select(((SQLSelectStatement) sqlStatement).getSelect(),true, commandType);
            sqlStatement = filterColumns(sqlStatement, filterColumns);
        } else if (sqlStatement instanceof SQLUpdateStatement) {
            // 为更新语句中的查询语句添加附加条件
            commandType = SqlCommandType.UPDATE;
            SQLUpdateStatement updateStatement = (SQLUpdateStatement) sqlStatement;
            SQLExpr where = updateStatement.getWhere();
            addCondition2QueryInWhere(where, commandType);
            SQLExpr newCondition = newEqualityCondition(SQLUtils.normalize(updateStatement.getTableName().getSimpleName()), updateStatement.getTableSource().getAlias(), where, SqlCommandType.UPDATE, true);
            updateStatement.setWhere(newCondition);
            handlerColumn2Update(updateStatement, filterColumns, parameterMappings);
        } else if (sqlStatement instanceof SQLDeleteStatement) {
            // 为删除语句中的查询语句添加附加条件
            commandType = SqlCommandType.DELETE;
            SQLDeleteStatement deleteStatement = (SQLDeleteStatement) sqlStatement;
            SQLExpr where = deleteStatement.getWhere();
            addCondition2QueryInWhere(where, commandType);
            SQLExpr newCondition = newEqualityCondition(SQLUtils.normalize(deleteStatement.getTableName().getSimpleName()),
                    deleteStatement.getTableSource().getAlias(), where, commandType, true);
            deleteStatement.setWhere(newCondition);
            sqlStatement = toLogicDeleteSql(deleteStatement);
        } else if (sqlStatement instanceof SQLInsertStatement) {
            commandType = SqlCommandType.INSERT;
            // 为插入语句中的查询语句添加附加条件
            SQLInsertStatement insertStatement = (SQLInsertStatement) sqlStatement;
            SQLSelect sqlSelect = insertStatement.getQuery();
            if (sqlSelect != null) {
                SQLSelectQueryBlock queryObject = (SQLSelectQueryBlock) sqlSelect.getQuery();
                addCondition2Select(sqlSelect.getQuery(),true,commandType);
            }
            addColumn2Insert(insertStatement);
        }
        String re = SQLUtils.toSQLString(sqlStatement, this.dbType);
        return re;
    }




    private SQLStatement filterColumns(SQLStatement originSql, Collection<String> filterColumns) {
        final SQLStatement tmp = SQLUtils.parseSingleStatement("select * from a", dbType);
        if (CollectionUtils.isEmpty(filterColumns)) {
            return originSql;
        }
        List<String> filterFields = filterColumns.stream().map(filterColumn -> CommonUtils.adaptePropertyName(filterColumn, this.configuration)).collect(Collectors.toList());
        log.warn("sql方式过滤查询列：" + String.join(",", filterFields));
        SQLSelectQueryBlock queryObject = (SQLSelectQueryBlock) ((SQLSelectStatement) originSql).getSelect().getQuery();
        List<SQLSelectItem> originItems = queryObject.getSelectList();
        Set<String> selectItemNames = new HashSet<>();
        for (SQLSelectItem originItem : originItems) {
            String name = originItem.toString();
            if (originItem.getAlias() != null) {
                name = originItem.getAlias();
            } else if (originItem.getExpr() instanceof SQLPropertyExpr) {
                name = ((SQLPropertyExpr) originItem.getExpr()).getName();
            }
            selectItemNames.add(SQLUtils.normalize(name));
        }

        if (selectItemNames.contains("*")) {
            log.warn("sql方式过滤失败");
            return originSql;
        }
        for (String filterColumn : filterFields) {
            selectItemNames.remove(filterColumn);
        }
        if (selectItemNames.size() < originItems.size()) {
            if (tmp instanceof SQLSelectStatement) {
                SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) ((SQLSelectStatement) tmp).getSelect().getQuery();
                sqlSelectQueryBlock.setFrom(((SQLSelectStatement) originSql).getSelect(), SUB_QUERY_ALIAS);
                List<SQLSelectItem> sqlSelectItems = sqlSelectQueryBlock.getSelectList();
                sqlSelectItems.clear();
                sqlSelectItems.addAll(selectItemNames.stream().map(selectItemName -> new SQLSelectItem(new SQLIdentifierExpr(selectItemName))).collect(Collectors.toList()));
                filterColumns.clear();
                return tmp;
            }
        }
        return originSql;
    }

    /**
     * 为出现在where中的子查询 附件条件
     *
     * @param expr the where
     */
    private void addCondition2QueryInWhere(SQLExpr expr, SqlCommandType commandType) {
        if (expr instanceof SQLInSubQueryExpr) {
            SQLInSubQueryExpr inWhere = (SQLInSubQueryExpr) expr;
            SQLSelect subSelectObject = inWhere.getSubQuery();
            addCondition2Select(subSelectObject.getQuery(), false,commandType);
        } else if (expr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr opExpr = (SQLBinaryOpExpr) expr;
            SQLExpr left = opExpr.getLeft();
            SQLExpr right = opExpr.getRight();
            addCondition2QueryInWhere(left, commandType);
            addCondition2QueryInWhere(right, commandType);
        } else if (expr instanceof SQLQueryExpr) {
            addCondition2Select(((SQLQueryExpr) expr).getSubQuery(), false,commandType);
        }
    }

    private SQLStatement toLogicDeleteSql(SQLDeleteStatement sqlStatement) {
        if (logicDeleteInfoHandler == null) {
            return sqlStatement;
        }
        SQLStatement logicSqlStatement = SQLUtils.parseSingleStatement(logicDeleteInfoHandler.getDeleteSqlDemo(), dbType);
        if (logicSqlStatement instanceof SQLUpdateStatement) {
            log.warn(String.format("表%s转逻辑删除", SQLUtils.normalize(sqlStatement.getTableName().getSimpleName())));
            SQLUpdateStatement updateStatement = (SQLUpdateStatement) logicSqlStatement;
            updateStatement.setTableSource(sqlStatement.getTableSource());
            updateStatement.setWhere(sqlStatement.getWhere());
        } else {
            throw new SqlHelperException("逻辑删除sqlDemo配置错误，应该是update语句如：update xx set isDelete = false where id = xx");
        }
        return logicSqlStatement;
    }

    /**
     * 为insert语句添加字段
     *
     * @param sqlStatement
     */
    private void addColumn2Insert(SQLInsertStatement sqlStatement) {
        List<SQLExpr> columns = sqlStatement.getColumns();
        List<SQLInsertStatement.ValuesClause> valuesClauses = sqlStatement.getValuesList();
        String tableName = SQLUtils.normalize(sqlStatement.getTableName().getSimpleName());
        for (InjectColumnInfoHandler infoHandler : infoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.INSERT) > 0) {
                if (!infoHandler.checkTableName(tableName) || !infoHandler.checkCommandType(SqlCommandType.INSERT)) {
                    continue;
                }
                int index = -1;
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.configuration);
                // 跳过
                if (infoHandler.isExistSkip()) {
                    for (int i = 0; i < columns.size(); i++) {
                        if (nameEquals(columns.get(i), columnName)) {
                            index = i;
                            break;
                        }
                    }
                }
                // 不跳过或者没找到
                if (!infoHandler.isExistSkip() || index == -1) {
                    log.warn(String.format("表%s新增插入项:%s = %s", tableName, columnName, infoHandler.getValue()));
                    columns.add(new SQLIdentifierExpr(columnName));

                }
                for (SQLInsertStatement.ValuesClause values : valuesClauses) {
                    // 不跳过或者没找到
                    if (!infoHandler.isExistSkip() || index == -1) {
                        values.addValue(infoHandler.toSQLExpr(dbType));
                    }
                }
            }
        }
    }

    /**
     * 为更新语句添加字段
     *
     * @param sqlStatement
     */
    private void handlerColumn2Update(SQLUpdateStatement sqlStatement, Collection<String> filterColumns, List<ParameterMapping> parameterMappings) {

        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        String tableName = SQLUtils.normalize(sqlStatement.getTableName().getSimpleName());
        for (InjectColumnInfoHandler infoHandler : infoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.UPDATE) > 0) {
                if (!infoHandler.checkTableName(tableName) || !infoHandler.checkCommandType(SqlCommandType.UPDATE)) {
                    continue;
                }
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.configuration);
                // 跳过
                if (infoHandler.isExistSkip()) {
                    for (SQLUpdateSetItem item : items) {
                        if (item.columnMatch(columnName)) {
                            continue;
                        }
                    }
                }
                SQLUpdateSetItem sqlUpdateSetItem = new SQLUpdateSetItem();
                log.warn(String.format("新增更新列：%s = %s", columnName, infoHandler.getValue()));
                sqlUpdateSetItem.setColumn(new SQLIdentifierExpr(columnName));
                sqlUpdateSetItem.setValue(infoHandler.toSQLExpr(dbType));
                items.add(sqlUpdateSetItem);
            }
        }
        if (CollectionUtils.isEmpty(filterColumns)) {
            return;
        }

        Set<String> filterFields = filterColumns.stream().map(filterColumn -> CommonUtils.adaptePropertyName(filterColumn, this.configuration)).collect(Collectors.toSet());
        log.warn("过滤更新列：" + String.join(",", filterColumns));
        List<SQLUpdateSetItem> needRemove = new ArrayList<>();
        List<Integer> removeIndex = new ArrayList<>();
        for (SQLUpdateSetItem item : items) {
            SQLExpr expr = item.getColumn();
            String name = item.toString();
            if (expr instanceof SQLPropertyExpr) {
                name = SQLUtils.normalize(((SQLPropertyExpr) expr).getName());
            } else if (expr instanceof SQLIdentifierExpr) {
                name = SQLUtils.normalize(((SQLIdentifierExpr) expr).getName());
            }
            if (filterFields.contains(name)) {
                needRemove.add(item);
                if (item.getValue() instanceof SQLVariantRefExpr) {
                    removeIndex.add(((SQLVariantRefExpr) item.getValue()).getIndex() + 1);
                }
            }

        }
        if (parameterMappings != null) {
            for (ParameterMapping parameterMapping : parameterMappings) {

                TypeHandler typeHandler = parameterMapping.getTypeHandler();
                SystemMetaObject.forObject(parameterMapping).setValue("typeHandler", new ColumnFilterTypeHandler(typeHandler, removeIndex));
            }
        }
        needRemove.forEach(item -> items.remove(item));
    }

    /**
     * 为查询语句添加附加条件（包括where中的子查询、from的子查询、以及表连接添加过滤条件）
     *
     * @param select
     * @param commandType
     */
    private void addCondition2Select(SQLObject select, boolean isFirstLevelQuery, SqlCommandType commandType) {
        if (select instanceof SQLSelect) {
            addCondition2Select(((SQLSelect) select).getQuery(), isFirstLevelQuery, commandType);
        } else if (select instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) select;
            addCondition2Query(sqlSelectQueryBlock, sqlSelectQueryBlock.getFrom(), isFirstLevelQuery, commandType);
        } else if (select instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) select;
            addCondition2Select(sqlUnionQuery.getLeft(), isFirstLevelQuery, commandType);
            addCondition2Select(sqlUnionQuery.getRight(), isFirstLevelQuery, commandType);
        }
    }

    /**
     * 为单独一个查询块添加附加条件
     * @param queryBody
     * @param fromBody
     * @param isFirstLevelQuery 当前queryBody是否第一层级别的查询块
     */
    private void addCondition2Query(SQLSelectQueryBlock queryBody, SQLTableSource fromBody, boolean isFirstLevelQuery, SqlCommandType commandType) {
        SQLExpr originCondition = queryBody.getWhere();
        if (fromBody instanceof SQLExprTableSource) {
            String tableName = SQLUtils.normalize(((SQLExprTableSource) fromBody).getTableName());
            String alias = fromBody.getAlias();
            addCondition2QueryInWhere(originCondition, commandType);
            originCondition = newEqualityCondition(tableName, alias, originCondition, commandType, isFirstLevelQuery);
            queryBody.setWhere(originCondition);
        } else if (fromBody instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinObject = (SQLJoinTableSource) fromBody;
            SQLTableSource left = joinObject.getLeft();
            SQLTableSource right = joinObject.getRight();
            SQLExpr onCondition = joinObject.getCondition();
            SQLJoinTableSource.JoinType joinType = joinObject.getJoinType();
            // 处理左外连接添加condition的位置
            if(left instanceof  SQLExprTableSource && (joinType == RIGHT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)){
                String tableName = SQLUtils.normalize(((SQLExprTableSource) left).getTableName());
                onCondition = newEqualityCondition(tableName, left.getAlias(), onCondition, commandType, isFirstLevelQuery);
            }else{
                addCondition2Query(queryBody, left, isFirstLevelQuery, commandType);
            }
            // 处理右外连接添加condition的位置
            if(right instanceof  SQLExprTableSource && (joinType == LEFT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)){
                String tableName = SQLUtils.normalize(((SQLExprTableSource) right).getTableName());
                onCondition = newEqualityCondition(tableName, right.getAlias(), onCondition, commandType, isFirstLevelQuery);
            } else {
                addCondition2Query(queryBody, right, isFirstLevelQuery, commandType);
            }
            joinObject.setCondition(onCondition);
        } else if (fromBody instanceof SQLSubqueryTableSource) {
            SQLSelect subSelectObject = ((SQLSubqueryTableSource) fromBody).getSelect();
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addCondition2Query(subQueryObject, subQueryObject.getFrom(), isFirstLevelQuery, commandType);
        } else if (fromBody instanceof SQLUnionQueryTableSource) {
            // 支持union 查询
            SQLUnionQueryTableSource sqlUnionQueryTableSource = (SQLUnionQueryTableSource) fromBody;
            SQLSelectQueryBlock left = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getLeft();
            SQLSelectQueryBlock right = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getRight();
            addCondition2Query(left, left.getFrom(), isFirstLevelQuery, commandType);
            addCondition2Query(right, right.getFrom(), isFirstLevelQuery, commandType);
        } else {
            throw new SqlHelperException("不支持的sql语句");
        }

    }

    /**
     * 判断
     *
     * @param column
     * @param columnName
     * @return
     */
    private boolean nameEquals(SQLExpr column, String columnName) {
        if (column instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) column).nameEquals(columnName);
        } else if (column instanceof SQLPropertyExpr) {
            ((SQLPropertyExpr) column).nameEquals(columnName);
        }
        return false;
    }

    /**
     * 判断查询表达式中是否已存在字段
     *
     * @param condition
     * @param aliasFieldName
     * @return
     */
    private boolean contains(SQLExpr condition, String aliasFieldName) {
        boolean contains = false;
        if (condition instanceof SQLBinaryOpExpr) {
            SQLExpr left = ((SQLBinaryOpExpr) condition).getLeft();
            SQLExpr right = ((SQLBinaryOpExpr) condition).getRight();
            if (left instanceof SQLPropertyExpr && ((SQLPropertyExpr) left).nameEquals(aliasFieldName)) {
                contains = true;
            }
            return contains || contains(right, aliasFieldName);
        }
        return false;
    }

    /**
     * 返回添加了附加条件的condition语句
     *
     * @param tableName       the table name
     * @param tableAlias      the table alias
     * @param originCondition the origin condition
     * @return the sql expr
     */
    private SQLExpr newEqualityCondition(String tableName, String tableAlias, SQLExpr originCondition, SqlCommandType commandType, boolean isFirstLevelQuery) {
        SQLExpr re = originCondition;

        for (InjectColumnInfoHandler infoHandler : infoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.CONDITION) > 0) {
                if (!infoHandler.checkTableName(tableName) || !infoHandler.checkCommandType(commandType) || !infoHandler.checkIsFirstLevelQuery(isFirstLevelQuery)) {
                    continue;
                }
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.configuration);
                if (columnName != null && infoHandler.isExistSkip() && contains(originCondition, columnName)) {
                    continue;
                }
                SQLExpr condition = null;

                if (infoHandler instanceof ConditionInjectInfo) {
                    condition = ((ConditionInjectInfo) infoHandler).toConditionSQLExpr(tableAlias, dbType, configuration);
                } else {
                    String aliasFieldName = StringUtils.isEmpty(tableAlias) ? tableName + "." + columnName : tableAlias + "." + columnName;
                    condition = new SQLBinaryOpExpr(new SQLIdentifierExpr(aliasFieldName), infoHandler.toSQLExpr(dbType), CommonUtils.convert(infoHandler.op()));

                }
                log.warn(String.format("表%s添加过滤条件：%s", tableName, condition.toString()));
                re = re == null ? condition : SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, re);
            }
        }
        return re;
    }




//    /**
//     * inj前缀优先级3    没有alias优先级1  其他前缀优先级0
//     *
//     * @param selectItem
//     * @param checkAliasPrefix
//     * @return
//     */
//    int getSelectItemPriority(SQLSelectItem selectItem, boolean checkAliasPrefix) {
//        if (selectItem == null) {
//            return checkAliasPrefix ? 2 : -1;
//        }
//        String alias = null;
//        if (selectItem.getExpr() instanceof SQLPropertyExpr) {
//            SQLPropertyExpr expr = (SQLPropertyExpr) selectItem.getExpr();
//            if (expr.getOwner() instanceof SQLIdentifierExpr) {
//                alias = ((SQLIdentifierExpr) expr.getOwner()).getName();
//            }
//        }
//        return alias == null ? 1 : alias.startsWith(tbAliasPrefix) ? 3 : 0;
//    }
//
//private void addSelectItem2Query(SQLSelectQueryBlock queryBody) {
//    addSelectItem2Query(queryBody, false);
//}
//    private void addSelectItem2Query(SQLSelectQueryBlock queryBody, boolean checkAliasPrefix) {
////        List<SQLSelectItem> sqlSelectItems = queryBody.getSelectList();
////        SQLSelectItem addItem = null;
////        for(SQLSelectItem sqlSelectItem:sqlSelectItems){
////            int one = getSelectItemPriority(sqlSelectItem,checkAliasPrefix);
////            int pre = getSelectItemPriority(addItem,checkAliasPrefix);
////            if(one>pre){
////                addItem = sqlSelectItem;
////            }else if(one == pre){
////                if(sqlSelectItem.getExpr() instanceof SQLPropertyExpr){
////                    SQLPropertyExpr expr = (SQLPropertyExpr) sqlSelectItem.getExpr();
////                    if("*".equals(expr.getName())){
////                        addItem = sqlSelectItem;
////                    }
////                }
////                if(sqlSelectItem.getExpr() instanceof SQLIdentifierExpr){
////                    SQLIdentifierExpr expr = (SQLIdentifierExpr) sqlSelectItem.getExpr();
////                    if("*".equals(expr.getName())){
////                        addItem = sqlSelectItem;
////                    }
////                }
////            }
////        }
////
////        if(addItem!=null){
////            if(addItem.getExpr() instanceof SQLPropertyExpr){
////                SQLPropertyExpr expr = (SQLPropertyExpr) addItem.getExpr();
////                if(!"*".equals(expr.getName())){
////                    SQLSelectItem item= addItem.clone();
////                    SQLPropertyExpr addExpr = (SQLPropertyExpr) item.getExpr();
////                    addExpr.setName("tenant_id");
////                    sqlSelectItems.add(item);
////                }
////            }
////            if(addItem.getExpr() instanceof SQLIdentifierExpr){
////                SQLIdentifierExpr expr = (SQLIdentifierExpr) addItem.getExpr();
////                if(!"*".equals(expr.getName())){
////                    SQLSelectItem item= addItem.clone();
////                    SQLIdentifierExpr addExpr = (SQLIdentifierExpr) item.getExpr();
////                    addExpr.setName("tenant_id");
////                    sqlSelectItems.add(item);
////                }
////            }
////        }
//    }
}
