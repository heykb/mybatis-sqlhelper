package io.github.heykb.sqlhelper.h2.tests.columnFilter;

import com.alibaba.fastjson.JSON;
import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDynamicDataSourceProxy;
import io.github.heykb.sqlhelper.h2.dao.PeopleMapper;
import io.github.heykb.sqlhelper.h2.domain.People;
import org.apache.ibatis.cursor.Cursor;
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

public class ColumnFilterTests {

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
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/department.sql");

    }

    @Test
    @DisplayName("sql方式过滤列测试")
    void filterFieldTest() throws IOException, SQLException {

        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper peopleMapper = sqlSession.getMapper(PeopleMapper.class);
            List<People> list = peopleMapper.selectList(new People());
            System.out.println(JSON.toJSONString(list));
            Assertions.assertNull(list.get(0).getName());
        }
    }
    @Test
    @DisplayName("结果集过滤列测试")
    void filterFieldTest2() throws IOException, SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper peopleMapper = sqlSession.getMapper(PeopleMapper.class);
            List<People> list = peopleMapper.selectList2(new People());
            Assertions.assertNull(list.get(0).getName());
        }
    }

    @Test
    @DisplayName("Cursor结果集过滤列测试")
    void filterFieldCursorTest() throws IOException, SQLException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper peopleMapper = sqlSession.getMapper(PeopleMapper.class);
            Cursor<People> list = peopleMapper.selectCursor(new People());
            list.forEach(item->{
                Assertions.assertNull(item.getName());
            });
        }
    }

}
