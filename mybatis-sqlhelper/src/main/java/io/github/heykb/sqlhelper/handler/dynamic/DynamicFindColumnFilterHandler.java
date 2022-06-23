package io.github.heykb.sqlhelper.handler.dynamic;

import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;

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
