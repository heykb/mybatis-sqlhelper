package com.zhu.interceptor;

import com.alibaba.druid.DbType;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.helper.SqlInjectColumnHelper;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 字段注入插件
 *
 **/
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class }) })
public class InjectColumnPlugin implements Interceptor {
    DbType dbtype;
    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;

    public InjectColumnPlugin(DbType dbtype, Collection<InjectColumnInfoHandler> injectColumnInfoHandlers) {
        this.injectColumnInfoHandlers = injectColumnInfoHandlers;
        this.dbtype = dbtype;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        Connection con = (Connection) args[0];
        con.getCatalog();
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        BoundSql obj = statementHandler.getBoundSql();
        Collection<InjectColumnInfoHandler> handlers = this.injectColumnInfoHandlers.stream().filter(handler->!handler.ignoreMapperId(mappedStatement.getId())).collect(Collectors.toList());
        if(handlers.size()>0){
            SqlInjectColumnHelper sqlInjectColumnHelper = new SqlInjectColumnHelper(dbtype,handlers);
            //通过反射修改sql语句
            String sql = obj.getSql();
            metaObject.setValue("delegate.boundSql.sql", sqlInjectColumnHelper.injectSql(sql));
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

}