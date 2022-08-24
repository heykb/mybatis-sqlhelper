package io.github.heykb.sqlhelper.helper;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.antspark.visitor.AntsparkSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.db2.visitor.DB2SchemaStatVisitor;
import com.alibaba.druid.sql.dialect.h2.visitor.H2SchemaStatVisitor;
import com.alibaba.druid.sql.dialect.hive.visitor.HiveSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.odps.visitor.OdpsSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.phoenix.visitor.PhoenixSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerSchemaStatVisitor;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.google.common.collect.Sets;
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

/**
 * The type Sql statement editor.
 */
public class SqlStatementEditor {
    private static final Log log = LogFactory.getLog(SqlStatementEditor.class);
    private static final Set<String> INNER_TABLE= Sets.newHashSet("dual");
    private DbType dbType;
    private SQLStatement sqlStatement;
    private Map<String, String> columnAliasMap;
    private boolean isMapUnderscoreToCamelCase = true;
    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;
    private Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    private List<LogicDeleteInfoHandler> logicDeleteInfoHandlers = new ArrayList<>();
    private SchemaStatVisitor schemaStatVisitor;
    private Set<String> tableNames = new HashSet<>();
    private Map<String,Set<String>> tableName2needFilterColumns = new HashMap<>();
    private Map<Object,LinkedHashSet<SQLExpr>> conditionAddedMap = new LinkedHashMap<>();
    private SqlStatementEditor() {
    }

    @Data
    @AllArgsConstructor
    public static class Result {
        private String sql;
        private List<Integer> removedParamIndex;
        private boolean columnFilterSuccess = true;
        private Set<String> failedFilterColumns;

        /**
         * Instantiates a new Result.
         *
         * @param sql               the sql
         * @param removedParamIndex the removed param index
         */
        public Result(String sql, List<Integer> removedParamIndex) {
            this.sql = sql;
            this.removedParamIndex = removedParamIndex;
        }

    }

    /**
     * The type Builder.
     */
    public static class Builder {
        private SqlStatementEditor sqlStatementEditor;
        private String sql;

        /**
         * Instantiates a new Builder.
         *
         * @param sql    the sql
         * @param dbType the db type
         */
        public Builder(String sql, DbType dbType) {
            this.sqlStatementEditor = new SqlStatementEditor();
            this.sql = sql;
            this.sqlStatementEditor.dbType = dbType;
        }

        /**
         * Inject column info handlers sql statement editor . builder.
         *
         * @param injectColumnInfoHandlers the inject column info handlers
         * @return the sql statement editor . builder
         */
        public SqlStatementEditor.Builder injectColumnInfoHandlers(Collection<InjectColumnInfoHandler> injectColumnInfoHandlers) {
            this.sqlStatementEditor.injectColumnInfoHandlers = injectColumnInfoHandlers;
            return this;
        }

        /**
         * Column filter info handlers sql statement editor . builder.
         *
         * @param columnFilterInfoHandlers the column filter info handlers
         * @return the sql statement editor . builder
         */
        public SqlStatementEditor.Builder columnFilterInfoHandlers(Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers) {
            this.sqlStatementEditor.columnFilterInfoHandlers = columnFilterInfoHandlers;
            return this;
        }

        /**
         * Column alias map sql statement editor . builder.
         *
         * @param columnAliasMap the column alias map
         * @return the sql statement editor . builder
         */
        public SqlStatementEditor.Builder columnAliasMap(Map<String, String> columnAliasMap) {
            this.sqlStatementEditor.columnAliasMap = columnAliasMap;
            return this;
        }

        /**
         * Is map underscore to camel case sql statement editor . builder.
         *
         * @param isMapUnderscoreToCamelCase the is map underscore to camel case
         * @return the sql statement editor . builder
         */
        public SqlStatementEditor.Builder isMapUnderscoreToCamelCase(boolean isMapUnderscoreToCamelCase) {
            this.sqlStatementEditor.isMapUnderscoreToCamelCase = isMapUnderscoreToCamelCase;
            return this;
        }

        /**
         * Build sql statement editor.
         *
         * @return the sql statement editor
         */
        public SqlStatementEditor build() {
            try {
                this.sqlStatementEditor.sqlStatement = SQLUtils.parseSingleStatement(sql, this.sqlStatementEditor.dbType);
            } catch (Exception e) {
                log.error("druid无法解析该sql,请检查语法。");
                this.sqlStatementEditor.sqlStatement = null;
                return this.sqlStatementEditor;
            }
            if(this.sqlStatementEditor.injectColumnInfoHandlers!=null){
                for(InjectColumnInfoHandler item:this.sqlStatementEditor.injectColumnInfoHandlers){
                    if(item instanceof LogicDeleteInfoHandler){
                        this.sqlStatementEditor.logicDeleteInfoHandlers.add((LogicDeleteInfoHandler) item);
                    }
                }
            }else{
                this.sqlStatementEditor.injectColumnInfoHandlers = new ArrayList<>();
            }
            if(this.sqlStatementEditor.columnFilterInfoHandlers==null){
                this.sqlStatementEditor.columnFilterInfoHandlers = new ArrayList<>();
            }
            this.sqlStatementEditor.schemaStatVisitor = getSchemaStatVisitor(this.sqlStatementEditor.dbType);
            return this.sqlStatementEditor;
        }

    }

