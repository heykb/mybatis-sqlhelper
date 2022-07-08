package io.github.heykb.sqlhelper.test;

import io.github.heykb.sqlhelper.dynamicdatasource.*;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@DisplayName("动态数据源")
public class DynamicDatasourceTest {

    static SqlSessionFactory sqlSessionFactory;
    static DefaultSqlHelperDsManager sqlHelperDsManager;
    @BeforeAll
    static void setUp() throws Exception {
        DataSource dataSource = new UnpooledDataSource("org.hsqldb.jdbcDriver","jdbc:hsqldb:mem:automapping","sa",null);
        SqlHelperDynamicDataSourceProxy dataSourceProxy = new SqlHelperDynamicDataSourceProxy(dataSource);
        sqlHelperDsManager = dataSourceProxy.getSqlHelperDsManager();
        sqlHelperDsManager.put("ds2", LogicDsMeta.builder()
                .expectedSubspaceType(ConnectionSubspaceTypeEnum.NOT_SUPPORT)
                .datasourceId("ds2").createFunc(()->{
                    return new UnpooledDataSource("org.hsqldb.jdbcDriver","jdbc:hsqldb:mem:automapping2","sa",null);
                }).build());
        sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSourceProxy, "io/github/heykb/sqlhelper/test/baseTest.sql",
                BaseTest.TestMapper.class, null,null);
        BaseUtils.runScript(sqlHelperDsManager.getByDatasourceId("ds2"),"io/github/heykb/sqlhelper/test/baseTest.sql");
    }

    @Test
    void cacheTest() throws Exception {
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            BaseTest.TestMapper testMapper = sqlSession.getMapper(BaseTest.TestMapper.class);
            List<Map> result =  testMapper.selectByName("tom");

        }
        SqlHelperDsContextHolder.switchTo("ds2");
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            BaseTest.TestMapper testMapper = sqlSession.getMapper(BaseTest.TestMapper.class);
            List<Map> result =  testMapper.selectByName("tom");
//            Assertions.assertTrue(result.size()>0);
        }
    }
}
