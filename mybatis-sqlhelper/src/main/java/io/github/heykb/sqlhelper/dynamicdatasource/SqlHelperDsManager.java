package io.github.heykb.sqlhelper.dynamicdatasource;


import io.github.heykb.sqlhelper.config.SqlHelperException;

import javax.sql.DataSource;

public interface SqlHelperDsManager {

    void put(String logicName, LogicDsMeta dsMeta);

    /**
     * 移除某个逻辑数据源
     * @param logicName
     * @throws SqlHelperException 当logicName不存在
     * @return 当对应的数据源id没有被其他逻辑数据源引用时，从管理中删除的Datasource对象。否则返回null
     */
    DataSource remove(String logicName);

    boolean contains(String logicName);
}
