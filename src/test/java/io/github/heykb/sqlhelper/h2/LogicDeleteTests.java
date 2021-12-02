package io.github.heykb.sqlhelper.h2;

import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.h2.dao.EmployeeMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Reader;

public class LogicDeleteTests {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void setUp() throws Exception {
        // create a SqlSessionFactory
        try (Reader reader = Resources.getResourceAsReader("io/github/heykb/sqlhelper/h2/mybatis.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/logicDeleteTest.sql");
    }

    @Test
    @DisplayName("转逻辑删除测试")
    void logicDeleteTest(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            EmployeeMapper demoMapper = sqlSession.getMapper(EmployeeMapper.class);
            int count1 = demoMapper.count();
            demoMapper.deleteById(1);
            int count2 = demoMapper.count();
            Assertions.assertEquals(count1,count2+1);
            int count3 = demoMapper.noPluginCount();
            Assertions.assertEquals(count1,count3);
        }
    }


}
