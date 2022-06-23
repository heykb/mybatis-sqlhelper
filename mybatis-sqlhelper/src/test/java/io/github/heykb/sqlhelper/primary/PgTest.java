package io.github.heykb.sqlhelper.primary;

import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.h2.dao.PeopleMapper;
import io.github.heykb.sqlhelper.h2.domain.People;
import io.github.heykb.sqlhelper.h2.handlers.UpdateByInjectColumnHandler;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Reader;

public class PgTest {
    private static SqlSessionFactory sqlSessionFactory;
    @BeforeAll
    static void setUp() throws Exception {
        // create a SqlSessionFactory
        try (Reader reader = Resources.getResourceAsReader("io/github/heykb/sqlhelper/primary/mybatis.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/people.sql");
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
    void deleteTest(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            demoMapper.delete("1");
        }
    }
}
