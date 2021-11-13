package io.github.heykb.sqlhelper.config;


import com.alibaba.druid.DbType;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author heykb
 */
public class SqlHelperConfig {
    @Value("${sqlhelper.dbtype:}")
    private DbType dbtype;
    @Value("${sqlhelper.enable:true}")
    private boolean enable;
    @Value("${sqlhelper.logicDelete.enable:false}")
    private boolean logicDeleteEnable;
    @Value("${sqlhelper.multiTenant.enable:true}")
    private boolean multiTenantEnable;
    @Value("${sqlhelper.selectItem.tbAliasPrefix:inj}")
    private String tbAliasPrefix;

    public String getTbAliasPrefix() {
        return tbAliasPrefix;
    }

    public void setTbAliasPrefix(String tbAliasPrefix) {
        this.tbAliasPrefix = tbAliasPrefix;
    }

    public DbType getDbtype() {
        return dbtype;
    }

    public void setDbtype(DbType dbtype) {
        this.dbtype = dbtype;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isLogicDeleteEnable() {
        return logicDeleteEnable;
    }

    public void setLogicDeleteEnable(boolean logicDeleteEnable) {
        this.logicDeleteEnable = logicDeleteEnable;
    }

    public boolean isMultiTenantEnable() {
        return multiTenantEnable;
    }

    public void setMultiTenantEnable(boolean multiTenantEnable) {
        this.multiTenantEnable = multiTenantEnable;
    }
}