    /**
     * Processing result.
     *
     * @return the result
     */
    public Result processing() {
        if(sqlStatement == null){
            return null;
        }
        if (sqlStatement instanceof SQLSelectStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class,SqlCommandType.SELECT);
            preFilterByTableNames(ColumnFilterInfoHandler.class,SqlCommandType.SELECT);
            if (tableNames.size() == 0) {
                return null;
            }
            return processing((SQLSelectStatement) sqlStatement);
        } else if (sqlStatement instanceof SQLUpdateStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class,SqlCommandType.UPDATE);
            preFilterByTableNames(ColumnFilterInfoHandler.class,SqlCommandType.UPDATE);
            if (tableNames.size() == 0) {
                return null;
            }
            return processing((SQLUpdateStatement) sqlStatement);
        } else if (sqlStatement instanceof SQLDeleteStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class,SqlCommandType.DELETE);
            preFilterByTableNames(LogicDeleteInfoHandler.class,SqlCommandType.DELETE);
            if (tableNames.size() == 0) {
                return null;
            }
            return processing((SQLDeleteStatement) sqlStatement);
        } else if (sqlStatement instanceof SQLInsertStatement) {
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class,SqlCommandType.INSERT);
            if (tableNames.size() == 0) {
                return null;
            }
            return processing((SQLInsertStatement) sqlStatement);
        }else if(sqlStatement instanceof OracleMultiInsertStatement){
            sqlStatement.accept(schemaStatVisitor);
            preFilterByTableNames(InjectColumnInfoHandler.class,SqlCommandType.INSERT);
            if (tableNames.size() == 0) {
                return null;
            }
            return processing((OracleMultiInsertStatement) sqlStatement);
        }
        conditionAddedMap = new LinkedHashMap<>();
        return new Result(sqlStatement.toString(),null);
    }



    Result processing(OracleMultiInsertStatement sqlStatement) {
        SQLSelect sqlSelect = sqlStatement.getSubQuery();
        addCondition2Select(sqlSelect.getQuery(), SqlCommandType.INSERT);
        List<Integer> removeIndex = new ArrayList<>();
        List<OracleMultiInsertStatement.Entry> entries = sqlStatement.getEntries();
        for(OracleMultiInsertStatement.Entry entry:entries){
            List<OracleMultiInsertStatement.InsertIntoClause> insertIntoClauses=new ArrayList<>();
            if(entry instanceof OracleMultiInsertStatement.ConditionalInsertClause){
                OracleMultiInsertStatement.ConditionalInsertClause conditionalInsertClause = (OracleMultiInsertStatement.ConditionalInsertClause) entry;
                List<OracleMultiInsertStatement.ConditionalInsertClauseItem> conditionalInsertClauseItems = conditionalInsertClause.getItems();
                if(conditionalInsertClause!=null){
                    for(OracleMultiInsertStatement.ConditionalInsertClauseItem conditionalInsertClauseItem:conditionalInsertClauseItems){
                        if(conditionalInsertClauseItem.getThen()!=null){
                            insertIntoClauses.add(conditionalInsertClauseItem.getThen());
                        }
                    }
                }
                if(conditionalInsertClause.getElseItem()!=null){
                    insertIntoClauses.add(conditionalInsertClause.getElseItem());
                }
            }else if(entry instanceof OracleMultiInsertStatement.InsertIntoClause){
                insertIntoClauses.add((OracleMultiInsertStatement.InsertIntoClause) entry);

            }
            for(OracleMultiInsertStatement.InsertIntoClause insertIntoClause:insertIntoClauses){
                addColumn2Insert(insertIntoClause.getTableName().getSimpleName(),insertIntoClause.getColumns(),insertIntoClause.getValuesList(),removeIndex);
            }
        }
        addAllCachedCondition();
        return new Result(SQLUtils.toSQLString(sqlStatement), removeIndex);
    }

    /**
     * Processing result.
     *
     * @param insertStatement the insert statement
     * @return the result
     */
    Result processing(SQLInsertStatement insertStatement) {
        // 为插入语句中的查询语句添加附加条件
        SQLSelect sqlSelect = insertStatement.getQuery();
        if(sqlSelect!=null){
            addCondition2Select(sqlSelect.getQuery(), SqlCommandType.INSERT);
        }
        addAllCachedCondition();
        List<Integer> removeIndex = addColumn2Insert(insertStatement);
        return new Result(SQLUtils.toSQLString(insertStatement), removeIndex);
    }


    /**
     * Processing result.
     *
     * @param sqlSelectStatement the sql select statement
     * @return the result
     */
    Result processing(SQLSelectStatement sqlSelectStatement) {
        Set<String> failedFilterColumns = new HashSet<>();
        if(tableName2needFilterColumns!=null && tableName2needFilterColumns.size()>0){
            failedFilterColumns = filterColumn2Select(sqlSelectStatement);
        }
        addCondition2Select(sqlSelectStatement.getSelect(), SqlCommandType.SELECT);
        addAllCachedCondition();
        Result result = new Result(SQLUtils.toSQLString(sqlSelectStatement), null);
        result.setFailedFilterColumns(failedFilterColumns);
        return result;
    }

    /**
     * Processing result.
     *
     * @param deleteStatement the delete statement
     * @return the result
     */
    Result processing(SQLDeleteStatement deleteStatement) {
        // 为删除语句中的查询语句添加附加条件
        SQLExpr where = deleteStatement.getWhere();
        addCondition2QueryInWhere(where, SqlCommandType.DELETE);
        Map<String,String> alias2normalizeTableName= new HashMap<>();
        if(deleteStatement.getUsing()!=null){
            addCondition2TableSource(deleteStatement,deleteStatement.getUsing(),SqlCommandType.DELETE,alias2normalizeTableName,null);
        }
        if(deleteStatement.getFrom()!=null){
            addCondition2TableSource(deleteStatement,deleteStatement.getFrom(),SqlCommandType.DELETE,alias2normalizeTableName,null);
        }
        addCondition2TableSource(deleteStatement,deleteStatement.getTableSource(),SqlCommandType.DELETE,alias2normalizeTableName,null);
        addAllCachedCondition();
        SQLStatement sqlStatement = toLogicDeleteSql(deleteStatement,alias2normalizeTableName);
        return new Result(SQLUtils.toSQLString(sqlStatement), null);
    }

    /**
     * Processing result.
     *
     * @param updateStatement the update statement
     * @return the result
     */
    Result processing(SQLUpdateStatement updateStatement) {
        // 为where中的查询语句添加附加条件
        SQLExpr where = updateStatement.getWhere();
        addCondition2QueryInWhere(where, SqlCommandType.UPDATE);
        Map<String,String> alias2normalizeTableNameMap = new HashMap<>();
        if(updateStatement.getFrom()!=null){
            addCondition2TableSource(updateStatement,updateStatement.getFrom(),SqlCommandType.UPDATE,alias2normalizeTableNameMap,null);
        }
        addCondition2TableSource(updateStatement,updateStatement.getTableSource(),SqlCommandType.UPDATE,alias2normalizeTableNameMap,null);
        // update tb t set username=?
        if(updateStatement.getTableSource() instanceof SQLExprTableSource && !alias2normalizeTableNameMap.containsKey(null)){
            String tableName = ((SQLExprTableSource) updateStatement.getTableSource()).getTableName();
            alias2normalizeTableNameMap.put(null,tableName);
        }
        addCondition2QueryInUpdateSetItem(updateStatement.getItems(),SqlCommandType.UPDATE);
        addAllCachedCondition();
        List<Integer> removedParamIndex = addColumn2Update(updateStatement,alias2normalizeTableNameMap);
        removedParamIndex.addAll(filterColumn2Update(updateStatement));
        return new Result(SQLUtils.toSQLString(updateStatement), removedParamIndex);
    }

    /**
     * Pre filter by table names.
     *
     * @param clazz          the clazz
     * @param sqlCommandType the sql command type
     */
    void preFilterByTableNames(Class clazz,SqlCommandType sqlCommandType) {
        if(tableNames.size()==0){
            for (TableStat.Name name : schemaStatVisitor.getTables().keySet()) {
                tableNames.add(name.getName());
            }
        }
        if (clazz.isAssignableFrom(LogicDeleteInfoHandler.class)) {
            if (logicDeleteInfoHandlers != null) {
                logicDeleteInfoHandlers = logicDeleteInfoHandlers.stream().filter(item -> {
                    if(!item.checkCommandType(sqlCommandType)){
                        return false;
                    }
                    for (String normalizedTableName : tableNames) {
                        if (item.checkTableName(normalizedTableName)) {
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
                    if(!item.checkCommandType(sqlCommandType)){
                        return false;
                    }
                    for (String normalizedTableName : tableNames) {
                        if (item.checkTableName(normalizedTableName)) {
                            Set<String> needFilterColumns = tableName2needFilterColumns.get(normalizedTableName);
                            if(needFilterColumns == null){
                                needFilterColumns = new HashSet<>();
                                tableName2needFilterColumns.put(normalizedTableName, needFilterColumns);
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
                    if(!item.checkCommandType(sqlCommandType)){
                        return false;
                    }
                    for (String normalizedTableName : tableNames) {
                        if (item.checkTableName(normalizedTableName)) {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
            }
            return;
        }
    }


    SQLStatement toLogicDeleteSql(SQLDeleteStatement deleteStatement,Map<String,String> alias2normalizeTableName) {
        if(this.logicDeleteInfoHandlers.size() == 0){
            return deleteStatement;
        }

        List<SQLUpdateSetItem> setItems = new ArrayList<>();
        SQLUpdateStatement updateStatement = null;
        //DELETE t1, t2 FROM t1 INNER JOIN t2 INNER JOIN t3
        //WHERE t1.id=t2.id AND t2.id=t3.id; 仅仅删除form之前的表
        // DELETE FROM t1, t2 USING t1 INNER JOIN t2 INNER JOIN t3
        //WHERE t1.id=t2.id AND t2.id=t3.id 仅仅删除from表
        Queue<SQLTableSource> queue = new LinkedList<>();
        if(deleteStatement.getTableSource()!=null){
            queue.offer(deleteStatement.getTableSource());
        }else{
            queue.offer(deleteStatement.getFrom());
        }
        while(!queue.isEmpty()){
            SQLTableSource item = queue.poll();
            if(item instanceof SQLExprTableSource){
                String alias = item.getAlias();
                String normalizeTableName = alias2normalizeTableName.get(((SQLExprTableSource) item).getTableName());
                for(LogicDeleteInfoHandler logicDeleteInfoHandler:this.logicDeleteInfoHandlers){
                    if (logicDeleteInfoHandler.checkTableName(normalizeTableName)) {
                        SQLStatement logicSqlStatement = SQLUtils.parseSingleStatement(logicDeleteInfoHandler.getDeleteSqlDemo(), dbType);
                        if (logicSqlStatement instanceof SQLUpdateStatement) {
                            updateStatement = (SQLUpdateStatement) logicSqlStatement;
                            log.warn(String.format("表%s转逻辑删除", normalizeTableName));
                        } else {
                            throw new SqlHelperException("逻辑删除sqlDemo配置错误，应该是update语句如：update xx set isDelete = false where id = xx");
                        }
                        break;
                    }
                }
                for(SQLUpdateSetItem setItemTemplate:updateStatement.getItems()){
                    if(alias==null && alias2normalizeTableName.size()<=2){
                        SQLUpdateSetItem updateSetItem = new SQLUpdateSetItem();
                        updateSetItem.setColumn(new SQLIdentifierExpr(getColumnName(setItemTemplate)));
                        updateSetItem.setValue(setItemTemplate.getValue());
                        setItems.add(updateSetItem);
                    }else {
                        SQLUpdateSetItem updateSetItem = new SQLUpdateSetItem();
                        updateSetItem.setColumn(new SQLPropertyExpr(alias==null?normalizeTableName:alias,getColumnName(setItemTemplate)));
                        updateSetItem.setValue(setItemTemplate.getValue());
                        setItems.add(updateSetItem);
                    }
                }
            }else if (item instanceof SQLJoinTableSource){
                queue.offer(((SQLJoinTableSource) item).getLeft());
                queue.offer(((SQLJoinTableSource) item).getRight());
            }
        }

        if(deleteStatement.getUsing()!=null){
//            updateStatement.setFrom(deleteStatement.getUsing());
            updateStatement.setTableSource(deleteStatement.getUsing());
        }else if(deleteStatement.getFrom()!=null){
//            updateStatement.setFrom(deleteStatement.getFrom());
//            updateStatement.setTableSource(deleteStatement.getTableSource());
            updateStatement.setTableSource(deleteStatement.getFrom());
        }else{
            updateStatement.setTableSource(deleteStatement.getTableSource());
        }

        updateStatement.setWhere(deleteStatement.getWhere());
        updateStatement.getItems().clear();
        updateStatement.getItems().addAll(setItems);
        return updateStatement;
    }

    /**
     * Filter column 2 select list.
     *
     * @param sqlSelect the sql select
     * @return the list
     */
    List<SQLSelectQueryBlock> filterColumn2Select(SQLSelect sqlSelect){
        SQLSelectQuery query = sqlSelect.getQuery();
        List<SQLSelectQueryBlock> re = new ArrayList<>();
        if (query instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) query;
            if (queryBlock.getFrom() instanceof SQLSubqueryTableSource) {
                re.addAll(filterColumn2Select(((SQLSubqueryTableSource) queryBlock.getFrom()).getSelect()));
            } else if (queryBlock.getFrom() instanceof SQLUnionQueryTableSource) {
                SQLUnionQuery union = ((SQLUnionQueryTableSource) queryBlock.getFrom()).getUnion();
                preVisitor(union,re);
            }else{
                re.add(queryBlock);
            }
        }else if (query instanceof SQLUnionQuery) {
            SQLUnionQuery union = (SQLUnionQuery) query;
            preVisitor(union,re);
        }
        return re;
    }

    /**
     * Filter column 2 select set.
     *
     * @param sqlSelectStatement the sql select statement
     * @return the set
     */
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

    /**
     * Filter column 2 select.
     *
     * @param queryBlock the query block
     */
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

    /**
     * Pre visitor.
     *
     * @param sqlUnionQuery the sql union query
     * @param queryBlocks   the query blocks
     */
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
    void addCondition2QueryInWhere(SQLExpr expr, SqlCommandType commandType) {
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

    /**
     * set name= (SELECT AddressList.PostCode FROM AddressList WHERE AddressList.PersonId = Persons.PersonId)
     * @param items
     * @param commandType
     */
    void addCondition2QueryInUpdateSetItem(List<SQLUpdateSetItem> items,SqlCommandType commandType){
        for(SQLUpdateSetItem setItem:items){
            if(setItem.getValue() instanceof SQLQueryExpr){
                addCondition2Select(((SQLQueryExpr) setItem.getValue()).getSubQuery(),commandType);
            }
        }
    }
    List<Integer> addColumn2Insert(SQLInsertStatement insertStatement){
        List<Integer> removeIndex = new ArrayList<>();
        addColumn2Insert(insertStatement.getTableName().getSimpleName(),insertStatement.getColumns(),insertStatement.getValuesList(),removeIndex);
        return removeIndex;
    }

    /**
     * 为insert语句添加字段
     */
    void addColumn2Insert(String tableName, List<SQLExpr> columns,List<SQLInsertStatement.ValuesClause> valuesClauses, List<Integer> removeIndex) {
        String normalizedTableName = SQLUtils.normalize(tableName);
        for (InjectColumnInfoHandler infoHandler : injectColumnInfoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.INSERT) > 0) {
                if (!infoHandler.checkTableName(normalizedTableName) || !infoHandler.checkCommandType(SqlCommandType.INSERT)) {
                    continue;
                }
                int index = -1;
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.columnAliasMap, this.isMapUnderscoreToCamelCase);
                for (int i = 0; i < columns.size(); i++) {
                    if (nameEquals(columns.get(i), columnName)) {
                        index = i;
                        break;
                    }
                }
                // 没找到
                if (index == -1) {
                    log.warn(String.format("表%s添加插入项:%s = %s", normalizedTableName, columnName, infoHandler.getValue()));
                    columns.add(new SQLIdentifierExpr(columnName));
                } else if (infoHandler.isExistOverride()) {
                    log.warn(String.format("表%s覆盖插入项:%s = %s", normalizedTableName, columnName, infoHandler.getValue()));
                }
                SQLExpr injectValue = infoHandler.toSQLExpr(dbType);
                for (SQLInsertStatement.ValuesClause values : valuesClauses) {
                    if (index == -1) {
                        values.addValue(injectValue);
                    } else {
                        if (infoHandler.isExistOverride()) {
                            for (int j = 0; j < values.getValues().size(); ++j) {
                                SQLExpr sqlExpr = values.getValues().get(j);
                                if (j == index) {
                                    values.getValues().set(j, injectValue);
                                    if (sqlExpr instanceof SQLVariantRefExpr) {
                                        removeIndex.add(((SQLVariantRefExpr) sqlExpr).getIndex() + 1);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    List<Integer> filterColumn2Update(SQLUpdateStatement sqlStatement) {
        List<Integer> removeIndex = new ArrayList<>();
        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        String normalizedTableName = SQLUtils.normalize(sqlStatement.getTableName().getSimpleName());
        Set<String> filterFields = new HashSet<>();
        for (ColumnFilterInfoHandler columnFilterInfoHandler : columnFilterInfoHandlers) {
            if (columnFilterInfoHandler.checkTableName(normalizedTableName) && columnFilterInfoHandler.getFilterColumns() != null) {
                for (String filterColumn : columnFilterInfoHandler.getFilterColumns()) {
                    filterFields.add(CommonUtils.adaptePropertyName(filterColumn, this.columnAliasMap, this.isMapUnderscoreToCamelCase));
                }
            }
        }
        if (filterFields.size() > 0) {
            log.warn("从更新语句中删除列：" + String.join(",", filterFields));
            List<SQLUpdateSetItem> needRemove = new ArrayList<>();

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
        }
        return removeIndex;
    }


    /**
     * 为更新语句添加字段
     *
     * @param sqlStatement
     */
    List<Integer> addColumn2Update(SQLUpdateStatement sqlStatement,Map<String,String> alias2tableNameMap) {
        List<Integer> removeIndex = new ArrayList<>();
        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        List<SQLUpdateSetItem> addSetItem = new ArrayList<>();
        Map<String,List<SQLUpdateSetItem>> alias2setItem = new HashMap<>();
        for(SQLUpdateSetItem setItem:items){
            SQLExpr column = setItem.getColumn();
            String ower = null;
            if(column instanceof SQLPropertyExpr){
                ower = ((SQLPropertyExpr) column).getOwnerName();
            }
            String tableName = alias2tableNameMap.get(ower);
            if(tableName == null){
                throw new SqlHelperException("sqlhepler无法确定"+setItem.toString()+"更新项发生在那张表");
            }
            List<SQLUpdateSetItem> list = alias2setItem.get(ower);
            if(list == null){
                list = new ArrayList<>();
                alias2setItem.put(ower,list);
            }
            list.add(setItem);
        }

        for(String alias:alias2setItem.keySet()){
            String tableName = alias2tableNameMap.get(alias);
            if(alias2tableNameMap.containsKey(tableName)){
                tableName = alias2tableNameMap.get(tableName);
            }
            List<SQLUpdateSetItem> aliasSetItems = alias2setItem.get(alias);
            String normalizedTableName = SQLUtils.normalize(tableName);
            // 注入
            for (InjectColumnInfoHandler infoHandler : injectColumnInfoHandlers){
                if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.UPDATE) > 0) {
                    if (!infoHandler.checkTableName(normalizedTableName) || !infoHandler.checkCommandType(SqlCommandType.UPDATE)) {
                        continue;
                    }
                    boolean override = infoHandler.isExistOverride();

                    // 是否已存在相同更新列名
                    SQLUpdateSetItem sameColumnSetItem = null;
                    for(SQLUpdateSetItem item:aliasSetItems){
                        if(nameEquals(item.getColumn(),infoHandler.getColumnName())){
                            sameColumnSetItem = item;
                            break;
                        }
                    }
                    if(sameColumnSetItem==null){
                        SQLUpdateSetItem newUpdateItem = new SQLUpdateSetItem();
                        SQLExpr newColumn = alias==null?new SQLIdentifierExpr(infoHandler.getColumnName()):new SQLPropertyExpr(new SQLIdentifierExpr(alias),infoHandler.getColumnName());
                        newUpdateItem.setColumn(newColumn);
                        newUpdateItem.setValue(infoHandler.toSQLExpr(dbType));
                        log.warn(String.format("新增更新列：%s", newUpdateItem.toString()));
                        addSetItem.add(newUpdateItem);
                    }else if(override){
                        if(sameColumnSetItem.getValue() instanceof SQLVariantRefExpr) {
                            removeIndex.add(((SQLVariantRefExpr) sameColumnSetItem.getValue()).getIndex() + 1);
                        }
                        sameColumnSetItem.setValue(infoHandler.toSQLExpr(dbType));
                        log.warn(String.format("覆盖更新列：%s", sameColumnSetItem.toString()));

                    }
                }
            }
        }
        items.addAll(addSetItem);
        return removeIndex;
    }


    /**
     * 为查询语句添加附加条件（包括where中的子查询、from的子查询、以及表连接添加过滤条件）
     *
     * @param select
     * @param commandType
     */
    void addCondition2Select(SQLObject select, SqlCommandType commandType) {
        if (select == null) {
            return;
        }
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

    void addCondition2TableSource(Object from,SQLTableSource tableSource, SqlCommandType commandType){
        addCondition2TableSource(from,tableSource,commandType,null,null);
    }
    void addCondition2TableSource(Object from,SQLTableSource tableSource, SqlCommandType commandType,Map<String,String> alias2normalizeTableNameMapAcceptor,List<String> normalizeTableNameAcceptor){
        if (from == null || tableSource == null) {
            return;
        }
        SQLExpr originCondition = getWhere(from);
        if (tableSource instanceof SQLExprTableSource) {
            String tableName = ((SQLExprTableSource) tableSource).getTableName();
            String alias = tableSource.getAlias();
            if(normalizeTableNameAcceptor!=null){
                normalizeTableNameAcceptor.add(SQLUtils.normalize(tableName, dbType));
            }
            if(alias2normalizeTableNameMapAcceptor!=null){
                String normalizeTableName = SQLUtils.normalize(tableName, dbType);
                alias2normalizeTableNameMapAcceptor.put(alias,normalizeTableName);
                alias2normalizeTableNameMapAcceptor.put(normalizeTableName,normalizeTableName);
            }

            addCondition2QueryInWhere(originCondition, commandType);
            addCondition(tableName, alias, from, commandType);
        } else if (tableSource instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinObject = (SQLJoinTableSource) tableSource;
            SQLTableSource left = joinObject.getLeft();
            SQLTableSource right = joinObject.getRight();
            SQLJoinTableSource.JoinType joinType = joinObject.getJoinType();
            // 处理左外连接添加condition的位置
            if (left instanceof SQLExprTableSource && (joinType == RIGHT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)) {
                String tableName = ((SQLExprTableSource) left).getTableName();
                if(normalizeTableNameAcceptor!=null){
                    normalizeTableNameAcceptor.add(SQLUtils.normalize(tableName, dbType));
                }
                if(alias2normalizeTableNameMapAcceptor!=null){
                    String normalizeTableName = SQLUtils.normalize(tableName, dbType);
                    alias2normalizeTableNameMapAcceptor.put(left.getAlias(),normalizeTableName);
                    alias2normalizeTableNameMapAcceptor.put(normalizeTableName,normalizeTableName);
                }
                addCondition(tableName, left.getAlias(), joinObject, commandType);
            } else {
                addCondition2TableSource(from, left, commandType,alias2normalizeTableNameMapAcceptor,normalizeTableNameAcceptor);
            }
            // 处理右外连接添加condition的位置
            if (right instanceof SQLExprTableSource && (joinType == LEFT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)) {
                String tableName = ((SQLExprTableSource) right).getTableName();
                if(normalizeTableNameAcceptor!=null){
                    normalizeTableNameAcceptor.add(SQLUtils.normalize(tableName, dbType));
                }
                if(alias2normalizeTableNameMapAcceptor!=null){
                    String normalizeTableName = SQLUtils.normalize(tableName, dbType);
                    alias2normalizeTableNameMapAcceptor.put(right.getAlias(),normalizeTableName);
                    alias2normalizeTableNameMapAcceptor.put(normalizeTableName,normalizeTableName);
                }
                addCondition(tableName, right.getAlias(), joinObject, commandType);
            } else {
                addCondition2TableSource(from, right, commandType,alias2normalizeTableNameMapAcceptor,normalizeTableNameAcceptor);
            }
        } else if (tableSource instanceof SQLSubqueryTableSource) {
            SQLSelect subSelectObject = ((SQLSubqueryTableSource) tableSource).getSelect();
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addCondition2TableSource(subQueryObject, subQueryObject.getFrom(), commandType);
        } else if (tableSource instanceof SQLUnionQueryTableSource) {
            // 支持union 查询
            SQLUnionQueryTableSource sqlUnionQueryTableSource = (SQLUnionQueryTableSource) tableSource;
            SQLSelectQueryBlock left = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getLeft();
            SQLSelectQueryBlock right = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getRight();
            addCondition2TableSource(left, left.getFrom(), commandType);
            addCondition2TableSource(right, right.getFrom(), commandType);
        } else {
            throw new SqlHelperException("不支持的sql,请排除，或者联系作者添加支持。" + tableSource.toString());
        }
    }


    /**
     * Cache new condition.
     *
     * @param target    the target
     * @param condition the condition
     */
    void cacheNewCondition(Object target,SQLExpr condition){
        LinkedHashSet<SQLExpr> sets = conditionAddedMap.getOrDefault(target,new LinkedHashSet<>());
        sets.add(condition);
        conditionAddedMap.put(target,sets);
    }


    /**
     * Add all cached condition.
     */
    void addAllCachedCondition(){
        if(conditionAddedMap.size()>0){
            Set<SQLExpr>[] sets = new Set[conditionAddedMap.size()];
            Object[] targets = new Object[conditionAddedMap.size()];
            int i = 0;
            for(Object o:conditionAddedMap.keySet()){
                Set<SQLExpr> conditions =conditionAddedMap.get(o);
                sets[i] = conditions;
                targets[i] = o;
                ++i;
            }
            for(int j = 0;j<i;++j){
                for(SQLExpr condition:sets[j]){
                    addCondition(targets[j],condition);
                }
            }
        }
    }

    /**
     * 为单独一个查询块添加附加条件
     *
     * @param queryBody
     * @param fromBody
     */
    void addCondition2Query(SQLSelectQueryBlock queryBody, SQLTableSource fromBody, SqlCommandType commandType) {
        addCondition2TableSource(queryBody,fromBody,commandType);
    }

    /**
     *
     * @param tableName       the table name
     * @param tableAlias      the table alias
     * @return the sql expr
     */
    void addCondition(String tableName, String tableAlias, Object target, SqlCommandType commandType) {
        String normalizeTableName = SQLUtils.normalize(tableName, dbType);
        for (InjectColumnInfoHandler infoHandler : injectColumnInfoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.CONDITION) > 0) {
                if (!infoHandler.checkTableName(normalizeTableName) || !infoHandler.checkCommandType(commandType)) {
                    continue;
                }
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.columnAliasMap, this.isMapUnderscoreToCamelCase);
                SQLExpr condition = null;

                if (infoHandler instanceof ConditionInjectInfo) {
                    condition = ((ConditionInjectInfo) infoHandler).toConditionSQLExpr(tableAlias, dbType, columnAliasMap, isMapUnderscoreToCamelCase);
                } else {
                    String aliasFieldName = CommonUtils.isEmpty(tableAlias) ? tableName + "." + columnName : tableAlias + "." + columnName;
                    condition = new SQLBinaryOpExpr(new SQLIdentifierExpr(aliasFieldName), infoHandler.toSQLExpr(dbType), CommonUtils.convert(infoHandler.op()));

                }
                log.warn(String.format("表%s添加过滤条件：%s", tableName, condition.toString()));
                cacheNewCondition(target,condition);
            }
        }
    }

    /**
     * 判断
     *
     * @param column
     * @param columnName
     * @return
     */
    boolean nameEquals(SQLExpr column, String columnName) {
        if (column instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) column).nameEquals(columnName);
        } else if (column instanceof SQLPropertyExpr) {
            return ((SQLPropertyExpr) column).nameEquals(columnName);
        }
        return false;
    }

    /**
     * Get where sql expr.
     *
     * @param o the o
     * @return the sql expr
     */
    static SQLExpr getWhere(Object o){
        if(o instanceof SQLSelectQueryBlock){
            return ((SQLSelectQueryBlock) o).getWhere();
        }
        if(o instanceof SQLUpdateStatement){
            return ((SQLUpdateStatement) o).getWhere();
        }
        if(o instanceof SQLDeleteStatement){
            return ((SQLDeleteStatement) o).getWhere();
        }
        throw new SqlHelperException("错误的方法调用getWhere()");
    }

    /**
     * Set where.
     *
     * @param o     the o
     * @param where the where
     */
    static void setWhere(Object o,SQLExpr where){
        if(o instanceof SQLSelectQueryBlock){
            ((SQLSelectQueryBlock) o).setWhere(where);
        }
        if(o instanceof SQLUpdateStatement){
            ((SQLUpdateStatement) o).setWhere(where);
        }
        if(o instanceof SQLDeleteStatement){
            ((SQLDeleteStatement) o).setWhere(where);
        }
    }
    /**
     * Add condition.
     *
     * @param o         the o
     * @param condition the condition
     */
    static void addCondition(Object o,SQLExpr condition){
        if(o instanceof SQLSelectQueryBlock){
            SQLExpr oldCondition = ((SQLSelectQueryBlock) o).getWhere();
            ((SQLSelectQueryBlock) o).setWhere(oldCondition==null?condition:SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, oldCondition));
        }
        if(o instanceof SQLUpdateStatement){
            SQLExpr oldCondition = ((SQLUpdateStatement) o).getWhere();
            ((SQLUpdateStatement) o).setWhere(oldCondition==null?condition:SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, oldCondition));
        }
        if(o instanceof SQLDeleteStatement){
            SQLExpr oldCondition = ((SQLDeleteStatement) o).getWhere();
            ((SQLDeleteStatement) o).setWhere(oldCondition==null?condition:SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, oldCondition));
        }
        if(o instanceof SQLJoinTableSource){
            SQLExpr oldCondition = ((SQLJoinTableSource) o).getCondition();
            ((SQLJoinTableSource) o).setCondition(oldCondition==null?condition:SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, oldCondition));

        }
    }
    static String getColumnName(SQLUpdateSetItem setItem){
        SQLExpr column = setItem.getColumn();
        if (column instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) column).getName();
        } else if (column instanceof SQLPropertyExpr) {
            return ((SQLIdentifierExpr) column).getName();
        }
        throw new SqlHelperException("不支持的update set item："+setItem.toString());
    }



    /**
     * Get schema stat visitor schema stat visitor.
     *
     * @param dbType the db type
     * @return the schema stat visitor
     */
    static SchemaStatVisitor getSchemaStatVisitor(DbType dbType){
        switch (dbType){
            case postgresql:return new PGSchemaStatVisitor();
            case mysql:return new MySqlSchemaStatVisitor();
            case oracle:return new OracleSchemaStatVisitor();
            case db2:return new DB2SchemaStatVisitor();
            case hive:return new HiveSchemaStatVisitor();
            case sqlserver:return new SQLServerSchemaStatVisitor();
            case h2:return new H2SchemaStatVisitor();
            case odps:return new OdpsSchemaStatVisitor();
            case phoenix:return new PhoenixSchemaStatVisitor();
            case antspark:return new AntsparkSchemaStatVisitor();
        }
        return new SchemaStatVisitor(dbType);
    }

}
