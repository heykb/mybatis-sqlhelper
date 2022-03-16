package io.github.heykb.sqlhelper.h2.tests.cache;

import com.alibaba.fastjson.JSON;
import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDynamicDataSourceProxy;
import io.github.heykb.sqlhelper.h2.dao.PeopleMapper;
import io.github.heykb.sqlhelper.h2.domain.People;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.util.List;

public class CacheTests {
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void setUp() throws Exception {
        // create a SqlSessionFactory
        try (Reader reader = Resources.getResourceAsReader("io/github/heykb/sqlhelper/h2/mybatis-columnFilter.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            Environment environment = sqlSessionFactory.getConfiguration().getEnvironment();
            Environment newEnv = new Environment(environment.getId(),environment.getTransactionFactory(),new SqlHelperDynamicDataSourceProxy(environment.getDataSource()));
            sqlSessionFactory.getConfiguration().setEnvironment(newEnv);
        }
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/people.sql");
    }

    @Test
    void localCacheTest(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper peopleMapper = sqlSession.getMapper(PeopleMapper.class);
            List<People> list = peopleMapper.selectList(new People());
            list = peopleMapper.selectList(new People());
        }
    }

}
