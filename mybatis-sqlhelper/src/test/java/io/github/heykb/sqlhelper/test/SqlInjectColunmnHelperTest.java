package io.github.heykb.sqlhelper.test;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

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
//        String sql = "SELECT important_data from (SELECT important_data FROM a union all SELECT important_data FROM b)";
//        String sql = "SELECT important_data,a,name FROM tb1 union SELECT important_data,b,name FROM tb2";
//        String sql = "SELECT name,b,c FROM \"select\"";
//        String sql = "SELECT get_auths('xx') as xxx,created_by, dept_id, important_data, data, tenant_id, id\n" +
//                "FROM test\n" +
//                "LIMIT ? OFFSET ?";
//        String sql = "select unix_timestamp(current_timestamp()) * 1000 as c_timestamp";
//        String sql = "select panel_group.*,panel_group.name as label , get_auths(panel_group.id,'panel',?) as `privileges` from panel_group where id =?";
//        String sql = "SELECT\n" +
//                "        v_auth_model.id,\n" +
//                "        v_auth_model.name,\n" +
//                "        v_auth_model.label,\n" +
//                "        v_auth_model.pid,\n" +
//                "        v_auth_model.node_type,\n" +
//                "        v_auth_model.model_type,\n" +
//                "        v_auth_model.model_inner_type,\n" +
//                "        v_auth_model.auth_type,\n" +
//                "        v_auth_model.create_by,\n" +
//                "        v_auth_model.level,\n" +
//                "        v_auth_model.mode,\n" +
//                "        v_auth_model.data_source_id,\n" +
//                "        authInfo.PRIVILEGES AS `privileges`\n" +
//                "        FROM\n" +
//                "        ( SELECT GET_V_AUTH_MODEL_ID_P_USE ( ? ) cids ) t,\n" +
//                "        v_auth_model\n" +
//                "        LEFT JOIN (\n" +
//                "        SELECT\n" +
//                "        auth_source,\n" +
//                "        group_concat( DISTINCT sys_auth_detail.privilege_extend ) AS `privileges`\n" +
//                "        FROM\n" +
//                "        (\n" +
//                "        `sys_auth`\n" +
//                "        LEFT JOIN `sys_auth_detail` ON ((\n" +
//                "        `sys_auth`.`id` = `sys_auth_detail`.`auth_id`\n" +
//                "        )))\n" +
//                "        WHERE\n" +
//                "        sys_auth_detail.privilege_value = 1\n" +
//                "        AND sys_auth.auth_source_type = ?\n" +
//                "        AND (\n" +
//                "        (\n" +
//                "        sys_auth.auth_target_type = 'dept'\n" +
//                "        AND sys_auth.auth_target IN ( SELECT dept_id FROM sys_user WHERE user_id = ? )\n" +
//                "        )\n" +
//                "        OR (\n" +
//                "        sys_auth.auth_target_type = 'user'\n" +
//                "        AND sys_auth.auth_target = ?\n" +
//                "        )\n" +
//                "        OR (\n" +
//                "        sys_auth.auth_target_type = 'role'\n" +
//                "        AND sys_auth.auth_target IN ( SELECT role_id FROM sys_users_roles WHERE user_id = ? )\n" +
//                "        )\n" +
//                "        )\n" +
//                "        GROUP BY\n" +
//                "        `sys_auth`.`auth_source`\n" +
//                "        ) authInfo ON v_auth_model.id = authInfo.auth_source\n" +
//                "        WHERE\n" +
//                "        FIND_IN_SET( v_auth_model.id, cids )\n" +
//                "        ORDER BY v_auth_model.node_type desc, CONVERT(v_auth_model.label using gbk) asc\n" +
//                "    ";
//        String sql = "UPDATE chart_view cv,\n" +
//                "chart_view_cache cve \n" +
//                "SET cv.` NAME ` = cve.` NAME `,\n" +
//                "cv.title = cve.title,\n" +
//                "cv.scene_id = cve.scene_id\n" +
//                "WHERE\n" +
//                "\tcve.ID = cv.ID \n" +
//                "\tAND cv.ID IN ( ?, ? ) ";
        // 多表
