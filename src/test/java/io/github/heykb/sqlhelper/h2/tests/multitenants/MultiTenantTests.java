package io.github.heykb.sqlhelper.h2.tests.multitenants;


import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDynamicDataSourceProxy;
import io.github.heykb.sqlhelper.h2.domain.People;
import io.github.heykb.sqlhelper.h2.dao.PeopleMapper;
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
public class MultiTenantTests {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void setUp() throws Exception {
        // create a SqlSessionFactory
        try (Reader reader = Resources.getResourceAsReader("io/github/heykb/sqlhelper/h2/mybatis-multitenants.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/people.sql");
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/department.sql");
    }

    @Test
    @DisplayName("简单select查询")
    void selectTest() throws IOException, SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            List<People> list = demoMapper.selectList(People.builder().name("tom").build());
            Assertions.assertEquals(list.size(),1);
        }
    }
    @Test
    @DisplayName("left join select查询")
    void joinSelectTest() throws IOException, SQLException {
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
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            String id = UUID.randomUUID().toString();
            demoMapper.insert(People.builder().name("sqlhelper").id(id).build());
            List<People> list1 = demoMapper.noPluginSelect(People.builder().id(id).build());
            Assertions.assertTrue(SimpleTenantInfoHandler.TENANT_ID.equals(list1.get(0).getTenantId()));
        }

    }

}
