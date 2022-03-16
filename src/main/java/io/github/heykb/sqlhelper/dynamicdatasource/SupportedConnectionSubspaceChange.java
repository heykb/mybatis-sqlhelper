package io.github.heykb.sqlhelper.dynamicdatasource;

import com.alibaba.druid.DbType;
import io.github.heykb.sqlhelper.config.SqlHelperAutoDbType;
import io.github.heykb.sqlhelper.config.SqlHelperException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Supported connection subspace change.
 */
public class SupportedConnectionSubspaceChange {

    private static final Log log = LogFactory.getLog(SupportedConnectionSubspaceChange.class);

    private static final Map<DbType, ConnectionSubspaceTypeEnum> DB_TO_NAMESPACE_TYPE = new HashMap<>(3);

    static {
        DB_TO_NAMESPACE_TYPE.put(DbType.postgresql, ConnectionSubspaceTypeEnum.SCHEMA);
        DB_TO_NAMESPACE_TYPE.put(DbType.mysql, ConnectionSubspaceTypeEnum.DATABASE);
        DB_TO_NAMESPACE_TYPE.put(DbType.sqlserver, ConnectionSubspaceTypeEnum.DATABASE);
    }

    /**
     * Change subspace if supprt connection subspace type.
     *
     * @param connection the connection
     * @param subspace   the subspace
     * @return the connection subspace type
     * @throws SQLException the sql exception
     */
    public static ConnectionSubspaceTypeEnum changeNamespaceIfSupport(Connection connection, String subspace, ConnectionSubspaceTypeEnum expectedType) throws SQLException {
        if (subspace == null) {
            return null;
        }
        log.warn(Thread.currentThread().getName() + "线程连接subspace切换到" + subspace);
        ConnectionSubspaceTypeEnum re = getSupportedSubspaceType(connection, expectedType);
        switch (re) {
            case SCHEMA:
                connection.setSchema(subspace);
                break;
            case DATABASE:
                connection.setCatalog(subspace);
                break;
        }
        log.warn("连接subspace类型为" + re.name());
        return re;
    }

    /**
     * Gets current subspace if support change.
     *
     * @param connection the connection
     * @return the current subspace if support change
     * @throws SQLException the sql exception
     */
    public static String getCurrentSubspaceIfSupport(Connection connection, ConnectionSubspaceTypeEnum expectedType) throws SQLException {
        String subspace = null;
        ConnectionSubspaceTypeEnum subspaceType = getSupportedSubspaceType(connection, expectedType);
        switch (subspaceType) {
            case SCHEMA:
                return connection.getSchema();
            case DATABASE:
                return connection.getCatalog();
        }
        return subspace;
    }

    public static ConnectionSubspaceTypeEnum getSupportedSubspaceType(Connection connection, ConnectionSubspaceTypeEnum expectedType) throws SQLException {
        DbType dbType = SqlHelperAutoDbType.fromJdbcUrl(connection.getMetaData().getURL());
        ConnectionSubspaceTypeEnum subspaceType = DB_TO_NAMESPACE_TYPE.get(dbType);
        if (subspaceType == null) {
            subspaceType = ConnectionSubspaceTypeEnum.NOT_SUPPORT;
        }
        if (expectedType != null && expectedType != subspaceType) {
            throw new SqlHelperException(dbType.name() + "数据库连接支持的subspace类型为" + subspaceType + "。但是LogicDsMeta配置中期望的类型是" + expectedType.name());
        }
        return subspaceType;
    }

    public static ConnectionSubspaceTypeEnum getSupportedSubspaceType(DbType dbType) {
        return DB_TO_NAMESPACE_TYPE.get(dbType);
    }


}
