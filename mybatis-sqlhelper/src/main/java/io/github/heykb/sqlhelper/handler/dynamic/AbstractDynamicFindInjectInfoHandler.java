package io.github.heykb.sqlhelper.handler.dynamic;

import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;

import java.util.List;

/**
 * The type Default dynamic find column filter handler.
 *
 * @param <T> the type parameter
 * @author heykb
 */
public abstract class AbstractDynamicFindInjectInfoHandler<T> implements DynamicFindInjectInfoHandler{

    /**
     * 编写将获取到的数据权限信息解析成handlers逻辑
     *
     * @param o the o
     * @return the list
     */
    abstract public List<InjectColumnInfoHandler> parse(T o);

    /**
     * 编写从上下文获取用户的数据权限信息逻辑
     *
     * @return the permission info
     */
    abstract public T getPermissionInfo();

    @Override
    public List<InjectColumnInfoHandler> findInjectInfoHandlers() {
        return parse(getPermissionInfo());
    }
}
