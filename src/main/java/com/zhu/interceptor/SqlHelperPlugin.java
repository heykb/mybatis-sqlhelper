package com.zhu.interceptor;

import com.alibaba.druid.DbType;
import com.zhu.config.SqlHelperAutoDbType;
import com.zhu.handler.ColumnFilterInfoHandler;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.handler.dynamic.DynamicFindColumnFilterHandler;
import com.zhu.handler.dynamic.DynamicFindInjectInfoHandler;
import com.zhu.helper.Configuration;
import com.zhu.helper.SqlInjectColumnHelper;
import com.zhu.utils.CommonUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段注入插件
 *
 **/
@Intercepts(
    {
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
    }
)
public class SqlHelperPlugin implements Interceptor {

    private static final Log log = LogFactory.getLog(SqlHelperPlugin.class);
    private DbType dbtype;
    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;
    private Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    private String tbAliasPrefix;
    private DynamicFindInjectInfoHandler dynamicFindInjectInfoHandler;
    private DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler;
    public SqlHelperPlugin(DbType dbtype, Collection<InjectColumnInfoHandler> injectColumnInfoHandlers) {
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
        Method method = invocation.getMethod();
        Executor executor = (Executor) invocation.getTarget();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];

        BoundSql boundSql = null;
        boolean isQuery = "query".equals(method.getName());
        //由于逻辑关系，只会进入一次
        if(isQuery && args.length==6){
            boundSql = (BoundSql) args[5];
        }else{
            boundSql = mappedStatement.getBoundSql(parameter);
        }
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        Map<String,String> resultPropertiesMap = new HashMap<>();
        for(ResultMap resultMap:resultMaps){
            List<ResultMapping> resultMappings = resultMap.getPropertyResultMappings();
            resultPropertiesMap.putAll(resultMappings.stream().collect(Collectors.toMap(ResultMapping::getProperty,ResultMapping::getColumn,(v1,v2)->v2)));
        }
        Configuration configuration = new Configuration(resultPropertiesMap,mappedStatement.getConfiguration().isMapUnderscoreToCamelCase());
        String mapperId = mappedStatement.getId();
        //过滤InjectColumnInfoHandler
        List<InjectColumnInfoHandler> handlers = this.injectColumnInfoHandlers.stream().filter(handler->handler.checkMapperId(mapperId)).collect(Collectors.toList());
        if(dynamicFindInjectInfoHandler!=null && dynamicFindInjectInfoHandler.checkMapperIds(mapperId)){
            List<InjectColumnInfoHandler> dynamicHandlers = dynamicFindInjectInfoHandler.findInjectInfoHandlers();
            if(!CollectionUtils.isEmpty(dynamicHandlers)){
                handlers.addAll(dynamicHandlers.stream().filter(handler->handler.checkMapperId(mapperId)).collect(Collectors.toList()));
            }
        }
        // 获取有无查询列过滤
        Set<String> filterColumns = null;
        if(mappedStatement.getSqlCommandType()== SqlCommandType.SELECT || mappedStatement.getSqlCommandType() == SqlCommandType.UPDATE){
            if(columnFilterInfoHandlers!=null ){
                filterColumns = columnFilterInfoHandlers.stream().filter(columnFilterInfoHandler->columnFilterInfoHandler.checkMapperId(mapperId))
                        .flatMap(columnFilterInfoHandler->columnFilterInfoHandler.getFilterColumns().stream()).collect(Collectors.toSet());
                if(dynamicFindColumnFilterHandler!=null && dynamicFindInjectInfoHandler.checkMapperIds(mapperId)){
                    List<ColumnFilterInfoHandler> dynamicHandlers = dynamicFindColumnFilterHandler.findColumnFilterHandlers();
                    if(!CollectionUtils.isEmpty(dynamicHandlers)){
                        filterColumns.addAll(dynamicHandlers.stream().filter(columnFilterInfoHandler->columnFilterInfoHandler.checkMapperId(mapperId))
                                .flatMap(columnFilterInfoHandler->columnFilterInfoHandler.getFilterColumns().stream()).collect(Collectors.toSet()));
                    }
                }
            }
        }
        // 处理sql
        if(handlers.size()>0 || !CollectionUtils.isEmpty(filterColumns)){
            DbType dbType = dbtype;
            if(dbType==null){
                dbType = SqlHelperAutoDbType.getDbType(mappedStatement.getConfiguration().getEnvironment().getDataSource());
            }
            SqlInjectColumnHelper sqlInjectColumnHelper = new SqlInjectColumnHelper(dbType,handlers,this.tbAliasPrefix);
            sqlInjectColumnHelper.setConfiguration(configuration);
            SystemMetaObject.forObject(boundSql).setValue("sql", sqlInjectColumnHelper.handlerSql(boundSql.getSql(),filterColumns,boundSql.getParameterMappings()));
        }
        Object re = null;
        if(isQuery){
            ResultHandler resultHandler = (ResultHandler) args[3];
            RowBounds rowBounds = (RowBounds) args[2];
            CacheKey cacheKey = executor.createCacheKey(mappedStatement, parameter, rowBounds, boundSql);
            re = executor.query(mappedStatement, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            if(!skipAble(re) &&  !CollectionUtils.isEmpty(filterColumns)){
                log.warn("降级为结果集过滤列："+String.join(",",filterColumns));
                filterColumns(re,filterColumns,configuration.isMapUnderscoreToCamelCase());
            }

        }else{
            re = invocation.proceed();
        }
        return re;
    }

    private boolean skipAble(Object o){
        if(o == null){
            return true;
        }
        if(CommonUtils.isPrimitiveOrWrap(o.getClass())){
            return true;
        }else if(o instanceof String){
            return true;
        }else if(o.getClass().isArray()){
            int length = Array.getLength(o);
            return length<=0 || skipAble(Array.get(o, 0));
        }else if(Collection.class.isAssignableFrom(o.getClass())){
            Collection list = (Collection)o;
            return list.size()<=0 || skipAble(list.iterator().next());
        }
        return false;

    }
    private void filterColumns(Object o,Set<String> ignoreColumns,boolean isMapUnderscoreToCamelCase){
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
                filterColumns(Array.get(o, i),ignoreColumns,isMapUnderscoreToCamelCase);
            }
        }else if(Collection.class.isAssignableFrom(o.getClass())){
            for(Object item:(Collection)o){
                filterColumns(item,ignoreColumns,isMapUnderscoreToCamelCase);
            }
        }else if(Map.class.isAssignableFrom(o.getClass())){
            List<String> removeKeys = new ArrayList<>();
            Map<String,Object> map = (Map<String,Object>)o;
            for(String key:map.keySet()){
                for(String column:ignoreColumns){
                    if(ignoreColumns.contains(column)){
                        removeKeys.add(key);
                    }
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
                for(String column:ignoreColumns){
                    boolean founded = false;
                    String name = field.getName();
                    if(name.equals(column)){
                        founded = true;
                    }else if(isMapUnderscoreToCamelCase && name.equalsIgnoreCase(column.replace("_",""))){
                        founded = true;
                    }
                    if(founded){
                        try {
                            field.set(o,null);
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
            }
        }
    }
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    @Override
    public void setProperties(Properties properties) {

    }

}