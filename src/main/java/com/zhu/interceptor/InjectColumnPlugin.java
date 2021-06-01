package com.zhu.interceptor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.util.StringUtils;
import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import com.zhu.handler.dynamic.DynamicFindInjectInfoHandler;
import com.zhu.helper.Configuration;
import com.zhu.helper.SqlInjectColumnHelper;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段注入插件
 *
 **/
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class }) })
public class InjectColumnPlugin implements Interceptor {
    private DbType dbtype;
    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;
    private Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    private String tbAliasPrefix;
    private DynamicFindInjectInfoHandler dynamicFindInjectInfoHandler;
    private DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler;
    public InjectColumnPlugin(DbType dbtype, Collection<InjectColumnInfoHandler> injectColumnInfoHandlers) {
        this.injectColumnInfoHandlers = injectColumnInfoHandlers;
        this.dbtype = dbtype;
    }

    public void setColumnFilterInfoHandlers(Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers) {
        this.columnFilterInfoHandlers = columnFilterInfoHandlers;
    }

    public void setDynamicFindInjectInfoHandler(DynamicFindInjectInfoHandler dynamicFindInjectInfoHandler) {
        this.dynamicFindInjectInfoHandler = dynamicFindInjectInfoHandler;
    }

    public void setDynamicFindColumnFilterHandler(DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler) {
        this.dynamicFindColumnFilterHandler = dynamicFindColumnFilterHandler;
    }

    public void setTbAliasPrefix(String tbAliasPrefix) {
        this.tbAliasPrefix = tbAliasPrefix;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        Connection con = (Connection) args[0];
        con.getCatalog();
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        boolean isMapUnderscoreToCamelCase = mappedStatement.getConfiguration().isMapUnderscoreToCamelCase();
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        Map<String,String> resultPropertiesMap = new HashMap<>();
        for(ResultMap resultMap:resultMaps){
            List<ResultMapping> resultMappings = resultMap.getPropertyResultMappings();
            resultPropertiesMap.putAll(resultMappings.stream().collect(Collectors.toMap(ResultMapping::getProperty,ResultMapping::getColumn,(v1,v2)->v2)));
        }
        Configuration configuration = new Configuration(resultPropertiesMap,isMapUnderscoreToCamelCase);
        BoundSql obj = statementHandler.getBoundSql();
        String sql = obj.getSql();

        //过滤InjectColumnInfoHandler
        List<InjectColumnInfoHandler> handlers = this.injectColumnInfoHandlers.stream().filter(handler->handler.checkMapperId(mappedStatement.getId())).collect(Collectors.toList());
        if(dynamicFindInjectInfoHandler!=null){
            List<InjectColumnInfoHandler> dynamicHandlers = dynamicFindInjectInfoHandler.findInjectInfoHandlers();
            if(!CollectionUtils.isEmpty(dynamicHandlers)){
                handlers.addAll(dynamicHandlers.stream().filter(handler->!handler.checkMapperId(mappedStatement.getId())).collect(Collectors.toList()));
            }
        }
        // 获取有无查询列过滤
        Set<String> filterColumns = null;
        if(mappedStatement.getSqlCommandType()== SqlCommandType.SELECT && columnFilterInfoHandlers!=null){
            filterColumns = columnFilterInfoHandlers.stream().filter(columnFilterInfoHandler->columnFilterInfoHandler.checkMapperId(mappedStatement.getId()))
                    .flatMap(columnFilterInfoHandler->columnFilterInfoHandler.getFilterColumns().stream()).collect(Collectors.toSet());
            if(dynamicFindColumnFilterHandler!=null){
                List<ColumnFilterInfoHandler> dynamicHandlers = dynamicFindColumnFilterHandler.findColumnFilterHandlers();
                if(!CollectionUtils.isEmpty(dynamicHandlers)){
                    filterColumns.addAll(dynamicHandlers.stream().filter(columnFilterInfoHandler->columnFilterInfoHandler.checkMapperId(mappedStatement.getId()))
                            .flatMap(columnFilterInfoHandler->columnFilterInfoHandler.getFilterColumns().stream()).collect(Collectors.toSet()));
                }
            }
        }

        // 处理sql
        if(handlers.size()>0){
            SqlInjectColumnHelper sqlInjectColumnHelper = new SqlInjectColumnHelper(dbtype,handlers,this.tbAliasPrefix);
            sqlInjectColumnHelper.setConfiguration(configuration);
            metaObject.setValue("delegate.boundSql.sql", sqlInjectColumnHelper.handlerSql(sql,filterColumns));
        }

        return invocation.proceed();
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

}