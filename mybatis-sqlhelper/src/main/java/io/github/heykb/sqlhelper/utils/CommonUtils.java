package io.github.heykb.sqlhelper.utils;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.google.common.base.CaseFormat;
import org.apache.commons.collections.CollectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * The type Common utils.
 *
 * @author heykb
 */
public class CommonUtils {
    /**
     * Convert sql binary operator.
     *
     * @param op the op
     * @return the sql binary operator
     */
    public static SQLBinaryOperator convert(String op){
        for (SQLBinaryOperator item:SQLBinaryOperator.values()){
            if(item.getName().equalsIgnoreCase(op.trim())){
                return item;
            }
        }
        throw new IllegalArgumentException(String.format("暂不支持%s操作",op));
    }

    /**
     * Is primitive or wrap boolean.
     *
     * @param clazz the clazz
     * @return the boolean
     */
    public static boolean isPrimitiveOrWrap(Class clazz){
        try{
            return clazz.isPrimitive() || ((Class)clazz.getField("TYPE").get(null)).isPrimitive();
        }catch (Exception e){
            return false;
        }

    }

    /**
     * Adapte property name string.
     *
     * @param originName                 the origin name
     * @param columnAliasMap             the column alias map
     * @param isMapUnderscoreToCamelCase the is map underscore to camel case
     * @return the string
     */
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

    /**
     * Gets instance by class name.
     *
     * @param classNames the class names
     * @return the instance by class name
     * @throws ClassNotFoundException the class not found exception
     * @throws IllegalAccessException the illegal access exception
     * @throws InstantiationException the instantiation exception
     */
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


    /**
     * Filter columns.
     *
     * @param o                          the o
     * @param ignoreColumns              the ignore columns
     * @param isMapUnderscoreToCamelCase the is map underscore to camel case
     */
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
//            List<String> removeKeys = new ArrayList<>();
            Map<String, Object> map = (Map<String, Object>) o;
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            while(iterator.hasNext()){
                if(ignoreColumns.contains(iterator.next().getKey().toLowerCase())){
                    iterator.remove();
                }
            }
//            for (String key : map.keySet()) {
//                for (String column : ignoreColumns) {
//                    if (ignoreColumns.contains(column)) {
//                        removeKeys.add(key);
//                    }
//                }
//            }
//            for (String key : removeKeys) {
//                map.remove(key);
//            }
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


    /**
     * Is empty boolean.
     *
     * @param str the str
     * @return the boolean
     */
    public static boolean isEmpty(String str) {
        return (str == null || str.length() == 0);
    }


    /**
     * 字符串通配符匹配：支持？ *
     *
     * @param pattern the pattern 通配符
     * @param str     the str 被匹配字符串
     * @return the boolean
     */
    public static boolean wildcardMatch(String pattern,String str){
        int m = pattern.length();
        int n = str.length();
        boolean[][] dp = new boolean[m+1][n+1];
        dp[0][0] = true;
        for(int i=0;i<pattern.length();++i){
            if(pattern.charAt(i)=='*'){
                dp[i+1][0] = true;
            }else{
                break;
            }
        }
        for(int i=0;i<m;++i){
            for(int j=0;j<n;++j){
                char p = pattern.charAt(i);
                char ch = str.charAt(j);
                if(p=='*'){
                    // 使用*消耗掉ch->i,j；不使用*消耗掉ch->i,j+1 ；使用*消耗掉ch,但是*是二次使用ch->i+1,j
                    dp[i+1][j+1] = dp[i][j+1] || dp[i][j] || dp[i+1][j];
                }else{
                    if(p=='?' || ch==p){
                        dp[i+1][j+1] = dp[i][j];
                    }else{
                        dp[i+1][j+1] = false;
                    }
                }
            }
        }
        return dp[m][n];
    }


}
