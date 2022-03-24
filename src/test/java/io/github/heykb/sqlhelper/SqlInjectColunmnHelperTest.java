package io.github.heykb.sqlhelper;

import com.alibaba.druid.DbType;
import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;
import io.github.heykb.sqlhelper.helper.SqlStatementEditor;
import org.apache.commons.compress.utils.Sets;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.Arrays;
import java.util.Set;

public class SqlInjectColunmnHelperTest {
    public static void main(String[] args) {
//        String sql = "select concat(name ,id) as \"x\" ,\"name\",s.id_s from user s where u.name='123'";
//        String sql = "select * from user s where s.name='333'";
//        String sql = "select name,s.xx, yy, count(xx) as test from (select s.yy,name from tab s where id = 2 and name = 'wenshao') s where s.name='333'";
//        String sql = "select u.*,g.name from user u join user_group g on u.groupId=g.groupId where u.name=?";
//        String sql = "select tenant_id from people where id in (select id from user s)";
//        String sql = "update user u set ds=?, u.name=?,id='fdf' ,ddd=? where id =?";
//        String sql = "delete from user where id = ( select id from user s )";
//        String sql = "insert into user (id,name) values('0','heykb')";
//        String sql = "insert into user (id,name) select g.id,g.name from user_group g where id=1";
//        String sql = "SELECT * FROM \"a\" left JOIN (select inj.yy from tab t where id = 2 and name = 'wenshao') b on a.name = b.name";
//        String sql = "SELECT * FROM (select * from user where id = 2) s";
//        String sql = "SELECT * from (SELECT * FROM a union all SELECT * FROM b)";
//        String sql = "SELECT auth_user.* from auth_user  where auth_user.\"name\" like '%3'";
//        String sql = "SELECT * from (SELECT * FROM a union all SELECT * FROM b)";
//        String sql = "SELECT a,name FROM tb1 union SELECT b,name FROM tb2";
//        String sql = "SELECT a.name,b,c FROM table a";
//        String sql = "SELECT created_by, dept_id, important_data, data, tenant_id, id\n" +
//                "FROM test\n" +
//                "LIMIT ? OFFSET ?";
        String sql = "update test set important_data = ?,created_by=?";
        InjectColumnInfoHandler right = new InjectColumnInfoHandler() {
            @Override
            public String getColumnName() {
                return "tenant_id";
            }
            @Override
            public String getValue() {
                return "sqlhelper";
            }
            @Override
            public int getInjectTypes() {
                return CONDITION;
            }
            @Override
            public boolean checkCommandType(SqlCommandType commandType) {
                return true;
            }
            @Override
            public boolean checkTableName(String tableName) {
                return true;
            }
        };
        ColumnFilterInfoHandler columnFilterInfoHandler = new ColumnFilterInfoHandler() {

            @Override
            public Set<String> getFilterColumns() {
                return Sets.newHashSet("importantData");
            }

            @Override
            public boolean checkTableName(String tableName) {
                return "test".equals(tableName);
            }
        };
        SqlStatementEditor sqlStatementEditorFactory =
                new SqlStatementEditor.Builder(sql, DbType.postgresql)
//                        .injectColumnInfoHandlers(Arrays.asList(right))
                        .columnFilterInfoHandlers(Arrays.asList(columnFilterInfoHandler))
                        .build();
        SqlStatementEditor.Result result = sqlStatementEditorFactory.processing();

        System.out.println(result.getSql());
    }
}
