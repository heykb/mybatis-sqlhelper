package io.github.heykb.sqlhelper.dynamicdatasource;

import com.sun.istack.internal.Nullable;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * The type Sql helper ds context holder.
 */
public class SqlHelperDsContextHolder {
    private static final Log log = LogFactory.getLog(SqlHelperDsContextHolder.class);
    private static final ThreadLocal<Stack<String>> CONTEXTHOLDER = new ThreadLocal<>();

    /**
     * 切换数据源
     *
     * @param logicName 数据源名称，null代表默认数据源
     */
    public static void switchTo(@Nullable String logicName) {
        Stack<String> switchStack = CONTEXTHOLDER.get();
        if(switchStack == null){
            switchStack = new Stack<>();
            CONTEXTHOLDER.set(switchStack);
        }
        switchStack.push(logicName);
    }

    /**
     * 退出当前数据源，会返回到上一次设置的值
     */
    public static void backToLast(){
        Stack<String> switchStack = CONTEXTHOLDER.get();
        if(switchStack!=null && !switchStack.empty()){
            switchStack.pop();
        }
    }

    /**
     * Clear.
     */
    public static void clear() {
        CONTEXTHOLDER.remove();
    }


    /**
     * Get string.
     *
     * @return the string
     */
    public static String get() {
        Stack<String> switchStack = CONTEXTHOLDER.get();
        if(switchStack!=null && !switchStack.empty()){
            return switchStack.peek();
        }
        return null;
    }

    public static <R> R executeOn(@Nullable String datasourceName, Callable<R> callable){
        switchTo(datasourceName);
        try {
            R re = callable.call();
            return re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            backToLast();
        }
    }


}
