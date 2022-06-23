package io.github.heykb.sqlhelper.spring.primary.handlers;


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
        return "people".equals(tableName)||"department".equals(tableName);
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return !mapperId.contains("noPluginSelect");
    }
}
