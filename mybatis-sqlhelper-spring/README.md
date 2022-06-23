# MyBatis SqlHelper Spring

[![Maven central](https://maven-badges.herokuapp.com/maven-central/io.github.heykb/mybatis-sqlHelper-spring/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.heykb/mybatis-sqlHelper-spring)

[MyBatis SqlHelper](https://github.com/heykb/mybatis-sqlhelper)的spring集成版本，友好集成了Spring依赖注入的能力，各种注入信息类可通过自动扫描bean添加到插件中。

~~~xml
<dependency>
    <groupId>io.github.heykb</groupId>
    <artifactId>mybatis-sqlHelper-spring</artifactId>
    <version>${version}</version>
</dependency>
~~~

## 关联
1. [MyBatis SqlHelper](https://github.com/heykb/mybatis-sqlhelper)
2. [MyBatis SqlHelper Spring](https://github.com/heykb/mybatis-sqlhelper-spring)
3. [MyBatis SqlHelper Spring Boot](https://github.com/heykb/mybatis-sqlhelper-spring-boot)

## 在applicationContext.xml中如下配置

注意使用[SqlHelperPluginFactoryBean](src/main/java/io/github/heykb/sqlhelper/spring/SqlHelperPluginFactoryBean.java)工厂类创建bean而不是SqlHelperPlugin，[SqlHelperPluginFactoryBean](src/main/java/io/github/heykb/sqlhelper/spring/SqlHelperPluginFactoryBean.java)提供了自动扫描注入信息类的bean的能力，通过其properties属性可以像原始方式一样配置各项参数。SqlHelperPlugin的详细参数参见[MyBatis SqlHelper](https://github.com/heykb/mybatis-sqlhelper)。
~~~xml
<beans>
  <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
    <property name="mapperLocations" value="classpath:io/github/heykb/sqlhelper/spring/primary/dao/*.xml"/>
    <property name="plugins">
      <array>
        <ref bean="sqlHelperPlugin"></ref>
      </array>
    </property>
  </bean>
  <bean id="sqlHelperPlugin" class="io.github.heykb.sqlhelper.spring.SqlHelperPluginFactoryBean">
    <property name="properties">
      <props>
        <prop key="enable">true</prop>
      </props>
    </property>
  </bean>
</beans>
 
~~~
## 使用自动注入等功能，配置相关bean即可。
~~~xml
<bean id="myConditionInfoHandler" class="io.github.heykb.sqlhelper.spring.primary.handlers.MyConditionInfoHandler"></bean>
~~~

## 提供[PropertyLogicDeleteInfoHandler](src/main/java/io/github/heykb/sqlhelper/spring/PropertyLogicDeleteInfoHandler.java)更方便配置逻辑删除
~~~xml
<bean id="propertyLogicDeleteInfoHandler" class="io.github.heykb.sqlhelper.spring.PropertyLogicDeleteInfoHandler">
    <property name="columnName">
      <value>is_deleted</value>
    </property>
    <property name="notDeletedValue">
      <value>'N'</value>
    </property>
    <property name="deleteSqlDemo">
      <value>update xx set is_deleted = 'Y'</value>
    </property>
    <property name="ignoreMapperIds">
      <list>
        <value>**.noPlugin*</value>
      </list>
    </property>
    <property name="ignoreTables">
      <list>
        <value>people</value>
      </list>
    </property>
  </bean>
~~~

## 使用多数据源
[多数据源专篇](./DYNAMIC_DATASOURCE_README.md)
## 未完待续。。(如果你有兴趣，右上角watch该项目获得最新的动态)
 
## 参与贡献

如果你发现问题，提交issue。

如果你解决了问题，fork项目提交pull request。

## 联系我
QQ: 1259609102<br>
email: bigsheller08@gmail.com,1259609102@qq.com