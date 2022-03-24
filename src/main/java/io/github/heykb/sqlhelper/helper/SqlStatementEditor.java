package io.github.heykb.sqlhelper.helper;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import io.github.heykb.sqlhelper.config.SqlHelperException;
import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;
import io.github.heykb.sqlhelper.handler.ConditionInjectInfo;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.utils.CommonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.druid.sql.ast.statement.SQLJoinTableSource.JoinType.*;

public class SqlStatementEditor {
    private static final Log log = LogFactory.getLog(SqlStatementEditor.class);
    public static final String SUB_QUERY_ALIAS = "_sql_help_";
    private DbType dbType;
    private SQLStatement sqlStatement;
    private Map<String, String> columnAliasMap;
    private boolean isMapUnderscoreToCamelCase = true;
    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;
    private Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    private List<LogicDeleteInfoHandler> logicDeleteInfoHandlers = new ArrayList<>();
    private MySchemaStatVisitor schemaStatVisitor = new MySchemaStatVisitor();
    private Set<String> tableNames = new HashSet<>();
    private Map<String,Set<String>> tableName2needFilterColumns = new HashMap<>();
    private SqlStatementEditor() {
    }

    public static class Builder {
        private SqlStatementEditor sqlStatementEditorFactory;
        private String sql;

        public Builder(String sql, DbType dbType) {
            this.sqlStatementEditorFactory = new SqlStatementEditor();
            this.sql = sql;
            this.sqlStatementEditorFactory.dbType = dbType;
        }

        public SqlStatementEditor.Builder injectColumnInfoHandlers(Collection<InjectColumnInfoHandler> injectColumnInfoHandlers) {
            this.sqlStatementEditorFactory.injectColumnInfoHandlers = injectColumnInfoHandlers;
            return this;
        }

        public SqlStatementEditor.Builder columnFilterInfoHandlers(Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers) {
            this.sqlStatementEditorFactory.columnFilterInfoHandlers = columnFilterInfoHandlers;
            return this;
        }

        public SqlStatementEditor.Builder columnAliasMap(Map<String, String> columnAliasMap) {
            this.sqlStatementEditorFactory.columnAliasMap = columnAliasMap;
            return this;
        }

        public SqlStatementEditor.Builder isMapUnderscoreToCamelCase(boolean isMapUnderscoreToCamelCase) {
            this.sqlStatementEditorFactory.isMapUnderscoreToCamelCase = isMapUnderscoreToCamelCase;
            return this;
        }

        public SqlStatementEditor build() {
            this.sqlStatementEditorFactory.sqlStatement = SQLUtils.parseSingleStatement(sql, this.sqlStatementEditorFactory.dbType);
            if(this.sqlStatementEditorFactory.injectColumnInfoHandlers!=null){
                for(InjectColumnInfoHandler item:this.sqlStatementEditorFactory.injectColumnInfoHandlers){
                    if(item instanceof LogicDeleteInfoHandler){
                        this.sqlStatementEditorFactory.logicDeleteInfoHandlers.add((LogicDeleteInfoHandler) item);
                    }
                }
            }else{
                this.sqlStatementEditorFactory.injectColumnInfoHandlers = new ArrayList<>();
            }
            return this.sqlStatementEditorFactory;
        }

    }

    @Data
    @AllArgsConstructor
    public static class Result {
        private String sql;
        private List<Integer> removedParamIndex;
        private boolean columnFilterSuccess = true;
        private Set<String> failedFilterColumns;

        public Result(String sql, List<Integer> removedParamIndex) {
            this.sql = sql;
            this.removedParamIndex = removedParamIndex;
        }

    }

