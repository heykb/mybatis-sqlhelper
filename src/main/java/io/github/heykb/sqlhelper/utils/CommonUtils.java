package io.github.heykb.sqlhelper.utils;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.google.common.base.CaseFormat;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author heykb
 */
public class CommonUtils {
    public static SQLBinaryOperator convert(String op){
        for (SQLBinaryOperator item:SQLBinaryOperator.values()){
            if(item.getName().equalsIgnoreCase(op.trim())){
                return item;
            }
        }
        throw new IllegalArgumentException(String.format("暂不支持%s操作",op));
    }
    public static boolean isPrimitiveOrWrap(Class clazz){
        try{
            return clazz.isPrimitive() || ((Class)clazz.getField("TYPE").get(null)).isPrimitive();
        }catch (Exception e){
            return false;
        }

    }

    public static String adaptePropertyName(String originName, Map<String, String> columnAliasMap, boolean isMapUnderscoreToCamelCase) {
        String re = originName;
        if (originName != null) {
            if (columnAliasMap != null && columnAliasMap.containsKey(re)) {
                re = columnAliasMap.get(re);
            }else if(isMapUnderscoreToCamelCase){
                if(Character.isUpperCase(re.charAt(0))){
                    re = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,re);
                }else{
                    re = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE,re);
                }
            }
        }
        return re;
    }

    public static List  getInstanceByClassName(String[] classNames) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List re = new ArrayList();
        if(classNames == null || classNames.length==0){
            return re;
        }
        for(String item:classNames){
            item = item.trim();
            Object obj = Class.forName(item).newInstance();
            re.add(obj);
        }
        return re;
    }


    public static void filterColumns(Object o, Set<String> ignoreColumns, boolean isMapUnderscoreToCamelCase) {
        if (o == null || CollectionUtils.isEmpty(ignoreColumns)) {
            return;
        }
        if (CommonUtils.isPrimitiveOrWrap(o.getClass())) {
            return;
        } else if (o instanceof String) {
            return;
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            for (int i = 0; i < length; i++) {
                filterColumns(Array.get(o, i), ignoreColumns, isMapUnderscoreToCamelCase);
            }
        } else if (Collection.class.isAssignableFrom(o.getClass())) {
            for (Object item : (Collection) o) {
                filterColumns(item, ignoreColumns, isMapUnderscoreToCamelCase);
            }
        } else if (Map.class.isAssignableFrom(o.getClass())) {
            List<String> removeKeys = new ArrayList<>();
            Map<String, Object> map = (Map<String, Object>) o;
            for (String key : map.keySet()) {
                for (String column : ignoreColumns) {
                    if (ignoreColumns.contains(column)) {
                        removeKeys.add(key);
                    }
                }
            }
            for (String key : removeKeys) {
                map.remove(key);
            }
        } else {
            Class clazz = o.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                for (String column : ignoreColumns) {
                    boolean founded = false;
                    String name = field.getName();
                    if (name.equals(column)) {
                        founded = true;
                    } else if (isMapUnderscoreToCamelCase && name.equalsIgnoreCase(column.replace("_", ""))) {
                        founded = true;
                    }
                    if (founded) {
                        try {
                            field.set(o, null);
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
            }
        }
    }


    public static boolean isEmpty(String str) {
        return (str == null || str.length() == 0);
    }
}
