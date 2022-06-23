package io.github.heykb.sqlhelper.spring.primary;

import io.github.heykb.sqlhelper.config.SqlHelperException;
import io.github.heykb.sqlhelper.dynamicdatasource.LogicDsMeta;
import io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDsManager;
import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.spring.primary.dao.EmployeeMapper;
import io.github.heykb.sqlhelper.spring.primary.dao.PeopleMapper;
import io.github.heykb.sqlhelper.spring.primary.domain.People;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(locations = "classpath:io/github/heykb/sqlhelper/spring/primary/applicationContext.xml")
public class PluginTests {
    private Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired(required = false)
    private SqlHelperDsManager sqlHelperDsManager;


    @Test
    @DisplayName("简单select查询")
    void selectTest(){
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            List<People> list = demoMapper.selectList(People.builder().name("tom").build());
            Assertions.assertEquals(list.size(),1);
        }
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

    @Test
    @DisplayName("多数据源注入测试")
    void multiDatasourceTest(){
        Assertions.assertThrows(SqlHelperException.class,()->sqlHelperDsManager.remove("xxx"));
    }

}
