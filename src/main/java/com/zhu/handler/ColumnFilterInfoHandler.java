package com.zhu.handler;

import java.util.Collection;
import java.util.Set;

/**
 * The interface Column filter info handler.
 *
 * @author heykb
 */
public interface ColumnFilterInfoHandler {
    /**
     * Filter columns set.
     *
     * @return the set
     */
    Collection<String> getFilterColumns();

    /**
     * 设置mapperId方法级别过滤逻辑
     *
     * @param mapperId the mapper id
     * @return boolean boolean
     */
     boolean checkMapperId(String mapperId);
}
