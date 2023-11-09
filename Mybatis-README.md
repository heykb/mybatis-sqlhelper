# MyBatis原生配置使用方式

[![Maven central](https://maven-badges.herokuapp.com/maven-central/io.github.heykb/mybatis-sqlHelper/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.heykb/mybatis-sqlHelper)

如果您正在使用MyBatis，并且您的项目需要<b>多租户、逻辑删除和数据权限、多数据源</b>的功能建议您使用，这一定是<b>最方便的</b>方式，使用它对您的现有代码没有侵入，您不需要对现有代码进行任何修改。

~~~xml
    <dependency>
        <groupId>io.github.heykb</groupId>
        <artifactId>mybatis-sqlHelper</artifactId>
        <version>${project.version}</version>
    </dependency>
~~~
## 使用快照版本(及时bug修复版本)
~~~xml
<dependency>
    <groupId>io.github.heykb</groupId>
    <artifactId>mybatis-sqlHelper</artifactId>
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

### CONDITION条件注入
条件注入， 创建类实现[InjectColumnInfoHandler](src/main/java/io/github/heykb/sqlhelper/handler/InjectColumnInfoHandler.java)，如：
~~~java
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
配置插件
~~~xml
<plugins>
    <plugin interceptor="io.github.heykb.sqlhelper.interceptor.SqlHelperPlugin">
      <property name="InjectColumnInfoHandler"
                value="io.github.heykb.sqlhelper.primary.handlers.MyConditionInfoHandler"/>
    </plugin>
  </plugins>
~~~


## 可用的属性

| 名称                         | 类型                           | 默认 | 描述                                                             | demo         |
| ------------------------------ | -------------------------------- | ---- | ------------------------------------------------------------------ | ------------ |
| enable                         | boolean                          | true | 用于设置功能的总开关。                                  | true         |
| multi-tenant.enable            | boolean                          | true | 用于设置多租户功能开关。                               | true         |
| logic-delete.enable            | boolean                          | true | 用于设置物理删除转逻辑删除功能开关。             | true         |
| ~~dbType~~                        | String(com.alibaba.druid.DbType) |      | 用于设置数据库类型的参数名称，非特殊不用配置，支持自动获取 | mysql        |
| InjectColumnInfoHandler        | String(逗号分割的字符串数组) |      | sql注入信息全限定类名数组。被用于反射生成注入信息对象 | com.xx,io.xx |
| ColumnFilterInfoHandler        | String(逗号分割的字符串数组) |      | 数据权限中的字段过滤信息全限定类名数组。被用于反射生成注入信息对象 | com.xx,io.xx |
| DynamicFindInjectInfoHandler   | String                           |      | 运行期间动态生成注入信息对象集合的全限定类名。 | com.xx       |
| DynamicFindColumnFilterHandler | String                           |      | 运行期间动态生成字段过滤信息对象集合的全限定类名。 | com.xx       |

