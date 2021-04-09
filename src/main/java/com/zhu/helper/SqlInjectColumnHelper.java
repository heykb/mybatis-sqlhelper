package com.zhu.helper;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.util.StringUtils;
import com.zhu.config.SqlHelperException;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.LogicDeleteInfoHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * The type Sql utils.
 *
 * @author heykb
 */
public class SqlInjectColumnHelper {

    private DbType dbType;

    private Collection<InjectColumnInfoHandler> infoHandlers;

    private LogicDeleteInfoHandler logicDeleteInfoHandler;

    /**
     * Instantiates a new Sql inject column helper.
     *
     * @param infoHandlers the inject items
     */
    public SqlInjectColumnHelper(DbType dbType,Collection<InjectColumnInfoHandler> infoHandlers) {
        if(infoHandlers == null || infoHandlers.size()==0){
            throw new IllegalArgumentException("参数不能为null或empty");
        }
        this.dbType = dbType;
        this.infoHandlers = infoHandlers;
        for(InjectColumnInfoHandler injectColumnInfoHandler:infoHandlers){
            if(injectColumnInfoHandler instanceof LogicDeleteInfoHandler){
                logicDeleteInfoHandler = (LogicDeleteInfoHandler) injectColumnInfoHandler;
                break;
            }
        }
    }

    private void addSelectItem2Query(SQLSelectQueryBlock queryBody){
        List<SQLSelectItem> sqlSelectItems = queryBody.getSelectList();
        List<SQLSelectItem> addItems = new ArrayList<>();
        for(SQLSelectItem sqlSelectItem:sqlSelectItems){
            if(sqlSelectItem.getExpr() instanceof SQLPropertyExpr){
                SQLPropertyExpr expr = (SQLPropertyExpr) sqlSelectItem.getExpr();
                if(!"*".equals(expr.getName())){
                    SQLSelectItem item= sqlSelectItem.clone();
                    SQLPropertyExpr addExpr = (SQLPropertyExpr) item.getExpr();
                    addExpr.setName("tenant_id");
                    addItems.add(item);
                }
            }
        }
        sqlSelectItems.addAll(addItems);
    }
    /**
     * 为sql语句中的注入字段
     *
     * @param sql    the sql
     * @return the string
     */
    public String injectSql(String sql){
        List<SQLStatement> statementList = SQLUtils.parseStatements(sql, this.dbType);
        System.out.println(SQLUtils.toSQLString(statementList, this.dbType));
        int i = 0;
        for(SQLStatement sqlStatement:statementList){
            if (sqlStatement instanceof SQLSelectStatement) {
                SQLSelectQueryBlock queryObject = (SQLSelectQueryBlock) ((SQLSelectStatement) sqlStatement).getSelect().getQuery();
                addSelectItem2Query(queryObject);
                addCondition2Query(queryObject, queryObject.getFrom());
            } else if (sqlStatement instanceof SQLUpdateStatement) {
                // 为更新语句中的查询语句添加附加条件
                SQLUpdateStatement updateStatement = (SQLUpdateStatement) sqlStatement;
                SQLExpr where = updateStatement.getWhere();
                addCondition2QueryInWhere(where);
                SQLExpr newCondition = newEqualityCondition(updateStatement.getTableName().getSimpleName(),
                        updateStatement.getTableSource().getAlias(), where);
                updateStatement.setWhere(newCondition);

                addColumn2Update(updateStatement);
            } else if (sqlStatement instanceof SQLDeleteStatement) {
                // 为删除语句中的查询语句添加附加条件
                SQLDeleteStatement deleteStatement = (SQLDeleteStatement) sqlStatement;
                SQLExpr where = deleteStatement.getWhere();
                addCondition2QueryInWhere(where);
                SQLExpr newCondition = newEqualityCondition(deleteStatement.getTableName().getSimpleName(),
                        deleteStatement.getTableSource().getAlias(), where);
                deleteStatement.setWhere(newCondition);
                statementList.set(i,toLogicDeleteSql(deleteStatement));
            } else if (sqlStatement instanceof SQLInsertStatement) {
                // 为插入语句中的查询语句添加附加条件
                SQLInsertStatement insertStatement = (SQLInsertStatement) sqlStatement;
                SQLSelect sqlSelect = insertStatement.getQuery();
                if (sqlSelect != null) {
                    SQLSelectQueryBlock selectQueryBlock = (SQLSelectQueryBlock) sqlSelect.getQuery();
                    addCondition2Query(selectQueryBlock, selectQueryBlock.getFrom());
                }

                addColumn2Insert(insertStatement);
            }
            ++i;
        }
        return SQLUtils.toSQLString(statementList, this.dbType);
    }


