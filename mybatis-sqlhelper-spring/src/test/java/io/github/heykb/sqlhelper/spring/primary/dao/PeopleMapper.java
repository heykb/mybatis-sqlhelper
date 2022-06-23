package io.github.heykb.sqlhelper.spring.primary.dao;

import io.github.heykb.sqlhelper.spring.primary.domain.People;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
