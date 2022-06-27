package io.github.heykb.sqlhelper.condition;

import com.google.common.collect.Sets;
import io.github.heykb.sqlhelper.BaseUtils;
import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.handler.abstractor.TenantInfoHandler;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.*;

public class BaseTest {
    @Mapper
    public interface TestMapper{
        @Select("select * from people where name=#{name}")
        List<Map> selectByName(@Param("name") String name);
        @Select("select * from people")
        List<Map> select();
        @Options(resultSetType = ResultSetType.FORWARD_ONLY)
        @Select("select * from people")
        Cursor<Map> selectCursor();
        @Select("select * from people")
        List<Map> select2();

        @Insert("INSERT INTO people(id,name,age) VALUES (#{id},#{name},#{age})")
        int insert(@Param("id") String id, @Param("name")String name,@Param("age")int age);

        @Update("update people set name=#{name},updated_time=now() where id=#{id}")
        int update(@Param("id") String id,@Param("name")String name);

        @Delete("delete from people where name=#{name}")
        int delete(@Param("name") String name);
    }
    private static DataSource dataSource;
    @BeforeAll
    static void setUp() throws Exception {
        dataSource = new UnpooledDataSource("org.hsqldb.jdbcDriver","jdbc:hsqldb:mem:automapping","sa",null);

    }

