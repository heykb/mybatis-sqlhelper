package com.zhu.handler.dynamic;

import com.zhu.handler.ColumnFilterInfoHandler;

import java.util.List;

/**
 * @author heykb
 */
public interface DynamicFindColumnFilterHandler {
    List<ColumnFilterInfoHandler> findColumnFilterHandlers();
    default boolean checkMapperIds(String mapperId){
        return true;
    }
}
