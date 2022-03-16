package io.github.heykb.sqlhelper.h2.dao;


import io.github.heykb.sqlhelper.h2.domain.Employee;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmployeeMapper {

    @Select("select count(*) from employees")
    int count();

    @Select("select count(*) from employees")
    int noPluginCount();

    @Delete("delete from employees where id = #{id}")
    int deleteById(@Param("id") Integer id);


    @Select("select * from employees")
    List<Employee> selectAll();


}
