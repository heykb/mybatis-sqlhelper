package io.github.heykb.sqlhelper.h2.tests.multidatasource;


import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.dynamicdatasource.*;
import io.github.heykb.sqlhelper.h2.dao.PeopleMapper;
import io.github.heykb.sqlhelper.h2.domain.People;
import io.github.heykb.sqlhelper.h2.handlers.SimpleTenantInfoHandler;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@DisplayName("多租户测试")
public class MultiDatasourceTests {

    private static SqlSessionFactory sqlSessionFactory;
    private static DefaultSqlHelperDsManager sqlHelperDsManager;

    @BeforeAll
    static void setUp() throws Exception {
        // create a SqlSessionFactory 主数据源postgre
        try (Reader reader = Resources.getResourceAsReader("io/github/heykb/sqlhelper/primary/mybatis.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            Environment environment = sqlSessionFactory.getConfiguration().getEnvironment();
            SqlHelperDynamicDataSourceProxy dataSourceProxy = new SqlHelperDynamicDataSourceProxy(environment.getDataSource());
            sqlHelperDsManager = dataSourceProxy.getSqlHelperDsManager();
            Environment newEnv = new Environment(environment.getId(), environment.getTransactionFactory(), dataSourceProxy);
            sqlSessionFactory.getConfiguration().setEnvironment(newEnv);
        }

//        // 添加mysql数据源
//        LogicDsMeta mysqlDs = LogicDsMeta.builder()
//                .datasourceId("localhost:3306")
//                .createFunc(s -> new PooledDataSource("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/mysql", "root", "123456"))
//                .expectedSubspaceType(ConnectionSubspaceTypeEnum.DATABASE)
//                .build();
//        sqlHelperDsManager.put("mysql", mysqlDs);
//        // 添加sqlserver数据源
//        LogicDsMeta sqlserverDs = LogicDsMeta.builder()
//                .datasourceId("localhost:1433")
//                .createFunc(s -> new PooledDataSource("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://localhost;databaseName=test", "sa", "Bdy123456"))
//                .build();
//        sqlHelperDsManager.put("sqlserver", sqlserverDs);

        LogicDsMeta primarySubDs = LogicDsMeta.builder()
                .datasourceId(DefaultSqlHelperDsManager.PRIMARY_DATASOURCE_ID)
                .subspace("test")
                .expectedSubspaceType(ConnectionSubspaceTypeEnum.SCHEMA)
                .build();
        sqlHelperDsManager.put("primary_sub", primarySubDs);

        SqlHelperDsContextHolder.executeOn("primary_sub",()->{
            BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                    "io/github/heykb/sqlhelper/primary/db/postgre.sql");
            return null;
        });


        // 初始化postgre数据源
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/primary/db/postgre.sql");

//        // 初始化mysql数据源
//        SqlHelperDsContextHolder.switchTo("mysql");
//        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
//                "io/github/heykb/sqlhelper/primary/db/mysql.sql");

    }
    void simpleSelect(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            List<People> list = demoMapper.selectList(People.builder().name("tom").build());
            Assertions.assertEquals(list.size(), 1);
        }
    }

    @Test
    @DisplayName("简单select查询")
    void selectTest(){
        simpleSelect();
    }

    @Test
    @DisplayName("left join select查询")
    void joinSelectTest() {

        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            People people = demoMapper.leftJoinSelect("1");
            Assertions.assertNotNull(people);
            people = demoMapper.leftJoinSelect("2");
            Assertions.assertNull(people);
        }
    }

    @Test
    @DisplayName("insert")
    void insertTest() throws IOException, SQLException {
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/primary/db/postgre.sql");
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            String id = UUID.randomUUID().toString();
            demoMapper.insert(People.builder().name("sqlhelper").id(id).build());
            List<People> list = demoMapper.noPluginSelect(People.builder().id(id).build());
            Assertions.assertEquals(list.get(0).getTenantId(), SimpleTenantInfoHandler.TENANT_ID);
        }

    }

    @Test
    void mysqlSubspaceChangeTest() throws IOException, SQLException {
        SqlHelperDsContextHolder.switchTo("mysql");
        simpleSelect();
        sqlHelperDsManager.put("mysql_test", LogicDsMeta.builder().datasourceId("localhost:3306").subspace("test").expectedSubspaceType(ConnectionSubspaceTypeEnum.DATABASE).build());
        SqlHelperDsContextHolder.switchTo("mysql_test");
        Assertions.assertThrows(Exception.class,this::simpleSelect);
    }


    @Test
    void sqlserverSubspaceChangeTest() throws IOException, SQLException {
        SqlHelperDsContextHolder.switchTo("sqlserver");
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/sqlservertest.sql");

    }

}
