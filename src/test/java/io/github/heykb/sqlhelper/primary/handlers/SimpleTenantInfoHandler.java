package io.github.heykb.sqlhelper.primary.handlers;

import io.github.heykb.sqlhelper.handler.abstractor.TenantInfoHandler;

/**
 * @author heykb
 */
public class SimpleTenantInfoHandler extends TenantInfoHandler {
    public static final String TENANT_ID = "1";
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
    @Override
    public String getTenantId() {
        // 可以从
        // SecurityContextHolder.getContext().getAuthentication()
        return "'"+TENANT_ID+"'";
    }
    @Override
    public boolean checkTableName(String tableName) {
        return true;
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return !mapperId.contains("noPluginSelect");
    }
}
