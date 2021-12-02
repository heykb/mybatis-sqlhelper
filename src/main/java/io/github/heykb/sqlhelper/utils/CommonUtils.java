package io.github.heykb.sqlhelper.utils;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.helper.Configuration;
import com.google.common.base.CaseFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static String adaptePropertyName(String originName, Configuration configuration){
        String re = originName;
        if(originName!=null && configuration!=null){
            Map<String, String> propertiesMap = configuration.getResultPropertiesMap();
            boolean isMapUnderscoreToCamelCase = configuration.isMapUnderscoreToCamelCase();
            if(configuration.getResultPropertiesMap()!=null && propertiesMap.containsKey(re)){
                re=propertiesMap.get(re);
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

}
