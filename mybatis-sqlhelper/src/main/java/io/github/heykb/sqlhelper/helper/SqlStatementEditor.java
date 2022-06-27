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
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGDeleteStatement;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGSchemaStatVisitor;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerSchemaStatVisitor;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
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
    private SqlStatementEditor() {
    }

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
        return new Result(sqlStatement.toString(),null);
    }

    private Result processing(OracleMultiInsertStatement sqlStatement) {
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
        return new Result(SQLUtils.toSQLString(sqlStatement), removeIndex);
    }

    Result processing(SQLInsertStatement insertStatement) {
        // 为插入语句中的查询语句添加附加条件
        SQLSelect sqlSelect = insertStatement.getQuery();
        if(sqlSelect!=null){
            addCondition2Select(sqlSelect.getQuery(), SqlCommandType.INSERT);
        }
        List<Integer> removeIndex = addColumn2Insert(insertStatement);
        return new Result(SQLUtils.toSQLString(insertStatement), removeIndex);
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

    Result processing(SQLDeleteStatement deleteStatement) {
        // 为删除语句中的查询语句添加附加条件
        SQLExpr where = deleteStatement.getWhere();
        addCondition2QueryInWhere(where, SqlCommandType.DELETE);
        Map<String,String> normalizeTableName2Alias = new HashMap<>();
        if(deleteStatement.getUsing()!=null){
            addCondition2From(deleteStatement,deleteStatement.getUsing(),SqlCommandType.DELETE,null,normalizeTableName2Alias);
        }else if(deleteStatement.getFrom()!=null){
            addCondition2From(deleteStatement,deleteStatement.getFrom(),SqlCommandType.DELETE,null,normalizeTableName2Alias);
        }else{
            addCondition2From(deleteStatement,deleteStatement.getTableSource(),SqlCommandType.DELETE,null,normalizeTableName2Alias);
        }
        SQLStatement sqlStatement = toLogicDeleteSql(deleteStatement,normalizeTableName2Alias);
        return new Result(SQLUtils.toSQLString(sqlStatement), null);
    }

    Result processing(SQLUpdateStatement updateStatement) {
        // 为where中的查询语句添加附加条件
        SQLExpr where = updateStatement.getWhere();
        addCondition2QueryInWhere(where, SqlCommandType.UPDATE);
        Map<String,String> alias2normalizeTableNameMap = new HashMap<>();
        if(updateStatement.getFrom()!=null){
            addCondition2From(updateStatement,updateStatement.getFrom(),SqlCommandType.UPDATE,alias2normalizeTableNameMap,null);
        }else{
            addCondition2From(updateStatement,updateStatement.getTableSource(),SqlCommandType.UPDATE,alias2normalizeTableNameMap,null);
        }
        List<Integer> removedParamIndex = addColumn2Update(updateStatement,alias2normalizeTableNameMap);
        removedParamIndex.addAll(filterColumn2Update(updateStatement));

        return new Result(SQLUtils.toSQLString(updateStatement), removedParamIndex);
    }

    private SQLStatement toLogicDeleteSql(SQLDeleteStatement deleteStatement,Map<String,String> normalizeTableName2Alias) {
        if(this.logicDeleteInfoHandlers.size() == 0){
            return deleteStatement;
        }
        SQLTableSource tableSource = deleteStatement.getTableSource();
        if(deleteStatement.getUsing()!=null){
            tableSource = deleteStatement.getUsing();
        }else if(deleteStatement.getFrom()!=null){
            tableSource = deleteStatement.getFrom();
        }
        List<SQLUpdateSetItem> setItems = new ArrayList<>();
        SQLUpdateStatement updateStatement = null;
        for(String normalizeTableName:normalizeTableName2Alias.keySet()){
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
                String alias = normalizeTableName2Alias.get(normalizeTableName);
                if(alias==null && normalizeTableName2Alias.size()==1){
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

        }
        updateStatement.setTableSource(tableSource);
        updateStatement.setWhere(deleteStatement.getWhere());
        updateStatement.getItems().clear();
        updateStatement.getItems().addAll(setItems);
        return updateStatement;
    }

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
    private List<Integer> addColumn2Insert(SQLInsertStatement insertStatement){
        List<Integer> removeIndex = new ArrayList<>();
        addColumn2Insert(insertStatement.getTableName().getSimpleName(),insertStatement.getColumns(),insertStatement.getValuesList(),removeIndex);
        return removeIndex;
    }

    /**
     * 为insert语句添加字段
     */
    private void addColumn2Insert(String tableName, List<SQLExpr> columns,List<SQLInsertStatement.ValuesClause> valuesClauses, List<Integer> removeIndex) {
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

    private List<Integer> filterColumn2Update(SQLUpdateStatement sqlStatement) {
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
    private List<Integer> addColumn2Update(SQLUpdateStatement sqlStatement,Map<String,String> alias2tableNameMap) {
        List<Integer> removeIndex = new ArrayList<>();
        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        List<SQLUpdateSetItem> addSetItem = new ArrayList<>();
        Map<String,List<SQLUpdateSetItem>> alias2setItem = new HashMap<>();
        for(SQLUpdateSetItem setItem:items){
            // set name= (SELECT AddressList.PostCode FROM AddressList WHERE AddressList.PersonId = Persons.PersonId)
            if(setItem.getValue() instanceof SQLQueryExpr){
                addCondition2Select(((SQLQueryExpr) setItem.getValue()).getSubQuery(),SqlCommandType.UPDATE);
            }
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
    private void addCondition2Select(SQLObject select, SqlCommandType commandType) {
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

    private void addCondition2From(Object from,SQLTableSource fromBody, SqlCommandType commandType){
        addCondition2From(from,fromBody,commandType,null,null);
    }
    private void addCondition2From(Object from,SQLTableSource fromBody, SqlCommandType commandType,Map<String,String> alias2normalizeTableNameMapAcceptor,Map<String,String> normalizeTableName2Alias){
        if (from == null || fromBody == null) {
            return;
        }
        SQLExpr originCondition = getWhere(from);
        if (fromBody instanceof SQLExprTableSource) {
            String tableName = ((SQLExprTableSource) fromBody).getTableName();
            String alias = fromBody.getAlias();
            if(alias2normalizeTableNameMapAcceptor!=null || normalizeTableName2Alias!=null){
                String normalizeTableName = SQLUtils.normalize(tableName, dbType);
                if(alias2normalizeTableNameMapAcceptor!=null){
                    alias2normalizeTableNameMapAcceptor.put(alias,normalizeTableName);
                    alias2normalizeTableNameMapAcceptor.put(normalizeTableName,normalizeTableName);
                }else{
                    normalizeTableName2Alias.put(normalizeTableName,alias);
                }
            }
            addCondition2QueryInWhere(originCondition, commandType);
            originCondition = newEqualityCondition(tableName, alias, originCondition, commandType);
            setWhere(from,originCondition);
        } else if (fromBody instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinObject = (SQLJoinTableSource) fromBody;
            SQLTableSource left = joinObject.getLeft();
            SQLTableSource right = joinObject.getRight();
            SQLExpr onCondition = joinObject.getCondition();
            SQLJoinTableSource.JoinType joinType = joinObject.getJoinType();
            // 处理左外连接添加condition的位置
            if (left instanceof SQLExprTableSource && (joinType == RIGHT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)) {
                String tableName = ((SQLExprTableSource) left).getTableName();
                if(alias2normalizeTableNameMapAcceptor!=null || normalizeTableName2Alias!=null){
                    String normalizeTableName = SQLUtils.normalize(tableName, dbType);
                    if(alias2normalizeTableNameMapAcceptor!=null){
                        alias2normalizeTableNameMapAcceptor.put(left.getAlias(),normalizeTableName);
                        alias2normalizeTableNameMapAcceptor.put(normalizeTableName,normalizeTableName);
                    }else{
                        normalizeTableName2Alias.put(normalizeTableName,left.getAlias());
                    }
                }
                onCondition = newEqualityCondition(tableName, left.getAlias(), onCondition, commandType);
            } else {
                addCondition2From(from, left, commandType,alias2normalizeTableNameMapAcceptor,normalizeTableName2Alias);
            }
            // 处理右外连接添加condition的位置
            if (right instanceof SQLExprTableSource && (joinType == LEFT_OUTER_JOIN || joinType == FULL_OUTER_JOIN)) {
                String tableName = ((SQLExprTableSource) right).getTableName();
                if(alias2normalizeTableNameMapAcceptor!=null || normalizeTableName2Alias!=null){
                    String normalizeTableName = SQLUtils.normalize(tableName, dbType);
                    if(alias2normalizeTableNameMapAcceptor!=null){
                        alias2normalizeTableNameMapAcceptor.put(right.getAlias(),normalizeTableName);
                        alias2normalizeTableNameMapAcceptor.put(normalizeTableName,normalizeTableName);
                    }else{
                        normalizeTableName2Alias.put(normalizeTableName,right.getAlias());
                    }
                }
                onCondition = newEqualityCondition(tableName, right.getAlias(), onCondition, commandType);
            } else {
                addCondition2From(from, right, commandType,alias2normalizeTableNameMapAcceptor,normalizeTableName2Alias);
            }
            joinObject.setCondition(onCondition);
        } else if (fromBody instanceof SQLSubqueryTableSource) {
            SQLSelect subSelectObject = ((SQLSubqueryTableSource) fromBody).getSelect();
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addCondition2From(subQueryObject, subQueryObject.getFrom(), commandType);
        } else if (fromBody instanceof SQLUnionQueryTableSource) {
            // 支持union 查询
            SQLUnionQueryTableSource sqlUnionQueryTableSource = (SQLUnionQueryTableSource) fromBody;
            SQLSelectQueryBlock left = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getLeft();
            SQLSelectQueryBlock right = (SQLSelectQueryBlock) sqlUnionQueryTableSource.getUnion().getRight();
            addCondition2From(left, left.getFrom(), commandType);
            addCondition2From(right, right.getFrom(), commandType);
        } else {
            throw new SqlHelperException("不支持的sql,请排除，或者联系作者添加支持。" + fromBody.toString());
        }
    }

    SQLExpr getWhere(Object o){
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
    void setWhere(Object o,SQLExpr where){
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
     * 为单独一个查询块添加附加条件
     *
     * @param queryBody
     * @param fromBody
     */
    private void addCondition2Query(SQLSelectQueryBlock queryBody, SQLTableSource fromBody, SqlCommandType commandType) {
        addCondition2From(queryBody,fromBody,commandType);
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
        String normalizeTableName = SQLUtils.normalize(tableName, dbType);
        for (InjectColumnInfoHandler infoHandler : injectColumnInfoHandlers) {
            if ((infoHandler.getInjectTypes() & InjectColumnInfoHandler.CONDITION) > 0) {
                if (!infoHandler.checkTableName(normalizeTableName) || !infoHandler.checkCommandType(commandType)) {
                    continue;
                }
                String columnName = CommonUtils.adaptePropertyName(infoHandler.getColumnName(), this.columnAliasMap, this.isMapUnderscoreToCamelCase);
//                if (columnName != null && contains(originCondition, columnName)) {
//                    continue;
//                }
                SQLExpr condition = null;

                if (infoHandler instanceof ConditionInjectInfo) {
                    condition = ((ConditionInjectInfo) infoHandler).toConditionSQLExpr(tableAlias, dbType, columnAliasMap, isMapUnderscoreToCamelCase);
                } else {
                    String aliasFieldName = CommonUtils.isEmpty(tableAlias) ? tableName + "." + columnName : tableAlias + "." + columnName;
                    condition = new SQLBinaryOpExpr(new SQLIdentifierExpr(aliasFieldName), infoHandler.toSQLExpr(dbType), CommonUtils.convert(infoHandler.op()));

                }
                if(contains(re,condition)){
                    continue;
                }

                log.warn(String.format("表%s添加过滤条件：%s", tableName, condition.toString()));
                re = re == null ? condition : SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, re);
            }
        }
        return re;
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
            return ((SQLPropertyExpr) column).nameEquals(columnName);
        }
        return false;
    }

    private String getColumnName(SQLUpdateSetItem setItem){
        SQLExpr column = setItem.getColumn();
        if (column instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) column).getName();
        } else if (column instanceof SQLPropertyExpr) {
            return ((SQLIdentifierExpr) column).getName();
        }
        throw new SqlHelperException("不支持的update set item："+setItem.toString());
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
    private boolean contains(SQLExpr condition, SQLExpr target) {
        boolean contains = false;
        if (condition instanceof SQLBinaryOpExpr) {
            SQLExpr left = ((SQLBinaryOpExpr) condition).getLeft();
            SQLExpr right = ((SQLBinaryOpExpr) condition).getRight();
            if(left instanceof SQLBinaryOpExpr){
                contains = contains || contains(left,target);
                if(contains == true){
                    return true;
                }
            }
            if(right instanceof SQLBinaryOpExpr){
                contains = contains || contains(right,target);
                if(contains == true){
                    return true;
                }
            }
            return target.equals(condition);
        }
        return false;
    }

    public static class Builder {
        private SqlStatementEditor sqlStatementEditor;
        private String sql;

        public Builder(String sql, DbType dbType) {
            this.sqlStatementEditor = new SqlStatementEditor();
            this.sql = sql;
            this.sqlStatementEditor.dbType = dbType;
        }

        public SqlStatementEditor.Builder injectColumnInfoHandlers(Collection<InjectColumnInfoHandler> injectColumnInfoHandlers) {
            this.sqlStatementEditor.injectColumnInfoHandlers = injectColumnInfoHandlers;
            return this;
        }

        public SqlStatementEditor.Builder columnFilterInfoHandlers(Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers) {
            this.sqlStatementEditor.columnFilterInfoHandlers = columnFilterInfoHandlers;
            return this;
        }

        public SqlStatementEditor.Builder columnAliasMap(Map<String, String> columnAliasMap) {
            this.sqlStatementEditor.columnAliasMap = columnAliasMap;
            return this;
        }

        public SqlStatementEditor.Builder isMapUnderscoreToCamelCase(boolean isMapUnderscoreToCamelCase) {
            this.sqlStatementEditor.isMapUnderscoreToCamelCase = isMapUnderscoreToCamelCase;
            return this;
        }

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
