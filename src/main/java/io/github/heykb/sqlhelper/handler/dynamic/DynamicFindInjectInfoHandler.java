package io.github.heykb.sqlhelper.handler.dynamic;

import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;

import java.util.List;

/**
 * @author heykb
 */
public interface DynamicFindInjectInfoHandler {
    List<InjectColumnInfoHandler> findInjectInfoHandlers();

    default boolean checkMapperIds(String mapperId){
        return true;
    }
}
