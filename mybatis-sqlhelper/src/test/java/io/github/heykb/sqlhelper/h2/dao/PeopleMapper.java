package io.github.heykb.sqlhelper.h2.dao;

import io.github.heykb.sqlhelper.h2.domain.People;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.ResultSetType;

import java.util.List;

/**
 * @author kb
 */
@Mapper
public interface PeopleMapper {
    /**
    * [新增]
    * @author kb
    **/
    int insert(People people);
    /**
    * [刪除]
    * @author kb
    **/
    int delete(@Param("id") String id);

    /**
    * [更新]
    * @author kb
    **/
    int update(People people);

    /**
    * [查询] 根据主键 id 查询
    * @author kb
    **/
    People select(@Param("id") String id);
    /**
    * [查询] 分页查询
    * @author kb
    **/
    List<People> selectList(People people);

    @Select("select * from people")
    List<People> selectList2(People people);

    @Options(resultSetType = ResultSetType.FORWARD_ONLY)
    @Select("select * from people")
    Cursor<People> selectCursor(People people);

    People leftJoinSelect(@Param("id") String id);


    List<People> noPluginSelect(People people);

    /**
    * [查询] 批量插入
    * @author kb
    **/
    int batchInsert(List<People> peoples);

    /**
    * [查询] 批量删除
    * @author kb
    **/
    int batchDelete(List<String> ids);
}
