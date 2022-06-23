# 多数据源专篇
 * 动态新增数据源，数据源管理能力
 * 动态切换数据源
 * 支持连接子空间，如mysql 一个连接支持切换不同数据库，可以配置不同的逻辑数据源名称（添加时设置不同的数据源名称，相同的数据源id）但是使用同一个数据源连接池，使用过程中我们会帮你自动切换到指定子空间
  

## 使用多数据源功能，配置SpringSqlHelperDsManager数据源管理bean即可开始使用。
xml配置
~~~xml
  <bean id="sqlHelperDsManager" class = "io.github.heykb.sqlhelper.spring.dynamicds.SpringSqlHelperDsManager"></bean>
~~~
spring boot配置
~~~java
    @Bean
    public SqlHelperDsManager springSqlHelperDsManager(){
        return new SpringSqlHelperDsManager();
    }
~~~
然后在程序中使用SqlHelperDsManager新增添加数据源
~~~java
    @Autowired
    private SqlHelperDsManager sqlHelperDsManager;
~~~
~~~java
sqlHelperDsManager.put("mysql", LogicDsMeta.builder()
      .datasourceId("localhost:3306")
      .expectedSubspaceType(ConnectionSubspaceTypeEnum.DATABASE)
      .createFunc(s->new PooledDataSource("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/test", "root", "123456")).build());
~~~
LogicDsMeta新增数据源参数解释
~~~java
/**
     * 数据源id,当已注册数据源中存在相同id时会复用数据源，并且会触发数据源升级回调方法如果存在的话。
     * 使用同一个数据源id的不同逻辑数据源可以设置不同的subspace子空间。如mysql支持同一个连接切换不同数据库
     */
    private String datasourceId;
    /**
     * 设置期望的子空间类型（不是所有的数据库都支持同一个连接进行切换)
     * 主要作用在于当用户期望和软件支持不匹配能快速失败及时报错
     */
    private ConnectionSubspaceTypeEnum expectedSubspaceType;
    /**
     * 当逻辑数据源使用时将连接切换到指定的子空间。子空间的名称。仅当数据库类型支持子空间时有效
     * nullable
     */
    private String subspace;
    /**
     * 当数据源id不存在时，使用此回调创建新的数据源
     */
    private Function<Void, DataSource> createFunc;
~~~
切换数据源
~~~java
SqlHelperDsContextHolder.switchTo("mysql");

// 返回上一次设置
SqlHelperDsContextHolder.backToLast();

// 清空切换栈，使用主数据源
SqlHelperDsContextHolder.clear();

// 切换到主数据源
SqlHelperDsContextHolder.switchTo(null)
~~~

## 未完待续。。(如果你有兴趣，右上角watch该项目获得最新的动态)
 
## 参与贡献

如果你发现问题，提交issue。

如果你解决了问题，fork项目提交pull request。

## 联系我
QQ: 1259609102<br>
email: bigsheller08@gmail.com,1259609102@qq.com