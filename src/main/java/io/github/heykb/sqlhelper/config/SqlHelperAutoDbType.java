package io.github.heykb.sqlhelper.config;

import com.alibaba.druid.DbType;
import com.alibaba.druid.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author heykb
 */
public class SqlHelperAutoDbType {
    public static DbType getDbType(DataSource dataSource){
        String url = getUrl(dataSource);
        if (StringUtils.isEmpty(url)) {
            throw new SqlHelperException("无法自动获取jdbcUrl，请通过配置指定数据库类型!");
        }
        DbType dbType = fromJdbcUrl(url);
        if (dbType == null) {
            throw new SqlHelperException("无法从" + url + "自动获取数据库类型com.alibaba.druid.DbType，请通过配置指定数据库类型!");
        }
        return dbType;
    }

    public static DbType getDbType(Connection connection) {
        try {
            DbType dbType = fromJdbcUrl(connection.getMetaData().getURL());
            return dbType;
        } catch (SQLException e) {
            throw new SqlHelperException("自动获取DbType失败");
        }
    }

    public static DbType fromJdbcUrl(String jdbcUrl) {
        final String url = jdbcUrl.toLowerCase();
        for (DbType dbType : DbType.values()) {
            if (url.contains(dbType.name().toLowerCase())) {
                return dbType;
            }
        }
        return null;
    }

    /**
     * 获取url
     *
     * @param dataSource
     * @return
     */
    static private String getUrl(DataSource dataSource) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            return conn.getMetaData().getURL();
        } catch (SQLException e) {
            throw new SqlHelperException(e);
        } finally {
            if (conn != null) {
                try {
                        conn.close();
                } catch (SQLException e) {
                    //ignore
                }
            }
        }
    }

    static void test(String url) {
        String cleanURI = url.substring(5);
        URI uri = URI.create(cleanURI);
        System.out.println(url);
        System.out.println(uri.getScheme());
        System.out.println(uri.getHost());
        System.out.println(uri.getPort());
        System.out.println(uri.getPath());
        System.out.println("*****************");
    }

    public static void main(String[] args) {
        test("jdbc:oracle:thin:@localhost:1521:orclpdb1");
        test("jdbc:mysql://localhost/database2");
        test("jdbc:postgresql://localhost/database2");
        test("jdbc:sqlserver://localhost;instance=SQLEXPRESS;databaseName=database2");
        test("jdbc:mariadb://localhost/database2");
        test("jdbc:db2://localhost/database2");
        test("jdbc:sap://localhost/database2");
        test("jdbc:informix-sqli://localhost:9088/sysuser:INFORMIXSERVER=hpjp");
        test("jdbc:hsqldb:mem:database2");
        test("jdbc:h2:mem:database2");
        test("jdbc:derby:target/tmp/derby/hpjp;databaseName=database2;create=true");
    }
}
