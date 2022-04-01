package io.github.heykb.sqlhelper.h2.tests.inject;

import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDynamicDataSourceProxy;
import io.github.heykb.sqlhelper.h2.dao.PeopleMapper;
import io.github.heykb.sqlhelper.h2.domain.People;
import io.github.heykb.sqlhelper.h2.handlers.UpdateByInjectColumnHandler;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class InjectTests {
    private static SqlSessionFactory sqlSessionFactory;
    @BeforeAll
    static void setUp() throws Exception {
        // create a SqlSessionFactory
        try (Reader reader = Resources.getResourceAsReader("io/github/heykb/sqlhelper/h2/tests/inject/mybatis-inject.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/people.sql");
    }

    @Test
    void insertTest(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            demoMapper.insert(People.builder().id("c_4").name("tom").age(10).updatedBy("888").build());
            People people1 = demoMapper.select("c_4");
            Assertions.assertTrue(UpdateByInjectColumnHandler.value.equals(people1.getUpdatedBy()));
        }
    }

    @Test
    void updateTest(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            demoMapper.update(People.builder().id("1").name("tom").updatedBy("888").age(10).build());
            People people1 = demoMapper.select("1");
            Assertions.assertTrue(UpdateByInjectColumnHandler.value.equals(people1.getUpdatedBy()));
        }
    }

    @Test
    void batchInsertTest(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            List<People> people = new ArrayList<>();
            people.add(People.builder().id("c_5").name("tom").updatedBy("888").age(10).build());
            people.add(People.builder().id("c_6").name("ccc").age(10).build());
            demoMapper.batchInsert(people);
            People people1 = demoMapper.select("c_5");
            Assertions.assertTrue(UpdateByInjectColumnHandler.value.equals(people1.getUpdatedBy()));
            people1 = demoMapper.select("c_6");
            Assertions.assertTrue(UpdateByInjectColumnHandler.value.equals(people1.getUpdatedBy()));
        }
    }
}
