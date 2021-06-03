package com.zhu.utils;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.google.common.base.CaseFormat;
import com.zhu.handler.InjectColumnInfoHandler;
import com.zhu.helper.Configuration;

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

    public static String adaptePropertieName(String originName, Configuration configuration){
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


}
