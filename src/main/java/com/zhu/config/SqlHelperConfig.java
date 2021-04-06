package com.zhu.config;


import com.alibaba.druid.DbType;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author heykb
 */
public class SqlHelperConfig {
    @Value("${sqlhelper.dbtype:mysql}")
    private DbType dbtype;
    @Value("${sqlhelper.enable:true}")
    private boolean enable;
    @Value("${sqlhelper.logic-delete.enable:true}")
    private boolean logicDeleteEnable;
    @Value("${sqlhelper.multi-tenant.enable:true}")
    private boolean multiTenantEnable;

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
