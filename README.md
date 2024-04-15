# MyBatis 多租户、逻辑删除、数据权限插件、多数据源-SqlHelper(持续维护，放心使用)

[![Maven central](https://maven-badges.herokuapp.com/maven-central/io.github.heykb/mybatis-sqlHelper/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.heykb/mybatis-sqlHelper)

如果您正在使用MyBatis，并且您的项目需要<b>多租户、逻辑删除和数据权限、多数据源</b>的功能建议您使用，这一定是<b>最方便的</b>方式，使用它对您的现有代码没有侵入，您不需要对现有代码进行任何修改。

~~~xml
    <dependency>
        <groupId>io.github.heykb</groupId>
        <artifactId>mybatis-sqlhelper-spring-boot-starter</artifactId>
        <version>${project.version}</version>
    </dependency>
~~~
## 使用快照版本(及时bug修复版本)
~~~xml
<dependency>
    <groupId>io.github.heykb</groupId>
    <artifactId>mybatis-sqlhelper-spring-boot-starter</artifactId>
    <version>3.0.0.SR2-SNAPSHOT</version>
</dependency>

<repositories>
        <repository>    
            <id>sonatype-nexus-snapshots</id>
            <name>Sonatype Nexus Snapshots</name>
            <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
            <releases>
                <enabled>false</enabled>
            </releases>
        </repository>
    </repositories>
~~~
## 特点
* 可实现多租户
* 可实现真实删除转逻辑删除
* 可实现行级别和列级别权限控制（自动解析注入）
* 简单方便开箱即用的多数据源管理和切换
* 可插拔
* 简单方便
* 高效率（基于[阿里 druid sql解析器](https://github.com/alibaba/druid/wiki/SQL-Parser)）
* 将多租户、逻辑删除与应用程序解耦，随配随用
* 强大的字段自动注入能力（<i>查询条件注入/插入语句注入/更新语句注入/查询列过滤</i>），定制其他业务逻辑
* 支持多种数据库（基于阿里 druid sql解析器）
* 超简单即可拥有多数据管理和切换

## spring 集成
1. [MyBatis SqlHelper Spring](./mybatis-sqlhelper-spring/README.md)
2. [MyBatis SqlHelper Spring Boot](./README_SPRING_BOOT.md)

### [查看博客戳这里 👆](https://heykb.github.io)

## 使用数据权限
[数据权限专篇](./README_DATA_PERMISSION.md)

## 使用多数据源
[多数据源专篇](./DYNAMIC_DATASOURCE_README.md)
## 注入示例
[注入示例](./sql-demo.md)

## sql自动注入
### CONDITION条件注入
1. 单一条件注入， 创建类实现[InjectColumnInfoHandler](mybatis-sqlhelper/src/main/java/io/github/heykb/sqlhelper/handler/InjectColumnInfoHandler.java)，如：
~~~java
@Component
public class MyConditionInfoHandler implements InjectColumnInfoHandler {
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

    @Override
    public boolean checkMapperId(String mapperId) {
        return true;
    }
}
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
[查看更多测试示例](./sql-demo.md)<br>
2. 多条件组合注入继承[BinaryConditionInjectInfoHandler](mybatis-sqlhelper/src/main/java/io/github/heykb/sqlhelper/handler/abstractor/BinaryConditionInjectInfoHandler.java)
```java
/**
 * @author heykb
 * 注入： a = 1 and (b > 1 or c = 'x')
 */
@Component
public class MyBinaryConditionInjectInfoHandler extends BinaryConditionInjectInfoHandler {

    @Override
    public String op() {
        return "and";
    }

    @Override
    public InjectColumnInfoHandler getLeftConditionInjectInfo() {
        return new SimpleCondition("a","=","1");
    }

    @Override
    public InjectColumnInfoHandler getRightConditionInjectInfo() {
        return new BinaryConditionInjectInfoHandler() {
            @Override
            public InjectColumnInfoHandler getLeftConditionInjectInfo() {
                return new SimpleCondition("b",">","1");
            }

            @Override
            public String op() {
                return "or";
            }

            @Override
            public InjectColumnInfoHandler getRightConditionInjectInfo() {
                return new SimpleCondition("c","=","'x'");
            }
        };
    }
    
    static class SimpleCondition implements InjectColumnInfoHandler{
        String columnName;
        String op;
        String value;

        public SimpleCondition(String columnName, String op, String value) {
            this.columnName = columnName;
            this.op = op;
            this.value = value;
        }

        @Override
        public String getColumnName() {
            return columnName;
        }

        @Override
        public String op() {
            return op;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public int getInjectTypes() {
            return 0;
        }
    }
}
```


### INSERT插入列注入  如自动插入租户id列
实现[InjectColumnInfoHandler](mybatis-sqlhelper/src/main/java/io/github/heykb/sqlhelper/handler/InjectColumnInfoHandler.java)，如:<br>
~~~java
@Component
public class MyInsertInfoHandler implements InjectColumnInfoHandler {
        
        @Override
        public String getColumnName() {
            return "tenant_id";
        }
        @Override
        public String getValue() {
            return "'sqlhelper'";
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

[查看更多测试示例](./sql-demo.md)<br>

### UPDATE更新项注入...如自动更新updated_time列
~~~java
@Override
public int getInjectTypes() {
    return UPDATE;
}
~~~

### 同时多种类型注入
~~~java
@Override
public int getInjectTypes() {
    return UPDATE|INSERT|...;
}
~~~
## [查看更多测试示例](./sql-demo.md)


## 字段隔离的多租户（数据源隔离级别参考sqlhelper多数据源配置）
### 能帮你做什么？
    1. 自动为所有where 、join on添加租户过滤条件
    2. 自动为insert语句添加租户列的插入
    3. 多租户的实现也是利用sqlhelper的自动注入功能，相当于配置了CONDITIO与INSERT的两种注入
### 创建注入类 
~~~
创建类继承[TenantInfoHandler](mybatis-sqlhelper/src/main/java/io/github/heykb/sqlhelper/handler/abstractor/TenantInfoHandler.java)，如：
~~~java
@Component
public class SimpleTenantInfoHandler extends TenantInfoHandler {

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



## 物理删除切换逻辑删除
### 能帮你做什么？
对于删除sql自动转逻辑删除,查询和更新sql添加逻辑删除条件保证不会查询到已经删除的数据，支持多表删除等复杂情况，详情可以查看 [更多测试示例](./sql-demo.md)<br>

创建类继承[LogicDeleteInfoHandler](mybatis-sqlhelper/src/main/java/io/github/heykb/sqlhelper/handler/abstractor/LogicDeleteInfoHandler.java)
~~~java
@Component
public class SimpleLogicDeleteInfoHandler extends LogicDeleteInfoHandler {
    // 主要是为了从中提取SET is_deleted = 'Y'
    @Override
    public String getDeleteSqlDemo() {
        return "UPDATE xx SET is_deleted = 'Y'";
    }

    @Override
    public String getNotDeletedValue() {
        return "'N'";
    }

    @Override
    public String getColumnName() {
        return "is_deleted";
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return !mapperId.contains("noPlugin");
    }
}
~~~
### 3.观察日志。
物理删除语句已经被自动转换成更新语句，并且保留了所有where条件。查询和更新也自动添加了过滤条件


## 未完待续。。(如果你有兴趣，右上角watch该项目获得最新的动态)
 
## 参与贡献

如果你发现问题，提交issue。

如果你解决了问题，fork项目提交pull request。

## 联系我
QQ群: 947964874<br>
email: 1259609102@qq.com,bigsheller08@gmail.com
