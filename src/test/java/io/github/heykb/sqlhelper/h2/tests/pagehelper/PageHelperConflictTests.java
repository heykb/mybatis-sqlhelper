package io.github.heykb.sqlhelper.h2.tests.pagehelper;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import io.github.heykb.sqlhelper.BaseDataUtils;
import io.github.heykb.sqlhelper.h2.dao.PeopleMapper;
import io.github.heykb.sqlhelper.h2.domain.People;
import io.github.heykb.sqlhelper.h2.handlers.SimpleColumnFilterHandler;
import io.github.heykb.sqlhelper.h2.handlers.SimpleTenantInfoHandler;
import io.github.heykb.sqlhelper.interceptor.SqlHelperPlugin;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

@DisplayName("pageHelper冲突测试")
public class PageHelperConflictTests {


    static SqlSessionFactory setUp(boolean pageFirst) throws Exception {
        SqlSessionFactory sqlSessionFactory = null;
        // create a SqlSessionFactory
        try (Reader reader = Resources.getResourceAsReader("io/github/heykb/sqlhelper/h2/mybatis-pagehelper.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        }
        BaseDataUtils.runScript(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
                "io/github/heykb/sqlhelper/h2/db/people.sql");
        PageInterceptor pageInterceptor = new PageInterceptor();
        SqlHelperPlugin sqlHelperPlugin = new SqlHelperPlugin();
        sqlHelperPlugin.setInjectColumnInfoHandlers(Arrays.asList(new SimpleTenantInfoHandler()));
        sqlHelperPlugin.setColumnFilterInfoHandlers(Arrays.asList(new SimpleColumnFilterHandler()));
        if(pageFirst){
            sqlSessionFactory.getConfiguration().addInterceptor(pageInterceptor);
            sqlSessionFactory.getConfiguration().addInterceptor(sqlHelperPlugin);
        }else{
            sqlSessionFactory.getConfiguration().addInterceptor(sqlHelperPlugin);
            sqlSessionFactory.getConfiguration().addInterceptor(pageInterceptor);
        }
       return sqlSessionFactory;

    }



    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void test(boolean isPagePluginFirst) throws Exception {
        // 测试sqlhelper插件与pagehelper插件注册前后顺序的影响。需要将sqlhelper先注册也就是运行期间后运行
        SqlSessionFactory sqlSessionFactory = setUp(isPagePluginFirst);
        try (SqlSession sqlSession = sqlSessionFactory.openSession();) {
            PeopleMapper demoMapper = sqlSession.getMapper(PeopleMapper.class);
            PageHelper.startPage(1,5);
            List<People> list = demoMapper.selectList(People.builder().build());
        }
    }



}