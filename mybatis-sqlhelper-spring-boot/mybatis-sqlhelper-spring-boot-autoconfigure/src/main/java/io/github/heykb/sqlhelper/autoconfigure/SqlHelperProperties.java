package io.github.heykb.sqlhelper.autoconfigure;

import com.alibaba.druid.DbType;
import io.github.heykb.sqlhelper.interceptor.SqlHelperPlugin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Properties;

@ConfigurationProperties(prefix = SqlHelperProperties.SQLHELPER_PREFIX)
public class SqlHelperProperties {
    public static final String SQLHELPER_PREFIX = "sqlhelper";
    /**
     * Master switch
     */
    private boolean enable = true;
    /**
     * database type.Support to automatically obtain the type according to the Datasource
     */
    @Deprecated
    private DbType dbType;
    @NestedConfigurationProperty
    private SqlHelperLogicDeleteProperties logicDelete = new SqlHelperLogicDeleteProperties();
    @NestedConfigurationProperty
    private SqlHelperMultiTenantProperties multiTenant = new SqlHelperMultiTenantProperties();


    private Properties properties = new Properties();


    public SqlHelperProperties() {
        properties.setProperty(SqlHelperPlugin.enableProp,String.valueOf(enable));
        properties.setProperty(SqlHelperPlugin.logicDeleteEnableProp,String.valueOf(logicDelete.isEnable()));
        properties.setProperty(SqlHelperPlugin.multiTenantEnableProp,String.valueOf(multiTenant.isEnable()));

    }

    public Properties getProperties() {
        return properties;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
        properties.setProperty(SqlHelperPlugin.enableProp,String.valueOf(enable));
    }

    @Deprecated
    public DbType getDbType() {
        return dbType;
    }

    @Deprecated
    public void setDbType(DbType dbType) {
        this.dbType = dbType;
        properties.setProperty(SqlHelperPlugin.dbTypeProp,dbType.name());
    }


    public SqlHelperLogicDeleteProperties getLogicDelete() {
        return logicDelete;
    }

    public void setLogicDelete(SqlHelperLogicDeleteProperties logicDelete) {
        this.logicDelete = logicDelete;
        properties.setProperty(SqlHelperPlugin.logicDeleteEnableProp,String.valueOf(logicDelete.isEnable()));
    }

    public SqlHelperMultiTenantProperties getMultiTenant() {
        return multiTenant;
    }

    public void setMultiTenant(SqlHelperMultiTenantProperties multiTenant) {
        this.multiTenant = multiTenant;
        properties.setProperty(SqlHelperPlugin.multiTenantEnableProp,String.valueOf(multiTenant.isEnable()));
    }
}