    /**
     * 为出现在where中的子查询 附件条件
     *
     * @param expr       the where
     */
    private void addCondition2QueryInWhere(SQLExpr expr) {
        if (expr instanceof SQLInSubQueryExpr) {
            SQLInSubQueryExpr inWhere = (SQLInSubQueryExpr) expr;
            SQLSelect subSelectObject = inWhere.getSubQuery();
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addCondition2Query(subQueryObject, subQueryObject.getFrom());
        } else if (expr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr opExpr = (SQLBinaryOpExpr) expr;
            SQLExpr left = opExpr.getLeft();
            SQLExpr right = opExpr.getRight();
            addCondition2QueryInWhere(left);
            addCondition2QueryInWhere(right);
        } else if (expr instanceof SQLQueryExpr) {
            SQLSelectQueryBlock selectQueryBlock = (SQLSelectQueryBlock) (((SQLQueryExpr) expr).getSubQuery()).getQuery();
            addCondition2Query(selectQueryBlock, selectQueryBlock.getFrom());
        }
    }


    /**
     * 为查询语句添加附加条件（包括where中的子查询、from的子查询、以及表连接添加过滤条件）
     *
     * @param queryBody  the query body
     * @param fromBody   the from body
     */
    private void addCondition2Query(SQLSelectQueryBlock queryBody, SQLTableSource fromBody){

        SQLExpr originCondition = queryBody.getWhere();
        if (fromBody instanceof SQLExprTableSource) {
            String tableName = ((SQLIdentifierExpr) ((SQLExprTableSource) fromBody).getExpr()).getName();
            String alias = fromBody.getAlias();
            originCondition = newEqualityCondition(tableName, alias, originCondition);
            queryBody.setWhere(originCondition);
        } else if (fromBody instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinObject = (SQLJoinTableSource) fromBody;
            SQLTableSource left = joinObject.getLeft();
            SQLTableSource right = joinObject.getRight();
            addCondition2Query(queryBody, left);
            addCondition2Query(queryBody, right);
        } else if (fromBody instanceof SQLSubqueryTableSource) {
            SQLSelect subSelectObject = ((SQLSubqueryTableSource) fromBody).getSelect();
            SQLSelectQueryBlock subQueryObject = (SQLSelectQueryBlock) subSelectObject.getQuery();
            addCondition2Query(subQueryObject, subQueryObject.getFrom());
        } else {
            throw new SqlHelperException("未处理的异常");
        }
        addCondition2QueryInWhere(originCondition);
    }

    SQLStatement toLogicDeleteSql(SQLDeleteStatement sqlStatement){
        if(logicDeleteInfoHandler == null){
            return sqlStatement;
        }
        SQLStatement logicSqlStatement = SQLUtils.parseSingleStatement(logicDeleteInfoHandler.getSqlDemo(), dbType);
        if(logicSqlStatement instanceof SQLUpdateStatement){
            SQLUpdateStatement updateStatement = (SQLUpdateStatement) logicSqlStatement;
            updateStatement.setTableSource(sqlStatement.getTableSource());
            updateStatement.setWhere(sqlStatement.getWhere());
        }else{
            throw new SqlHelperException("逻辑删除sqlDemo配置错误，应该是update语句如：update xx set isDelete = false where id = xx");
        }
        return logicSqlStatement;
    }

    /**
     * 为insert语句添加字段
     * @param sqlStatement
     */
    private void addColumn2Insert(SQLInsertStatement sqlStatement){
        List<SQLExpr> columns = sqlStatement.getColumns();
        List<SQLInsertStatement.ValuesClause> valuesClauses = sqlStatement.getValuesList();
        for(InjectColumnInfoHandler infoHandler:infoHandlers){
            if((infoHandler.getInjectTypes()&InjectColumnInfoHandler.INSERT) > 0){
                int index = -1;
                // 跳过
                if(infoHandler.isExistSkip()){
                    for(int i = 0;i<columns.size();i++){
                        if(nameEquals(columns.get(i),infoHandler.getColumnName())){
                            index = i;
                            break;
                        }
                    }
                }
                // 不跳过或者没找到
                if(!infoHandler.isExistSkip() || index==-1){
                    columns.add(new SQLIdentifierExpr(infoHandler.getColumnName()));
                }
                for(SQLInsertStatement.ValuesClause values:valuesClauses){
                    // 不跳过或者没找到
                    if(!infoHandler.isExistSkip() || index==-1){
                        values.addValue(infoHandler.toSQLExpr());
                    }
                }
            }
        }
    }

