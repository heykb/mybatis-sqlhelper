# MyBatis 多租户、逻辑删除、数据权限插件-SqlHelper
如果您正在使用MyBatis，并且您的项目需要<b>多租户、逻辑删除和数据权限</b>的功能建议您使用，这一定是<b>最方便的</b>方式，使用它对您的现有代码没有侵入，您不需要对现有代码进行任何修改。
~~~xml
    <dependency>
        <groupId>io.github.heykb</groupId>
        <artifactId>mybatis-sqlHelper</artifactId>
        <version>1.1</version>
    </dependency>
~~~
文档
----------
  - [特点](#特点)
  - [使用自动注入](#mybatis-sqlhelper使用自动注入)
    - [能帮你做什么？](#能帮你做什么)
    - [准备](#准备)
    - [CONDITION条件注入](#condition条件注入)
    - [INSERT插入列注入](#insert插入列注入)
    - [UPDATE更新列注入...](#update更新列注入)
    - [SELECT_ITEM查询列注入...](#select_item查询列注入)
  - [使用多租户](#mybatis-sqlhelper使用多租户)
    - [能帮你做什么？](#能帮你做什么-1)
    - [准备](#准备-1)
    - [1.开启多租户](#1开启多租户)
    - [2.注入bean](#2注入bean)
    - [3.启动](#3启动项目观察日志)
    - [4.灵活配置](#4灵活配置)
  - [使用物理删除切换逻辑删除](#mybatis-sqlhelper使用物理删除切换逻辑删除)
    - [能帮你做什么？](#能帮你做什么-2)
    - [准备](#准备-2)
    - [1.开启逻辑删除开关](#1开启逻辑删除开关)
    - [2.添加配置](#2添加配置)
    - [3.观察日志](#3观察日志)
  - [使用数据权限](#使用数据权限)
  - [未完待续](#未完待续留下你的start鼓励我完善它)
  - [参与贡献](#参与贡献)
  - [联系我](#联系我)
 
## 特点
* 可实现多租户
* 可实现真实删除转逻辑删除
* 可实现行级别和列级别权限控制（自动解析注入）
* 可插拔
* 简单方便
* 高效率（基于[阿里 druid sql解析器](https://github.com/alibaba/druid/wiki/SQL-Parser)）
* 将多租户、逻辑删除与应用程序解耦，随配随用
* 强大的字段自动注入能力（<i>查询条件注入/插入语句注入/更新语句注入/查询列过滤</i>），定制其他业务逻辑
* 支持多种数据库（基于阿里 druid sql解析器）

## Mybatis-Sqlhelper使用自动注入
### 能帮你做什么？
    1. 多种类型的sql动态注入能力
### 准备 
1.  在spring boot mybatis项目中引入mybatis-sqlhelper
~~~java
    <dependency>
        <groupId>io.github.heykb</groupId>
        <artifactId>mybatis-sqlHelper</artifactId>
        <version>1.0</version>
    </dependency>
~~~
2. 开启总开关 ```sqlhelper.enable=true```
### CONDITION条件注入
1. 单一条件注入， 实现[InjectColumnInfoHandler](src/main/java/io/github/heykb/sqlhelper/handler/InjectColumnInfoHandler.java)，如：
~~~java
InjectColumnInfoHandler injectColumnInfoHandler = new InjectColumnInfoHandler() {
        @Override
        public String getColumnName() {
            return "tenant_id";
        }
        @Override
        public String getValue() {
            return "sqlhelper";
        }
        @Override
        public String op() {
            return "=";
        }
        @Override
        public int getInjectTypes() {
            return CONDITION;
        }
        @Override
        public boolean checkCommandType(SqlCommandType commandType) {
            return true;
        }
        @Override
        public boolean checkTableName(String tableName) {
            return true;
        }
};
~~~
##### 查询语句中： 
~~~java 
select * from user s
~~~
##### 输出：
~~~sql
SELECT * FROM user s WHERE s.tenant_id = 'sqlhelper'
~~~
##### 更新语句中： 
~~~java 
update user set name = ? where id = ?
~~~
##### 输出：
~~~sql
update user set name = ? where id = ? and user.tenant_id = 'sqlhelper'
~~~
##### 删除语句中： 
~~~java 
delete from user where id = ?
~~~
##### 输出：
~~~sql
delete from user where id = ? and user.tenant_id = 'sqlhelper'
~~~
##### 外连接语句中： 
~~~java 
SELECT * FROM user u left JOIN card c ON u.id = c.user_id
~~~
##### 输出：
~~~sql
SELECT *
FROM user u
	LEFT JOIN card c
	ON u.id = c.user_id
		AND c.tenant_id = sqlhelper
WHERE u.tenant_id = sqlhelper
~~~
##### 各种子查询语句中： 
~~~java 
SELECT * FROM (select * from user where id = 2) s
~~~
##### 输出：
~~~sql
SELECT *
FROM (
	(SELECT *
	FROM user
	WHERE id = 2
		AND user.tenant_id = sqlhelper)
) s
~~~
2. 多条件组合注入继承[BinaryConditionInjectInfoHandler](src/main/java/io/github/heykb/sqlhelper/handler/abstractor/BinaryConditionInjectInfoHandler.java)...
### INSERT插入列注入
1. 单一条件注入,实现[InjectColumnInfoHandler](src/main/java/io/github/heykb/sqlhelper/handler/InjectColumnInfoHandler.java)，如：
~~~java
InjectColumnInfoHandler injectColumnInfoHandler = new InjectColumnInfoHandler() {
        
        @Override
        public String getColumnName() {
            return "tenant_id";
        }
        @Override
        public String getValue() {
            return "sqlhelper";
        }
    
        @Override
        public int getInjectTypes() {
            return INSERT;
        }
        @Override
        public boolean checkCommandType(SqlCommandType commandType) {
            return true;
        }
        @Override
        public boolean checkTableName(String tableName) {
            return true;
        }
};
~~~
#### 输入：
~~~sql
INSERT INTO user (id, name)
VALUES ('0', 'heykb')
~~~
#### 输出：
~~~sql
INSERT INTO user (id, name, tenant_id)
VALUES ('0', 'heykb', 'sqlhelper')
~~~
#### 输入：
~~~sql
INSERT INTO user (id, name)
SELECT g.id, g.name
FROM user_group g
WHERE id = 1
~~~
#### 输出：
~~~sql
INSERT INTO user (id, name, tenant_id)
SELECT g.id, g.name
FROM user_group g
WHERE id = 1
~~~
### UPDATE更新列注入...
### SELECT_ITEM查询列注入...

## Mybatis-Sqlhelper使用多租户
### 能帮你做什么？
    1. 自动为所有where 、join on添加租户过滤条件
    2. 自动为insert语句添加租户列的插入
    3. 多租户的实现也是利用sqlhelper的自动注入功能，相当于配置了CONDITIO与INSERT的两种注入
### 准备 
1.  在spring boot mybatis项目中引入mybatis-sqlhelper
~~~java
    <dependency>
        <groupId>io.github.heykb</groupId>
        <artifactId>mybatis-sqlHelper</artifactId>
        <version>1.0</version>
    </dependency>
~~~
2. 开启总开关
```sqlhelper.enable=true```
### 1.开启多租户
```sqlhelper.multi-tenant.enable=true```
### 2.注入bean。
注入租户信息获取bean,继承[TenantInfoHanlder](src/main/java/io/github/heykb/sqlhelper/handler/abstractor/TenantInfoHanlder.java)，如：
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
     * 当前租户value获取方式。通常从线程上下文中获取用户租户信息
     * @return
     */
    @Override
    public String getTenantId() {
        // SecurityContextHolder.getContext().getAuthentication()
        return "sqlhelper";
    }

}
~~~
### 3.启动项目，观察日志

### 4.灵活配置。
过滤特定的表、过滤特定的sql maperid、过滤特定的sql类型。。。参看自动注入一章

## Mybatis-Sqlhelper使用物理删除切换逻辑删除
### 能帮你做什么？
    1. 多种类型的sql动态注入能力
### 准备 
1.  在spring boot mybatis项目中引入mybatis-sqlhelper
~~~java
    <dependency>
        <groupId>io.github.heykb</groupId>
        <artifactId>mybatis-sqlHelper</artifactId>
        <version>1.0</version>
    </dependency>
~~~
2. 开启总开关 ```sqlhelper.enable=true```
### 1.开启逻辑删除开关 
```sqlhelper.logic-delete.enable=true```
### 2.添加配置。
可以实现[InjectColumnInfoHandler]()但是没必要，Mybatis-Sqlhelper提供了默认的实现可通过配置文件配置。如：
~~~yml
sqlhelper:
  logic-delete:
    enable: true
    sqldemo: update xx set is_deleted = 'Y' where id = 'xx'
    columnName: is_deleted
    not-deleted-value: "'N'"
    ignore-tables:
      - people
    ignore-mapper-ids:
      - com.**.xx
~~~
### 3.观察日志。
物理删除语句已经被自动转换成更新语句，并且保留了所有where条件

## 使用数据权限
[数据权限专篇](./README_DATA_PERMISSION.md)

## 未完待续。。(留下你的start鼓励我完善它)
## 参与贡献
fork项目提交pull request。如果您不会请学习它，它很有用
## 联系我
QQ: 1259609102<br>
email: bigsheller08@gmail.com,1259609102@qq.com