    @Test
    void conditionInject() throws Exception {
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, Arrays.asList(new InjectColumnInfoHandler() {
                    @Override
                    public String getColumnName() {
                        return "age";
                    }

                    @Override
                    public String getValue() {
                        return "18";
                    }

                    @Override
                    public String op() {
                        return ">";
                    }

                    @Override
                    public int getInjectTypes() {
                        return CONDITION;
                    }
                }),null);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            List<Map> result =  testMapper.select();
            Assertions.assertTrue(result.size()==1);
        }
    }

    @Test
    void insertInject() throws Exception{
        List<InjectColumnInfoHandler> injectColumnInfoHandlers = Arrays.asList(
                new InjectColumnInfoHandler() {
                   @Override
                   public String getColumnName() {
                       return "name";
                   }
                   @Override
                   public String getValue() {
                       return "'insertInject'";
                   }

                   @Override
                   public int getInjectTypes() {
                       return INSERT;
                   }
               });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, injectColumnInfoHandlers,null);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            int re =  testMapper.insert(UUID.randomUUID().toString(),null,19);
            Assertions.assertTrue(re==1);
            List<Map> list = testMapper.selectByName("insertInject");
            Assertions.assertTrue(list.size() == 1);
        }
    }

    @Test
    void updateInject() throws Exception{
        List<InjectColumnInfoHandler> injectColumnInfoHandlers = Arrays.asList(
                new InjectColumnInfoHandler() {
                    @Override
                    public String getColumnName() {
                        return "name";
                    }
                    @Override
                    public String getValue() {
                        return "'updateInject'";
                    }
                    @Override
                    public int getInjectTypes() {
                        return UPDATE;
                    }
                });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, injectColumnInfoHandlers,null);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            int re =  testMapper.update("1","");
            Assertions.assertTrue(re==1);
            List<Map> list = testMapper.selectByName("updateInject");
            Assertions.assertTrue(list.size() == 1);
        }
    }
    @Test
    void logicDelete() throws Exception{
        List<InjectColumnInfoHandler> injectColumnInfoHandlers = Arrays.asList(
                new LogicDeleteInfoHandler() {

                    @Override
                    public String getColumnName() {
                        return "is_deleted";
                    }

                    @Override
                    public String getDeleteSqlDemo() {
                        return "update a set is_deleted='Y'";
                    }

                    @Override
                    public String getNotDeletedValue() {
                        return "'N'";
                    }
                });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, injectColumnInfoHandlers,null);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            List<Map> list = testMapper.selectByName("tom");
            Assertions.assertTrue(list.size() > 0);
            int re =  testMapper.delete("tom");
            Assertions.assertTrue(re==1);
            list = testMapper.selectByName("tom");
            Assertions.assertTrue(list.size() == 0);
        }
    }

    @Test
    void tenantTest() throws Exception{
        List<InjectColumnInfoHandler> injectColumnInfoHandlers = Arrays.asList(
                new TenantInfoHandler() {
                    @Override
                    public String getTenantIdColumn() {
                        return "tenant_id";
                    }
                    @Override
                    public String getTenantId() {
                        return "'new_tenant'";
                    }
                });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, injectColumnInfoHandlers,null);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            List<Map> list = testMapper.selectByName("tom");
            Assertions.assertTrue(list.size()==0);
            String id = UUID.randomUUID().toString();
            int re = testMapper.insert(id,"tom",18);
            Assertions.assertTrue(re==1);
            list = testMapper.selectByName("tom");
            Assertions.assertTrue(list.size()==1);
            testMapper.update(id,"tenantTest");
            list = testMapper.selectByName("tom");
            Assertions.assertTrue(list.size()==0);
            list = testMapper.selectByName("tenantTest");
            Assertions.assertTrue(list.size()==1);
            testMapper.delete("tenantTest");
            list = testMapper.selectByName("tenantTest");
            Assertions.assertTrue(list.size()==0);
        }
    }


    @Test
    @DisplayName("结果集过滤")
    void columnFilter1() throws Exception{
        List<ColumnFilterInfoHandler> columnFilterInfoHandlers = Arrays.asList(
                new ColumnFilterInfoHandler() {
                    @Override
                    public Set<String> getFilterColumns() {
                        return Sets.newHashSet("name");
                    }
                    @Override
                    public boolean checkTableName(String tableName) {
                        return true;
                    }
                });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, null,columnFilterInfoHandlers);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            List<Map> list = testMapper.select();
            for(Map map:list){
                Assertions.assertTrue(map.get("NAME")==null);
            }
        }
    }

    @Test
    @DisplayName("sql过滤")
    void columnFilter2() throws Exception{
        List<ColumnFilterInfoHandler> columnFilterInfoHandlers = Arrays.asList(
                new ColumnFilterInfoHandler() {
                    @Override
                    public Set<String> getFilterColumns() {
                        return Sets.newHashSet("name");
                    }
                    @Override
                    public boolean checkTableName(String tableName) {
                        return true;
                    }
                });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, null,columnFilterInfoHandlers);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            List<Map> list = testMapper.select2();
            for(Map map:list){
                Assertions.assertTrue(map.get("NAME")==null);
            }
        }
    }


    @Test
    @DisplayName("结果集过滤 cursor游标支持")
    void columnFilter3() throws Exception{
        List<ColumnFilterInfoHandler> columnFilterInfoHandlers = Arrays.asList(
                new ColumnFilterInfoHandler() {
                    @Override
                    public Set<String> getFilterColumns() {
                        return Sets.newHashSet("name");
                    }
                    @Override
                    public boolean checkTableName(String tableName) {
                        return true;
                    }
                });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, null,columnFilterInfoHandlers);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            Cursor<Map> list = testMapper.selectCursor();
            for(Map map:list){
                Assertions.assertTrue(map.get("NAME")==null);
            }
        }
    }

    @Test
    @DisplayName("update 列过滤")
    void columnFilter4() throws Exception{
        List<ColumnFilterInfoHandler> columnFilterInfoHandlers = Arrays.asList(
                new ColumnFilterInfoHandler() {
                    @Override
                    public Set<String> getFilterColumns() {
                        return Sets.newHashSet("name");
                    }
                    @Override
                    public boolean checkTableName(String tableName) {
                        return true;
                    }
                    @Override
                    public boolean checkCommandType(SqlCommandType commandType) {
                        return commandType == SqlCommandType.UPDATE;
                    }
                });
        SqlSessionFactory sqlSessionFactory = BaseUtils.generateSqlSessionFactory(dataSource, "io/github/heykb/sqlhelper/condition/baseTest.sql",
                TestMapper.class, null,columnFilterInfoHandlers);
        try(SqlSession sqlSession =sqlSessionFactory.openSession()){
            TestMapper testMapper = sqlSession.getMapper(TestMapper.class);
            testMapper.update("1","newName");
            List<Map> re = testMapper.selectByName("newName");
            Assertions.assertTrue(re.size()==0);
        }
    }

}
