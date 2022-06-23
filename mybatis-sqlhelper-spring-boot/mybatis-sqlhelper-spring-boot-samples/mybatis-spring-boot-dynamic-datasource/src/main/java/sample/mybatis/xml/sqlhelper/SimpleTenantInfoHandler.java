package sample.mybatis.xml.sqlhelper;


import io.github.heykb.sqlhelper.handler.abstractor.TenantInfoHandler;

/**
 * @author heykb
 */
//@Component
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
        return "city".equals(tableName);
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return !mapperId.contains("noPluginSelect");
    }
}
