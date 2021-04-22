# MyBatis多租户、自动注入插件-SqlHelper
如果您正在使用MyBatis，并且您的项目需要多租户、逻辑删除的功能建议您使用，这一定是<b>最方便的</b>方式，您的应用程序将具备在几种模式间随意切换的能力。
## 集成
~~~java
    <dependency>
        <groupId>com.zhu</groupId>
        <artifactId>mybatis-sqlHelper</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
~~~
## 特点
* 可插拔
* 简单方便
* 高效率（基于阿里 druid sql解析器）
* 将多租户、逻辑删除与应用程序解耦，随配随用
* 强大的字段自动注入能力（<i>查询条件注入/插入语句注入/更新语句注入</i>），定制其他业务逻辑
* 支持多种数据库（基于阿里 druid sql解析器）
## 使用方式
### 多租户
配置文件
~~~yml
sqlhelper:
  enable: true
  dbtype: postgresql
  multi-tenant:
    enable: true
~~~
只需要继承[TenantInfoHanlder](./src/main/java/com/zhu/handler/TenantInfoHanlder.java)类，重写以下必要方法。
~~~java
@Component
public class SimpleTenantInfoHanlder extends TenantInfoHanlder {

    /**
     * 设置代表租户字段名称
     * @return
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 当前租户value获取方式
     * @return
     */
    @Override
    public Object getTenantId() {
        // SecurityContextHolder.getContext().getAuthentication()
        return "123456";
    }
    /**
     * 设置表级别过滤逻辑
     * @param tableName
     * @return
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return false;
    }

}
~~~

### 真实删除转逻辑删除
1. 配置文件的方式配置逻辑删除(内置的配置文件配置实现类[DefaultLogicDeleteInfoHandler](./src/main/java/com/zhu/handler/defaultimpl/DefaultLogicDeleteInfoHandler.java))
~~~yml
sqlhelper:
  enable: true
  dbtype: postgresql
  logic-delete:
    enable: true
    sqldemo: update xx set is_deleted = 'Y' where id = 'xx'
    columnName: is_deleted
    not-deleted-value: N
    ignore-tables:
      - people
    ignore-mapper-ids:
      - com.**.xx
~~~
1. 代码配置方式
   继承[LogicDeleteInfoHandler](./src/main/java/com/zhu/handler/LogicDeleteInfoHandler.java)类，重写主要方法即可
### 自动注入
接口：[InjectColumnInfoHandler](./src/main/java/com/zhu/handler/InjectColumnInfoHandler.java)。插件会自动扫描该接口所有bean。
1. CONDITION 条件注入：未被过滤的所有查询条件语句（包括子查询）都会被注入指定条<br>
   如：定义这样一条注入信息
   ~~~java
   InjectColumnInfoHandler injectColumnInfoHandler = new InjectColumnInfoHandler() {
            @Override
            public String getColumnName() {
                return "tenant_id";
            }

            @Override
            public Object getValue() {
                return "sqlhelper";
            }

            @Override
            public int getInjectTypes() {
                return CONDITION;
            }
        };
   ~~~
   
   ### 输入：
   ~~~sql
   select * from user s
   ~~~
   ### 输出：
   ~~~sql
   SELECT * FROM user s WHERE s.tenant_id = 'sqlhelper'
   ~~~
    ### 输入：
   ~~~sql
   select * from 
   (select * from tab t where id = 2 and name = 'wenshao') s 
   where s.name='333'
   ~~~
   ### 输出：
   ~~~sql
    SELECT *
    FROM (
        (SELECT *
        FROM tab t
        WHERE id = 2
            AND name = 'wenshao'
            AND t.tenant_id = 'sqlhelper')
    ) s
    WHERE s.name = '333'
   ~~~
   ### 输入：
   ~~~sql
    SELECT u.*, g.name
    FROM user u
        JOIN user_group g ON u.groupId = g.groupId
    WHERE u.name = '123'
   ~~~
   ### 输出：
   ~~~sql
    SELECT u.*, g.name
    FROM user u
        JOIN user_group g ON u.groupId = g.groupId
    WHERE u.name = '123'
        AND u.tenant_id = 'sqlhelper'
        AND g.tenant_id = 'sqlhelper'
   ~~~
   
2. INSERT 新增注入：向插入语句中增加一列信息
   <br>
   如：定义这样一条注入信息
   ~~~java
   InjectColumnInfoHandler injectColumnInfoHandler = new InjectColumnInfoHandler() {
            @Override
            public String getColumnName() {
                return "tenant_id";
            }

            @Override
            public Object getValue() {
                return "sqlhelper";
            }

            @Override
            public int getInjectTypes() {
                return INSERT;
            }
        };
   ~~~
   
   ### 输入：
   ~~~sql
    INSERT INTO user (id, name)
    VALUES ('0', 'heykb')
   ~~~
   ### 输出：
   ~~~sql
    INSERT INTO user (id, name, tenant_id)
    VALUES ('0', 'heykb', 'sqlhelper')
   ~~~
   ### 输入：
   ~~~sql
    INSERT INTO user (id, name)
    SELECT g.id, g.name
    FROM user_group g
    WHERE id = 1
   ~~~
   ### 输出：
   ~~~sql
    INSERT INTO user (id, name, tenant_id)
    SELECT g.id, g.name
    FROM user_group g
    WHERE id = 1
   ~~~

## 未完待续。。(留下你的小星星鼓励我完善它)
## 联系我
QQ: 1259609102<br>
email: bigsheller08@gmail.com,1259609102@qq.com