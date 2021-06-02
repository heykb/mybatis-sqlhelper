# MyBatis SqlHelper 数据权限专篇
## 数据权限
数据权限是指对系统用户进行数据资源可见性的控制。权限控制粒度达到行和列级别。<br>
MyBatis SqlHelper实现了底层最重要的过滤逻辑，借助MyBatis SqlHelper您可以封装您自己的业务层逻辑实现数据权限管理
### 特点
* <b>完整</b>：支持行级别和列级别的sql注入
* <b>智能</b>：所有的注入通过sql解析器智能操作，无须编码修改现有sql
* <b>通用</b>：适配所有MyBatis项目，包括像mybatis-plus,tk-mybatis等；支持多种数据库（基于阿里 druid sql解析器）
* <b>方便</b>：依托于spring boot ,代码集成方便

## MyBatis SqlHelper实现
1. 行级别的控制方式：利用[阿里 druid sql解析器](https://github.com/alibaba/druid/wiki/SQL-Parser)解析查询语句注入sql条件<br>
2. 列级别的控制方式：对查询结果集过滤，分为在```数据库sql层过滤```和```java代码层结果集过滤```两种实现方式

## 使用范例
### 行级别控制
1. 单条件注入：继承[ConditionInjectInfoHandler](./src/main/java/com/zhu/handler/abstractor/ConditionInjectInfoHandler.java)类，重写以下必要方法
~~~java
@Component
public class SimpleConditionInjectInfoHandler extends ConditionInjectInfoHandler {

    /**
     * 条件左值（实体属性名称或者表列名）
     * @return
     */
    @Override
    public String getColumnName() {
        return "createdBy";
    }

     /**
     * 条件右值（sql能识别的都可以包括子查询语句）
     * @return
     */
    @Override
    public String getValue() {
        return "zrc";
    }

    /**
     * 操作符(sql能识别的都可以)
     * @return
     */
    @Override
    public String op(){
        return "=";
    }

    /**
     * 设置表级别过滤逻辑
     *
     * @param tableName the table name
     * @return boolean
     */
    public boolean checkTableName(String tableName){
        return "example".equals(tableName);
    }

    /**
     * 设置mapperId方法级别过滤逻辑
     *
     * @param mapperId the mapper id
     * @return boolean
     */
    public boolean checkMapperId(String mapperId){
        return true;
    }
}
~~~
2. 多条件注入：继承[BinaryConditionInjectInfoHandler](./src/main/java/com/zhu/handler/abstractor/BinaryConditionInjectInfoHandler.java)类...
### 输入：
~~~sql
SELECT created_by, dept_id, importand_data, data, tenant_id, id
FROM test
LIMIT ? OFFSET ?
~~~
### 输出：
~~~sql
SELECT created_by, dept_id, importand_data, data, tenant_id , id 
FROM test 
WHERE created_by = 'zrc' 
LIMIT ? OFFSET ? 
~~~
   
### 列级别控制
实现[ColumnFilterInfoHandler](./src/main/java/com/zhu/handler/ColumnFilterInfoHandler.java)类
 ~~~java
@Component
public class SimpleColumnFilterInfoHandler extends ColumnFilterInfoHandler {

    public Collection<String> getFilterColumns(){
        return Arrays.asList("importantData");
    }
    
    /**
     * 设置mapperId方法级别过滤逻辑
     *
     * @param mapperId the mapper id
     * @return boolean
     */
    public boolean checkMapperId(String mapperId){
        return "com.zhu.mapper.ExampleMapper.pageList".equals(mapperId);
    }
}
~~~
配置：
~~~yml
sqlhelper:
  # sql：通过注入sql过滤列（默认），result：通过对java结果集过滤列
  columnFilterType: sql
~~~
### 输入：
~~~sql
SELECT created_by, dept_id, importand_data, data, tenant_id, id
FROM test
LIMIT ? OFFSET ?
~~~
### 输出：
~~~sql
SELECT created_by, dept_id, data, tenant_id, id  
FROM( 
    SELECT created_by, dept_id, importand_data, data, tenant_id, id
    FROM test
    LIMIT ? OFFSET ? 
) _sql_help_ 
~~~

### 程序运行启动动态分配注入
以上方式都是程序启动就分配好的（权限）注入，不适用数据权限管理为不同用户分配不同的权限。<br>
MyBatis SqlHelper提供了[DynamicFindInjectInfoHandler](./src/main/java/com/zhu/handler/dynamic/DynamicFindInjectInfoHandler.java)和[DynamicFindColumnFilterHandler](./src/main/java/com/zhu/handler/dynamic/DynamicFindColumnFilterHandler.java),您可以实现他们并编写```根据用户权限构造handler列表返回```逻辑。

### 缺憾
针对列级别过滤，如果columnFilterType为sql,那么要求原查询sql不得使用```select *```查询列，所有查询列必须明确写出来。<br>
如果columnFilterType为result则不存在这个问题，因为这种方式不是在数据库级别过滤字段了，而是在java代码层。
