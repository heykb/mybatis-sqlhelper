package io.github.heykb.sqlhelper.autoconfigure;

import lombok.Data;

@Data
public class SqlHelperMultiTenantProperties {
    /**
     * Multi-tenant feature switch
     */
    private boolean enable = true;
}
