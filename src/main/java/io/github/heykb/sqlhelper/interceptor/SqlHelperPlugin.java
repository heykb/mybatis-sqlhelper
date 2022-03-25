package io.github.heykb.sqlhelper.interceptor;

import com.alibaba.druid.DbType;
import io.github.heykb.sqlhelper.config.SqlHelperAutoDbType;
import io.github.heykb.sqlhelper.config.SqlHelperException;
import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.handler.abstractor.TenantInfoHandler;
import io.github.heykb.sqlhelper.handler.dynamic.DynamicFindColumnFilterHandler;
import io.github.heykb.sqlhelper.handler.dynamic.DynamicFindInjectInfoHandler;
import io.github.heykb.sqlhelper.helper.SqlStatementEditor;
import io.github.heykb.sqlhelper.typeHandler.ColumnFilterTypeHandler;
import io.github.heykb.sqlhelper.utils.CommonUtils;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段注入插件
 */
@Intercepts(
        {
                @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
                @Signature(type = Executor.class, method = "queryCursor", args = {MappedStatement.class, Object.class, RowBounds.class}),
        }
)
@Data
public class SqlHelperPlugin implements Interceptor {
    /** 一个布尔参数名,用于设置功能的总开关，默认true */
    public static final String enableProp = "enable";
    /**
     * 用于设置数据库类型的参数名称，非特殊不用配置，支持自动获取。
     */
    @Deprecated
    public static final String dbTypeProp = "dbType";
    /**
     * 一个布尔参数名,用于设置多租户功能开关，默认true。
     */
    public static final String multiTenantEnableProp = "multi-tenant.enable";
    /**
     * 一个布尔参数名,用于设置物理删除转逻辑删除功能开关，默认true。
     */
    public static final String logicDeleteEnableProp = "logic-delete.enable";
    /**
     * 一个逗号分割的全限定类名数组，用于设置注入信息类。
     */
    public static final String injectColumnInfoHandlersProp = "InjectColumnInfoHandler";
    /**
     * 一个逗号分割的全限定类名数组，用于设置数据权限中的字段过滤信息类的类名。
     */
    public static final String columnFilterInfoHandlersProp = "ColumnFilterInfoHandler";
    /**
     * 一个全限定类名，用于设置运行期间动态生成注入信息集合的类的类名
     */
    public static final String dynamicFindInjectInfoHandlerProp = "DynamicFindInjectInfoHandler";
    /**
     * 一个全限定类名，用于设置运行期间动态生成数据权限中的字段过滤信息集合的类的类名
     */
    public static final String dynamicFindColumnFilterHandlerProp = "DynamicFindColumnFilterHandler";
    private static final Log log = LogFactory.getLog(SqlHelperPlugin.class);
    /**
     * 指定数据库类型。不需要设置，自动解析。
     */
    private DbType dbType;
    /**
     * 开关插件
     */
    private boolean enable = true;
    /**
     * 开启关闭逻辑删除转换。关闭后所有TenantInfoHandler注入都失效
     */
    private boolean multiTenantEnable  = true;
    /**
     * 开启关闭逻辑删除转换。关闭后所有LogicDeleteInfoHandler注入都失效
     */
    private boolean logicDeleteEnable  = true;
    /**
     * 静态注入信息集合
     */
    private Collection<InjectColumnInfoHandler> injectColumnInfoHandlers;
    /**
     * 数据权限：字段过滤信息集合
     */
    private Collection<ColumnFilterInfoHandler> columnFilterInfoHandlers;
    /**
     * 待定！
     */
    private String tbAliasPrefix;
    /**
     * 注入信息动态获取类集合
     */
    private DynamicFindInjectInfoHandler dynamicFindInjectInfoHandler;
    /**
     * 数据权限：字段过滤信息动态获取集合
     */
    private DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler;

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        if(properties == null){
            properties = new Properties();
        }
        try{
            this.enable = Boolean.parseBoolean((String) properties.getOrDefault("enable","true")) ;
            this.multiTenantEnable = Boolean.parseBoolean((String) properties.getOrDefault("multi-tenant.enable","true")) ;
            this.logicDeleteEnable = Boolean.parseBoolean((String)properties.getOrDefault("logic-delete.enable","true")) ;
            this.dbType = DbType.of(properties.getProperty("dbType"));
            this.tbAliasPrefix  = properties.getProperty("tbAliasPrefix");
            // 读取InjectColumnInfoHandler
            String commaString = properties.getProperty("InjectColumnInfoHandler");
            if (!CommonUtils.isEmpty(commaString)) {
                String[] classes = commaString.split(",");
                this.injectColumnInfoHandlers = CommonUtils.getInstanceByClassName(classes);
            }
            // 读取DynamicFindInjectInfoHandler
            String classString = properties.getProperty("DynamicFindInjectInfoHandler");
            if (!CommonUtils.isEmpty(classString)) {
                String[] classes = new String[]{classString};
                this.dynamicFindInjectInfoHandler = (DynamicFindInjectInfoHandler) CommonUtils.getInstanceByClassName(classes).get(0);
            }
            // 读取InjectColumnInfoHandler
            commaString = properties.getProperty("ColumnFilterInfoHandler");
            if (!CommonUtils.isEmpty(commaString)) {
                String[] classes = commaString.split(",");
                this.columnFilterInfoHandlers = CommonUtils.getInstanceByClassName(classes);
            }
            // 读取DynamicFindInjectInfoHandler
            classString = properties.getProperty("DynamicFindColumnFilterHandler");
            if (!CommonUtils.isEmpty(classString)) {
                String[] classes = new String[]{classString};
                this.dynamicFindColumnFilterHandler = (DynamicFindColumnFilterHandler) CommonUtils.getInstanceByClassName(classes).get(0);
            }

        }catch (Exception e){
            throw new SqlHelperException("插件属性解析错误");
        }

    }




    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if(!this.enable){
            return invocation.proceed();
        }
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];
        BoundSql boundSql = null;
        boolean useTempMappedStatement = true;
        //由于逻辑关系，只会进入一次
        if (args.length == 6) {
            boundSql = (BoundSql) args[5];
            useTempMappedStatement = false;
        } else {
            boundSql = mappedStatement.getBoundSql(parameter);
        }
        DbType dbType = SqlHelperAutoDbType.getDbType(mappedStatement.getConfiguration().getEnvironment().getDataSource());
        Set<String> filterColumns = changeSql(mappedStatement, boundSql, dbType);
        if (useTempMappedStatement) {
            args[0] = newMappedStatement(mappedStatement, boundSql);
        }
        Object re = invocation.proceed();
        if (!CollectionUtils.isEmpty(filterColumns)) {
            log.warn("降级为结果集过滤列：" + String.join(",", filterColumns));
            String methodName = invocation.getMethod().getName();
            if (re instanceof Cursor) {
                return new ColumnFilterCursor((Cursor) re, filterColumns, mappedStatement.getConfiguration().isMapUnderscoreToCamelCase());
            } else if ("query".equals(methodName) && !skipAble(re)) {
                CommonUtils.filterColumns(re, filterColumns, mappedStatement.getConfiguration().isMapUnderscoreToCamelCase());
            }
        }
        return re;
    }

    MappedStatement newMappedStatement(MappedStatement source, BoundSql boundSql) {
        MappedStatement.Builder builder = new MappedStatement.Builder(source.getConfiguration(), source.getId(), new SqlSource() {
            @Override
            public BoundSql getBoundSql(Object parameterObject) {
                return boundSql;
            }
        }, source.getSqlCommandType());
        builder.resource(source.getResource());
        builder.parameterMap(source.getParameterMap());
        builder.resultMaps(source.getResultMaps());
        builder.fetchSize(source.getFetchSize());
        builder.timeout(source.getFetchSize());
        builder.statementType(source.getStatementType());
        builder.resultSetType(source.getResultSetType());
        builder.cache(source.getCache());
        builder.flushCacheRequired(source.isFlushCacheRequired());
        builder.useCache(source.isUseCache());
        builder.resultOrdered(source.isResultOrdered());
        builder.keyGenerator(source.getKeyGenerator());
        if (source.getKeyProperties() != null) {
            builder.keyProperty(String.join(",", source.getKeyProperties()));
        }
        if (source.getKeyColumns() != null) {
            builder.keyColumn(String.join(",", source.getKeyColumns()));
        }
        builder.databaseId(source.getDatabaseId());
        builder.lang(source.getLang());
        if (source.getResultSets() != null) {
            builder.resultSets(String.join(",", source.getResultSets()));
        }
        return builder.build();
    }
    /**
     * 获取可用的注入
     *
     * @param handlers    the handlers
     * @param curMapperId the cur mapper id
     * @return list list
     */
    List<InjectColumnInfoHandler> getEnabledInjectColumnInfoHandler(String curMapperId, Collection<InjectColumnInfoHandler> handlers, DynamicFindInjectInfoHandler dynamicFindInjectInfoHandler) {
        List<InjectColumnInfoHandler> re = new ArrayList<>();
        if(handlers != null){
            for(InjectColumnInfoHandler item:handlers){
                if(!multiTenantEnable && item instanceof TenantInfoHandler){
                    continue;
                }
                if(!logicDeleteEnable && item instanceof LogicDeleteInfoHandler){
                    continue;
                }
                if(item.checkMapperId(curMapperId)){
                    re.add(item);
                }
            }
        }
        if (dynamicFindInjectInfoHandler != null && dynamicFindInjectInfoHandler.checkMapperIds(curMapperId)) {
            List<InjectColumnInfoHandler> dynamicHandlers = dynamicFindInjectInfoHandler.findInjectInfoHandlers();
            if (!CollectionUtils.isEmpty(dynamicHandlers)) {
                re.addAll(getEnabledInjectColumnInfoHandler(curMapperId, dynamicHandlers, null));
            }
        }
        return re;
    }


    List<ColumnFilterInfoHandler> getEnabledColumnFilterInfoHandler(String mapperId, Collection<ColumnFilterInfoHandler> handlers, DynamicFindColumnFilterHandler dynamicFindColumnFilterHandler) {
        List<ColumnFilterInfoHandler> re = new ArrayList<>();
        if (handlers != null) {
            for (ColumnFilterInfoHandler item : handlers) {
                if (item.checkMapperId(mapperId)) {
                    re.add(item);
                }
            }
            if (dynamicFindColumnFilterHandler != null && dynamicFindColumnFilterHandler.checkMapperIds(mapperId)) {
                List<ColumnFilterInfoHandler> dynamicHandlers = dynamicFindColumnFilterHandler.findColumnFilterHandlers();
                if (!CollectionUtils.isEmpty(dynamicHandlers)) {
                    re.addAll(getEnabledColumnFilterInfoHandler(mapperId, dynamicHandlers, null));
                }
            }
        }
        return re;

    }
    /**
     * Change sql set.
     *
     * @param mappedStatement the mapped statement
     * @param boundSql        the bound sql
     * @return set set
     */
    Set<String> changeSql(MappedStatement mappedStatement, BoundSql boundSql, DbType dbType) {
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        Map<String, String> resultPropertiesMap = new HashMap<>();
        for (ResultMap resultMap : resultMaps) {
            List<ResultMapping> resultMappings = resultMap.getPropertyResultMappings();
            for(ResultMapping resultMapping:resultMappings){
                if(resultMapping.getColumn()!=null){
                    resultPropertiesMap.put(resultMapping.getProperty(),resultMapping.getColumn());
                }
            }
        }
        String mapperId = mappedStatement.getId();
        //过滤InjectColumnInfoHandler
        List<InjectColumnInfoHandler> handlers = getEnabledInjectColumnInfoHandler(mapperId, this.injectColumnInfoHandlers, this.dynamicFindInjectInfoHandler);
        // 获取有无查询列过滤
        List<ColumnFilterInfoHandler> columnFilterInfoHandlers = getEnabledColumnFilterInfoHandler(mapperId, this.columnFilterInfoHandlers, this.dynamicFindColumnFilterHandler);

        // 处理sql
        if (handlers.size() > 0 || columnFilterInfoHandlers.size() > 0) {
            SqlStatementEditor sqlStatementEditorFactory = new SqlStatementEditor.Builder(boundSql.getSql(), dbType)
                    .columnAliasMap(resultPropertiesMap)
                    .isMapUnderscoreToCamelCase(mappedStatement.getConfiguration().isMapUnderscoreToCamelCase())
                    .injectColumnInfoHandlers(handlers)
                    .columnFilterInfoHandlers(columnFilterInfoHandlers)
                    .build();
            SqlStatementEditor.Result result = sqlStatementEditorFactory.processing();
            if (result != null) {
                if (boundSql.getParameterMappings() != null && !CollectionUtils.isEmpty(result.getRemovedParamIndex())) {
                    for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
                        TypeHandler typeHandler = parameterMapping.getTypeHandler();
                        SystemMetaObject.forObject(parameterMapping).setValue("typeHandler", new ColumnFilterTypeHandler(typeHandler, result.getRemovedParamIndex()));
                    }
                }
                SystemMetaObject.forObject(boundSql).setValue("sql", result.getSql());
                return result.getFailedFilterColumns();
            }
        }
        return null;
    }

    private boolean skipAble(Object o) {
        if (o == null) {
            return true;
        }
        if (CommonUtils.isPrimitiveOrWrap(o.getClass())) {
            return true;
        } else if (o instanceof String) {
            return true;
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            return length <= 0 || skipAble(Array.get(o, 0));
        } else if (Collection.class.isAssignableFrom(o.getClass())) {
            Collection list = (Collection) o;
            return list.size() <= 0 || skipAble(list.iterator().next());
        }
        return false;
    }


}