package io.github.heykb.sqlhelper.handler;

import java.util.Collection;

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
