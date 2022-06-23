package io.github.heykb.sqlhelper.dynamicdatasource;

import io.github.heykb.sqlhelper.config.SqlHelperException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The type Primary datasource.
 */
public class PrimaryDatasource extends SimpleProxyDatasource {
    private String primaryDsInitialSubspace;

    /**
     * Instantiates a new Primary datasource.
     *
     * @param dataSource the data source
     */
    public PrimaryDatasource(DataSource dataSource) {
        super(dataSource);
        init();
    }

    private void init() {
        try (Connection connection = super.getConnection()) {
            this.primaryDsInitialSubspace = SupportedConnectionSubspaceChange.getCurrentSubspaceIfSupport(connection, null);
        } catch (SQLException e) {
            throw new SqlHelperException(e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        SupportedConnectionSubspaceChange.changeSubspaceIfSupport(connection, primaryDsInitialSubspace,null);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        SupportedConnectionSubspaceChange.changeSubspaceIfSupport(connection, primaryDsInitialSubspace,null);
        return connection;
    }
}