//        String sql = "DELETE MV\n" +
//                "FROM MV,Track\n" +
//                "WHERE Track.trkid=MV.mvid";
//        String sql = "DELETE FROM MV\n" +
//                "USING MV,Track\n" +
//                "WHERE Track.trkid=MV.mvid";
//        String sql = "DELETE MV\n" +
//                "FROM MV\n" +
//                "LEFT JOIN Track\n" +
//                "ON MV.mvid=Track.trkid\n" +
//                "WHERE Track.trkid IS NULL";
//        String sql = "DELETE FROM MV\n" +
//                "USING MV\n" +
//                "LEFT JOIN Track\n" +
//                "ON MV.mvid=Track.trkid\n" +
//                "WHERE Track.trkid IS NULL";
//        String sql = "UPDATE chart_view cv,\n" +
//                "chart_view_cache cve \n" +
//                "SET cv.` NAME ` = cve.` NAME `,\n" +
//                "cv.title = cve.title,\n" +
//                "cv.scene_id = cve.scene_id\n" +
//                "WHERE\n" +
//                "\tcve.ID = cv.ID \n" +
//                "\tAND cv.ID IN ( ?, ? ) ";
//        String sql = "UPDATE s,c SET s.class_name='test00',c.stu_name='test00' from student s,class c  WHERE s.class_id = c.id ";
//        String sql = "UPDATE student s,class c SET s.class_name='test00',c.stu_name='test00' WHERE s.class_id = c.id";
//        String sql = "UPDATE student s JOIN class c ON s.class_id = c.id SET s.class_name='test11'";
//        String sql = "UPDATE student s LEFT JOIN class c ON s.class_id = c.id SET s.class_name='test22',c.stu_name='test22'";
//        String sql = "UPDATE student LEFT JOIN class ON student.class_id = class.id SET student.class_name='test22',class.stu_name='test22'";

        //        String sql ="UPDATE student s RIGHT JOIN class c ON s.class_id = c.id SET s.class_name='test33',c.stu_name='test33'";
//        String sql = "INSERT ALL\n" +
//                "  INTO suppliers (supplier_id, supplier_name) VALUES (?, ?)\n" +
//                "  INTO tb2 (supplier_id, supplier_name) VALUES (?, ?)\n" +
//                "  INTO tb3 (supplier_id, supplier_name) VALUES (?, ?)\n" +
//                "SELECT * FROM dual;";
//        String sql = "INSERT ALL\n" +
//                "  INTO suppliers (supplier_id, supplier_name) VALUES (1000, 'IBM')\n" +
//                "  INTO suppliers (supplier_id, supplier_name) VALUES (2000, 'Microsoft')\n" +
//                "  INTO customers (customer_id, customer_name, city) VALUES (999999, 'Anderson Construction', 'New York')\n" +
//                "SELECT * FROM dual;";
//        String sql = "INSERT ALL\n" +
//        "when object_id < 5 then  INTO suppliers (supplier_id, supplier_name) VALUES (?, 'IBM')\n" +
//        "when object_id < 10 then  INTO suppliers (supplier_id, supplier_name) VALUES (?, ?)\n" +
//        " else INTO customers (customer_id, customer_name, city) VALUES (999999, 'Anderson Construction', 'New York')\n" +
//        "SELECT object_id FROM t;";
        String sql = "select * from p where a in (1,2,3) and x='x'";
        SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(sql);
        System.out.println(sql);
//        InjectColumnInfoHandler right = new InjectColumnInfoHandler() {
//            @Override
//            public String getColumnName() {
//                return "supplier_id";
//            }
//            @Override
//            public String getValue() {
//                return "'sqlhelper'";
//            }
//            @Override
//            public int getInjectTypes() {
//                return CONDITION|INSERT|UPDATE;
//            }
//            @Override
//            public boolean checkCommandType(SqlCommandType commandType) {
//                return true;
//            }
//            @Override
//            public boolean checkTableName(String tableName) {
//                return !"dual".equals(tableName);
//            }
//        };
//        ColumnFilterInfoHandler columnFilterInfoHandler = new ColumnFilterInfoHandler() {
//
//            @Override
//            public Set<String> getFilterColumns() {
//                return Sets.newHashSet("id");
//            }
//
//            @Override
//            public boolean checkTableName(String tableName) {
//                return "v_auth_model".equals(tableName);
//            }
//        };
//        LogicDeleteInfoHandler logicDeleteInfoHandler = new LogicDeleteInfoHandler (){
//            @Override
//            public String getDeleteSqlDemo() {
//                return "UPDATE xx SET is_deleted = 'Y'";
//            }
//
//            @Override
//            public String getNotDeletedValue() {
//                return "'N'";
//            }
//
//            @Override
//            public String getColumnName() {
//                return "is_deleted";
//            }
//
//            @Override
//            public boolean checkTableName(String tableName) {
//                return !"MV".equals(tableName);
//            }
//        };
//        LogicDeleteInfoHandler logicDeleteInfoHandler2 = new LogicDeleteInfoHandler (){
//            @Override
//            public String getDeleteSqlDemo() {
//                return "UPDATE xx SET is_deleted = true";
//            }
//
//            @Override
//            public String getNotDeletedValue() {
//                return "false";
//            }
//
//            @Override
//            public String getColumnName() {
//                return "is_deleted";
//            }
//
//            @Override
//            public boolean checkTableName(String tableName) {
//                return "MV".equals(tableName);
//            }
//        };
//        SqlStatementEditor sqlStatementEditor =
//                new SqlStatementEditor.Builder(sql, DbType.mysql)
//                        .injectColumnInfoHandlers(Arrays.asList(right,logicDeleteInfoHandler,logicDeleteInfoHandler2))
////                        .columnFilterInfoHandlers(Arrays.asList(columnFilterInfoHandler))
//                        .build();
//        SqlStatementEditor.Result result = sqlStatementEditor.processing();
//
//        System.out.println(result.getSql());
    }
}
