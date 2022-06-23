package io.github.heykb.sqlhelper.spring.primary.dao;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    @Select("select count(*) from employees")
    int count();

    @Select("select count(*) from employees")
    int noPluginCount();

    @Delete("delete from employees where id = #{id}")
    int deleteById(@Param("id")Integer id);


}
