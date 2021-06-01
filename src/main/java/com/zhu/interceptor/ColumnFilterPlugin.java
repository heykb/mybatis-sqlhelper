package com.zhu.interceptor;

import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import com.zhu.utils.CommonUtils;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;


@Intercepts({ @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = { Statement.class }) })
public class ColumnFilterPlugin implements Interceptor {
    private Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    private DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler;
    public ColumnFilterPlugin(Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers) {
        this.columnFilterInfoHandlers = columnFilterInfoHandlers;
    }

    public void setColumnFilterInfoHandlers(Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers) {
        this.columnFilterInfoHandlers = columnFilterInfoHandlers;
    }

    public void setDynamicFindColumnFilterHandler(DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler) {
        this.dynamicFindColumnFilterHandler = dynamicFindColumnFilterHandler;
    }

    public ColumnFilterPlugin() {
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object re = invocation.proceed();
        Object[] args = invocation.getArgs();
        Statement statement = (Statement) args[0];
        ResultSet rs = statement.getResultSet();
        Object target = invocation.getTarget();
        while (target.getClass().getName().startsWith("com.sun.proxy.")){
            target = Proxy.getInvocationHandler(target);
        }
        if(target instanceof Plugin){
            target =SystemMetaObject.forObject(target).getValue("target");
        }
        if(target instanceof DefaultResultSetHandler){
            DefaultResultSetHandler defaultResultSetHandler = (DefaultResultSetHandler) target;
            MetaObject metaStatementHandler = SystemMetaObject.forObject(defaultResultSetHandler);
            MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("mappedStatement");
            if(mappedStatement.getSqlCommandType()== SqlCommandType.SELECT){
                Set<String> ignoreColumns = columnFilterInfoHandlers.stream().filter(columnFilterInfoHandler->columnFilterInfoHandler.checkMapperId(mappedStatement.getId()))
                        .flatMap(columnFilterInfoHandler->columnFilterInfoHandler.getFilterColumns().stream()).collect(Collectors.toSet());
                if(dynamicFindColumnFilterHandler!=null){
                    List<ColumnFilterInfoHandler> dynamicHandlers = dynamicFindColumnFilterHandler.findColumnFilterHandlers();
                    if(!CollectionUtils.isEmpty(dynamicHandlers)){
                        ignoreColumns.addAll(dynamicHandlers.stream().filter(columnFilterInfoHandler->columnFilterInfoHandler.checkMapperId(mappedStatement.getId()))
                                .flatMap(columnFilterInfoHandler->columnFilterInfoHandler.getFilterColumns().stream()).collect(Collectors.toSet()));
                    }
                }
                filterColumns(re,ignoreColumns);
            }
        }
        return re;
    }

    private void filterColumns(Object o,Set<String> ignoreColumns){
        if(o==null || CollectionUtils.isEmpty(ignoreColumns)){
            return ;
        }
        if(CommonUtils.isPrimitiveOrWrap(o.getClass())){
            return ;
        }else if(o instanceof String){
            return ;
        }else if(o.getClass().isArray()){
            int length = Array.getLength(o);
            for (int i = 0; i < length; i++) {
                filterColumns(Array.get(o, i),ignoreColumns);
            }
        }else if(Collection.class.isAssignableFrom(o.getClass())){
            for(Object item:(Collection)o){
                filterColumns(item,ignoreColumns);
            }
        }else if(Map.class.isAssignableFrom(o.getClass())){
            List<String> removeKeys = new ArrayList<>();
            Map<String,Object> map = (Map<String,Object>)o;
            for(String key:map.keySet()){
                if(ignoreColumns.contains(key)){
                    removeKeys.add(key);
                }
            }
            for(String key:removeKeys){
                map.remove(key);
            }
        }else{
            Class clazz = o.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for(Field field:fields){
                field.setAccessible(true);
                if(ignoreColumns.contains(field.getName())){
                    try {
                        field.set(o,null);
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target,this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

}
