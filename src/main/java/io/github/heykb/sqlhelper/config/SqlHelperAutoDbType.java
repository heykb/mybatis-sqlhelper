package io.github.heykb.sqlhelper.config;

import com.alibaba.druid.DbType;
import com.alibaba.druid.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author heykb
 */
public class SqlHelperAutoDbType {
    private static final Map<DataSource, DbType> datasourceDbTypeMap = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();
    public static DbType getDbType(DataSource dataSource){
        DbType re = datasourceDbTypeMap.get(dataSource);
        if(re==null){
            lock.lock();
            try {
                if (datasourceDbTypeMap.containsKey(dataSource)) {
                    return datasourceDbTypeMap.get(dataSource);
                }
                String url = getUrl(dataSource);
                if (StringUtils.isEmpty(url)) {
                    throw new SqlHelperException("无法自动获取jdbcUrl，请通过配置指定数据库类型!");
                }
                DbType dbType = fromJdbcUrl(url);
                if (dbType == null) {
                    throw new SqlHelperException("无法从"+url+"自动获取数据库类型com.alibaba.druid.DbType，请通过配置指定数据库类型!");
                }
                datasourceDbTypeMap.put(dataSource,dbType);
                return dbType;
            } finally {
                lock.unlock();
            }
        }
        return re;
    }

    static private DbType fromJdbcUrl(String jdbcUrl) {
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
}
