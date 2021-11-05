package com.zhu.handler.dynamic;

import com.zhu.handler.InjectColumnInfoHandler;

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