    void preFilterByTableNames(Class clazz) {
        if(tableNames.size()==0){
            for (TableStat.Name name : schemaStatVisitor.getTables().keySet()) {
                tableNames.add(name.getName());
            }
        }
        if (clazz.isAssignableFrom(LogicDeleteInfoHandler.class)) {
            if (logicDeleteInfoHandlers != null) {
                logicDeleteInfoHandlers = logicDeleteInfoHandlers.stream().filter(item -> {
                    for (String name : tableNames) {
                        if (item.checkTableName(name)) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
            }
            return;
        }
        if (clazz.isAssignableFrom(ColumnFilterInfoHandler.class)) {
            if (columnFilterInfoHandlers != null) {
                columnFilterInfoHandlers = columnFilterInfoHandlers.stream().filter(item -> {
                    boolean re = false;
                    for (String name : tableNames) {
                        if (item.checkTableName(name)) {
                            Set<String> needFilterColumns = tableName2needFilterColumns.get(name);
                            if(needFilterColumns == null){
                                needFilterColumns = new HashSet<>();
                                tableName2needFilterColumns.put(name,needFilterColumns);
                            }
                            for(String filterColumn:item.getFilterColumns()){
                                needFilterColumns.add(filterColumn);
                                needFilterColumns.add(CommonUtils.adaptePropertyName(filterColumn,columnAliasMap,isMapUnderscoreToCamelCase));
                            }
                            re = true;
                        }
                    }
                    return re;
                }).collect(Collectors.toList());
            }
            return;
        }
        if (clazz.isAssignableFrom(InjectColumnInfoHandler.class)) {
            if (injectColumnInfoHandlers != null) {
                injectColumnInfoHandlers = injectColumnInfoHandlers.stream().filter(item -> {
                    for (String name : tableNames) {
                        if (item.checkTableName(name)) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
            }
            return;
        }
    }

    public Result processing() {
        if (sqlStatement instanceof SQLSelectStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class);
            preFilterByTableNames(ColumnFilterInfoHandler.class);
            return processing((SQLSelectStatement) sqlStatement);
        } else if (sqlStatement instanceof SQLUpdateStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class);
            preFilterByTableNames(ColumnFilterInfoHandler.class);
            return processing((SQLUpdateStatement) sqlStatement);
        } else if (sqlStatement instanceof SQLDeleteStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class);
            preFilterByTableNames(LogicDeleteInfoHandler.class);
            return processing((SQLDeleteStatement) sqlStatement);
        } else if (sqlStatement instanceof SQLInsertStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class);
            return processing((SQLInsertStatement) sqlStatement);
        }
        return null;
    }


    Result processing(SQLSelectStatement sqlSelectStatement) {
        Set<String> failedFilterColumns = new HashSet<>();
        if(tableName2needFilterColumns!=null && tableName2needFilterColumns.size()>0){
            failedFilterColumns = filterColumn2Select(sqlSelectStatement);
        }
        addCondition2Select(sqlSelectStatement.getSelect(), SqlCommandType.SELECT);
        Result result = new Result(SQLUtils.toSQLString(sqlSelectStatement), null);
        result.setFailedFilterColumns(failedFilterColumns);
        return result;
    }

    Result processing(SQLInsertStatement insertStatement) {
        // 为插入语句中的查询语句添加附加条件
        SQLSelect sqlSelect = insertStatement.getQuery();
        if (sqlSelect != null) {
            addCondition2Select(sqlSelect.getQuery(), SqlCommandType.INSERT);
        }
        addColumn2Insert(insertStatement);
        return new Result(SQLUtils.toSQLString(insertStatement), null);
    }

    Result processing(SQLDeleteStatement deleteStatement) {
        // 为删除语句中的查询语句添加附加条件
        SQLExpr where = deleteStatement.getWhere();
        addCondition2QueryInWhere(where, SqlCommandType.DELETE);
        SQLExpr newCondition = newEqualityCondition(SQLUtils.normalize(deleteStatement.getTableName().getSimpleName()),
                deleteStatement.getTableSource().getAlias(), where, SqlCommandType.DELETE);
        deleteStatement.setWhere(newCondition);
        SQLStatement sqlStatement = toLogicDeleteSql(deleteStatement);
        return new Result(SQLUtils.toSQLString(sqlStatement), null);
    }


    Result processing(SQLUpdateStatement updateStatement) {
        // 为where中的查询语句添加附加条件
        SQLExpr where = updateStatement.getWhere();
        addCondition2QueryInWhere(where, SqlCommandType.UPDATE);
        SQLExpr newCondition = newEqualityCondition(SQLUtils.normalize(updateStatement.getTableName().getSimpleName()), updateStatement.getTableSource().getAlias(), where, SqlCommandType.UPDATE);
        updateStatement.setWhere(newCondition);

        addColumn2Update(updateStatement);
        List<Integer> removedParamIndex = filterColumn2Update(updateStatement);
        return new Result(SQLUtils.toSQLString(updateStatement), removedParamIndex);
    }

    List<SQLSelectQueryBlock> filterColumn2Select(SQLSelect sqlSelect){
        SQLSelectQuery query = sqlSelect.getQuery();
        List<SQLSelectQueryBlock> re = new ArrayList<>();
        if (query instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) query;
            if(queryBlock.getFrom() instanceof SQLSubqueryTableSource){
                re.addAll(filterColumn2Select(((SQLSubqueryTableSource) queryBlock.getFrom()).getSelect()));
            }else{
                re.add(queryBlock);
            }
        }
        if (query instanceof SQLUnionQuery) {
            SQLUnionQuery union = (SQLUnionQuery) query;
            preVisitor(union,re);
        }
        return re;
    }
    Set<String> filterColumn2Select(SQLSelectStatement sqlSelectStatement){
        List<SQLSelectQueryBlock> queryBlocks = filterColumn2Select(sqlSelectStatement.getSelect());
        boolean sqlFilterEnable = true;
        for(SQLSelectQueryBlock queryBlock:queryBlocks){
            for(SQLSelectItem selectItem:queryBlock.getSelectList()){
                if(selectItem.getExpr() instanceof SQLAllColumnExpr){
                    sqlFilterEnable = false;
                    break;
                }
            }
        }
        if(sqlFilterEnable){
            for(SQLSelectQueryBlock queryBlock:queryBlocks){
                filterColumn2Select(queryBlock);
            }
            return null;
        }
        Set<String> columns = new HashSet<>();
        // 返回使用结果集方式过滤
        tableName2needFilterColumns.values().forEach(item->{
            columns.addAll(item);
        });
        return columns;
    }
    void filterColumn2Select(SQLSelectQueryBlock queryBlock){
        List<SQLSelectItem> selectItems = queryBlock.getSelectList();
        SchemaStatVisitor visitor = new SchemaStatVisitor();
        queryBlock.accept(visitor);
        Collection<TableStat.Column> columns = visitor.getColumns();
        Map<String,String> column2tableName = new HashMap<>();
        for(TableStat.Column column:columns){
            String columnName = column.getName();
            column2tableName.put(columnName,column.getTable());
        }
        for(SQLSelectItem selectItem:selectItems){
            String columnName = null;
            if(selectItem.getExpr() instanceof SQLIdentifierExpr){
                columnName = ((SQLIdentifierExpr)selectItem.getExpr()).getName();
            }
            if(selectItem.getExpr() instanceof SQLPropertyExpr) {
                columnName = ((SQLPropertyExpr) selectItem.getExpr()).getName();
            }
            if(columnName!=null){
                if(column2tableName.containsKey(columnName)){
                    String tableName = column2tableName.get(columnName);
                    if(tableName2needFilterColumns.containsKey(tableName)){
                        if(tableName2needFilterColumns.get(tableName).contains(columnName)){
                            log.warn(String.format("sql方式过滤%s.%s",tableName,columnName));
                            selectItem.setAlias(columnName);
                            selectItem.setExpr(new SQLNullExpr());
                        }
                    }
                }
            }
        }
    }

    void preVisitor(SQLUnionQuery sqlUnionQuery,List<SQLSelectQueryBlock> queryBlocks){
        if(sqlUnionQuery.getLeft() instanceof SQLSelectQueryBlock){
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) sqlUnionQuery.getLeft();
            if(queryBlock.getFrom() instanceof SQLSubqueryTableSource){
                queryBlocks.addAll(filterColumn2Select(((SQLSubqueryTableSource) queryBlock.getFrom()).getSelect()));
            }else{
                queryBlocks.add(queryBlock);
            }
        }
        if(sqlUnionQuery.getRight() instanceof SQLSelectQueryBlock){
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) sqlUnionQuery.getRight();
            if(queryBlock.getFrom() instanceof SQLSubqueryTableSource){
                queryBlocks.addAll(filterColumn2Select(((SQLSubqueryTableSource) queryBlock.getFrom()).getSelect()));
            }else{
                queryBlocks.add(queryBlock);
            }
        }
        if(sqlUnionQuery.getLeft() instanceof SQLUnionQuery){
            preVisitor((SQLUnionQuery) sqlUnionQuery.getLeft(),queryBlocks);
        }
        if(sqlUnionQuery.getRight() instanceof SQLUnionQuery){
            preVisitor((SQLUnionQuery) sqlUnionQuery.getRight(),queryBlocks);
        }
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
            addCondition2Select(subSelectObject.getQuery(), commandType);
        } else if (expr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr opExpr = (SQLBinaryOpExpr) expr;
            SQLExpr left = opExpr.getLeft();
            SQLExpr right = opExpr.getRight();
            addCondition2QueryInWhere(left, commandType);
            addCondition2QueryInWhere(right, commandType);
        } else if (expr instanceof SQLQueryExpr) {
            addCondition2Select(((SQLQueryExpr) expr).getSubQuery(), commandType);
        }
    }

    private SQLStatement toLogicDeleteSql(SQLDeleteStatement deleteStatement) {
        String tableName = SQLUtils.normalize(deleteStatement.getTableName().getSimpleName());
        LogicDeleteInfoHandler useLogicDeleteInfoHandle = null;
        for (LogicDeleteInfoHandler logicDeleteInfoHandler : logicDeleteInfoHandlers) {
            if (logicDeleteInfoHandler.checkTableName(tableName)) {
                useLogicDeleteInfoHandle = logicDeleteInfoHandler;
                break;
            }
        }
        if (useLogicDeleteInfoHandle == null) {
            return deleteStatement;
        }
        SQLStatement logicSqlStatement = SQLUtils.parseSingleStatement(useLogicDeleteInfoHandle.getDeleteSqlDemo(), dbType);
        if (logicSqlStatement instanceof SQLUpdateStatement) {
            log.warn(String.format("表%s转逻辑删除", tableName));
            SQLUpdateStatement updateStatement = (SQLUpdateStatement) logicSqlStatement;
            updateStatement.setTableSource(deleteStatement.getTableSource());
            updateStatement.setWhere(deleteStatement.getWhere());
        } else {
            throw new SqlHelperException("逻辑删除sqlDemo配置错误，应该是update语句如：update xx set isDelete = false where id = xx");
        }
        return logicSqlStatement;
    }

    /**
     * 为insert语句添加字段
     */
    private void addColumn2Insert(SQLInsertStatement insertStatement) {
        List<SQLExpr> columns = insertStatement.getColumns();
        List<SQLInsertStatement.ValuesClause> valuesClauses = insertStatement.getValuesList();
        String tableName = SQLUtils.normalize(insertStatement.getTableName().getSimpleName());
        for (InjectColumnInfoHandler infoHandler : injectColumnInfoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.INSERT) > 0) {
                if (!infoHandler.checkTableName(tableName) || !infoHandler.checkCommandType(SqlCommandType.INSERT)) {
                    continue;
                }
                int index = -1;
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.columnAliasMap, this.isMapUnderscoreToCamelCase);
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

    private List<Integer> filterColumn2Update(SQLUpdateStatement sqlStatement) {
        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        String tableName = SQLUtils.normalize(sqlStatement.getTableName().getSimpleName());
        Set<String> filterFields = new HashSet<>();
        for (ColumnFilterInfoHandler columnFilterInfoHandler : columnFilterInfoHandlers) {
            if (columnFilterInfoHandler.checkTableName(tableName) && columnFilterInfoHandler.getFilterColumns() != null) {
                for (String filterColumn : columnFilterInfoHandler.getFilterColumns()) {
                    filterFields.add(CommonUtils.adaptePropertyName(filterColumn, this.columnAliasMap, this.isMapUnderscoreToCamelCase));
                }
            }
        }
        log.warn("从更新语句中删除列：" + String.join(",", filterFields));
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
        needRemove.forEach(item -> items.remove(item));
        return removeIndex;
    }

    /**
     * 为更新语句添加字段
     *
     * @param sqlStatement
     */
    private void addColumn2Update(SQLUpdateStatement sqlStatement) {
        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        String tableName = SQLUtils.normalize(sqlStatement.getTableName().getSimpleName());
        for (InjectColumnInfoHandler infoHandler : injectColumnInfoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.UPDATE) > 0) {
                if (!infoHandler.checkTableName(tableName) || !infoHandler.checkCommandType(SqlCommandType.UPDATE)) {
                    continue;
                }
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.columnAliasMap, this.isMapUnderscoreToCamelCase);
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
    }

    /**
     * 为查询语句添加附加条件（包括where中的子查询、from的子查询、以及表连接添加过滤条件）
     *
     * @param select
     * @param commandType
     */
    private void addCondition2Select(SQLObject select, SqlCommandType commandType) {
        if (select instanceof SQLSelect) {
            addCondition2Select(((SQLSelect) select).getQuery(), commandType);
        } else if (select instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) select;
            addCondition2Query(sqlSelectQueryBlock, sqlSelectQueryBlock.getFrom(), commandType);
        } else if (select instanceof SQLUnionQuery) {
            SQLUnionQuery sqlUnionQuery = (SQLUnionQuery) select;
            addCondition2Select(sqlUnionQuery.getLeft(), commandType);
            addCondition2Select(sqlUnionQuery.getRight(), commandType);
        }
    }


    /**
     * 为单独一个查询块添加附加条件
     *
     * @param queryBody
     * @param fromBody
     */
    private void addCondition2Query(SQLSelectQueryBlock queryBody, SQLTableSource fromBody, SqlCommandType commandType) {
        SQLExpr originCondition = queryBody.getWhere();
        if (fromBody instanceof SQLExprTableSource) {
            String tableName = SQLUtils.normalize(((SQLExprTableSource) fromBody).getTableName());
            String alias = fromBody.getAlias();
            addCondition2QueryInWhere(originCondition, commandType);
            originCondition = newEqualityCondition(tableName, alias, originCondition, commandType);
            queryBody.setWhere(originCondition);
        } else if (fromBody instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinObject = (SQLJoinTableSource) fromBody;
            SQLTableSource left = joinObject.getLeft();
            SQLTableSource right = joinObject.getRight();
            SQLExpr onCondition = joinObject.getCondition();
            SQLJoinTableSource.JoinType joinType = joinObject.getJoinType();
            // 处理左外连接添加condition的位置
            if (left instanceof SQLExprTableSource && (joinType == RIGHT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)) {
                String tableName = SQLUtils.normalize(((SQLExprTableSource) left).getTableName());
                onCondition = newEqualityCondition(tableName, left.getAlias(), onCondition, commandType);
            } else {
                addCondition2Query(queryBody, left, commandType);
            }
            // 处理右外连接添加condition的位置
            if (right instanceof SQLExprTableSource && (joinType == LEFT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)) {
                String tableName = SQLUtils.normalize(((SQLExprTableSource) right).getTableName());
                onCondition = newEqualityCondition(tableName, right.getAlias(), onCondition, commandType);
            } else {
                addCondition2Query(queryBody, right, commandType);
            }
            joinObject.setCondition(onCondition);
        } else if (fromBody instanceof SQLSubqueryTableSource) {
            SQLSelect subSelectObject = ((SQLSubqueryTableSource) fromBody).getSelect();
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addCondition2Query(subQueryObject, subQueryObject.getFrom(), commandType);
        } else if (fromBody instanceof SQLUnionQueryTableSource) {
            // 支持union 查询
            SQLUnionQueryTableSource sqlUnionQueryTableSource = (SQLUnionQueryTableSource) fromBody;
            SQLSelectQueryBlock left = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getLeft();
            SQLSelectQueryBlock right = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getRight();
            addCondition2Query(left, left.getFrom(), commandType);
            addCondition2Query(right, right.getFrom(), commandType);
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
    private SQLExpr newEqualityCondition(String tableName, String tableAlias, SQLExpr originCondition, SqlCommandType commandType) {
        SQLExpr re = originCondition;

        for (InjectColumnInfoHandler infoHandler : injectColumnInfoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.CONDITION) > 0) {
                if (!infoHandler.checkTableName(tableName) || !infoHandler.checkCommandType(commandType)) {
                    continue;
                }
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.columnAliasMap, this.isMapUnderscoreToCamelCase);
                if (columnName != null && infoHandler.isExistSkip() && contains(originCondition, columnName)) {
                    continue;
                }
                SQLExpr condition = null;

                if (infoHandler instanceof ConditionInjectInfo) {
                    condition = ((ConditionInjectInfo) infoHandler).toConditionSQLExpr(tableAlias, dbType, columnAliasMap, isMapUnderscoreToCamelCase);
                } else {
                    String aliasFieldName = CommonUtils.isEmpty(tableAlias) ? tableName + "." + columnName : tableAlias + "." + columnName;
                    condition = new SQLBinaryOpExpr(new SQLIdentifierExpr(aliasFieldName), infoHandler.toSQLExpr(dbType), CommonUtils.convert(infoHandler.op()));

                }
                log.warn(String.format("表%s添加过滤条件：%s", tableName, condition.toString()));
                re = re == null ? condition : SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, re);
            }
        }
        return re;
    }


}