    /**
     * 为更新语句添加字段
     * @param sqlStatement
     */
    private void addColumn2Update(SQLUpdateStatement sqlStatement){
        List<SQLUpdateSetItem> items = sqlStatement.getItems();
        for(InjectColumnInfoHandler infoHandler:infoHandlers){
            if((infoHandler.getInjectTypes()&InjectColumnInfoHandler.UPDATE) > 0){
                // 跳过
                if(infoHandler.isExistSkip()){
                    for(SQLUpdateSetItem item:items){
                        if(item.columnMatch(infoHandler.getColumnName())){
                            continue;
                        }
                    }
                }
                SQLUpdateSetItem sqlUpdateSetItem = new SQLUpdateSetItem();
                sqlUpdateSetItem.setColumn(new SQLIdentifierExpr(infoHandler.getColumnName()));
                sqlUpdateSetItem.setValue(infoHandler.toSQLExpr());
                items.add(sqlUpdateSetItem);
            }
        }
    }
    /**
     * 返回添加了附加条件的condition语句
     *
     * @param tableName       the table name
     * @param tableAlias      the table alias
     * @param originCondition the origin condition
     * @return the sql expr
     */
    private SQLExpr newEqualityCondition(String tableName, String tableAlias, SQLExpr originCondition) {
        SQLExpr re = originCondition;
        for(InjectColumnInfoHandler infoHandler:infoHandlers){
            if((infoHandler.getInjectTypes()&InjectColumnInfoHandler.CONDITION) > 0){
                if(infoHandler.ignoreTable(tableName)){
                    continue;
                }
                if(infoHandler.isExistSkip() && contains(originCondition,infoHandler.getColumnName())){
                    continue;
                }
                String fieldName = StringUtils.isEmpty(tableAlias) ? infoHandler.getColumnName() : tableAlias + "." + infoHandler.getColumnName();
                SQLExpr condition = new SQLBinaryOpExpr(new SQLIdentifierExpr(fieldName), infoHandler.toSQLExpr(), SQLBinaryOperator.Equality);
                re = SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, condition, false, re);
            }
        }
        return re;
    }

    /**
     * 判断
     * @param column
     * @param columnName
     * @return
     */
    private boolean nameEquals(SQLExpr column, String columnName){
        if (column instanceof SQLIdentifierExpr) {
            return ((SQLIdentifierExpr) column).nameEquals(columnName);
        } else if (column instanceof SQLPropertyExpr) {
            ((SQLPropertyExpr) column).nameEquals(columnName);
        }
        return false;
    }

    /**
     * 判断查询表达式中是否已存在字段
     * @param condition
     * @param fieldName
     * @return
     */
    private boolean contains(SQLExpr condition, String fieldName){
        boolean contains = false;
        if(condition instanceof SQLBinaryOpExpr){
            SQLExpr left = ((SQLBinaryOpExpr) condition).getLeft();
            SQLExpr right = ((SQLBinaryOpExpr) condition).getRight();
            if(left instanceof SQLPropertyExpr && ((SQLPropertyExpr) left).nameEquals(fieldName)){
                contains =  true;
            }
            return contains || contains(right,fieldName);
        }
        return false;
    }

    public static void main(String[] args) {
//        String sql = "select * from user s";
//        String sql = "select * from user s where s.name='333'";
//        String sql = "select * from (select * from tab t where id = 2 and name = 'wenshao') s where s.name='333'";
        String sql="select u.*,g.name from user u join user_group g on u.groupId=g.groupId where u.name='123'";

//        String sql = "update user set name=? where id =(select id from user s)";
//        String sql = "delete from user where id = ( select id from user s )";
//        String sql = "insert into user (id,name) values('0','heykb')";
//        String sql = "insert into user (id,name) select g.id,g.name from user_group g where id=1";
        InjectColumnInfoHandler injectColumnInfoHandler = new InjectColumnInfoHandler() {
            @Override
            public String getColumnName() {
                return "tenant_id";
            }

            @Override
            public Object getValue() {
                return "sqlhelper";
            }

            @Override
            public int getInjectTypes() {
                return CONDITION;
            }
        };
        SqlInjectColumnHelper helper = new SqlInjectColumnHelper(DbType.postgresql, Arrays.asList(injectColumnInfoHandler));
        String re = helper.injectSql(sql);
        System.out.println(re);
    }
}
