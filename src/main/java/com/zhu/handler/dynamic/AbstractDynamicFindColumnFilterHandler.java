package com.zhu.handler.dynamic;

import com.zhu.handler.ColumnFilterInfoHandler;

import java.util.List;

/**
 * The type Default dynamic find column filter handler.
 *
 * @param <T> 数据权限信息
 * @author heykb
 */
public abstract class AbstractDynamicFindColumnFilterHandler<T> implements DynamicFindColumnFilterHandler{
    /**
     * 编写将获取到的数据权限信息解析成handlers逻辑
     *
     * @param o the o
     * @return the list
     */
    abstract public List<ColumnFilterInfoHandler> parse(T o);

    /**
     * 编写从上下文获取用户的数据权限信息逻辑
     *
     * @return the permission info
     */
    abstract public T getPermissionInfo();

    @Override
    public List<ColumnFilterInfoHandler> findColumnFilterHandlers() {
        return parse(getPermissionInfo());
    }
}
