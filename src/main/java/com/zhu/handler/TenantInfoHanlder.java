package com.zhu.handler;


/**
 * @author heykb
 */
public abstract class TenantInfoHanlder implements InjectColumnInfoHandler{
    public abstract String getTenantIdColumn();
    public abstract Object getTenantId();

    @Override
    public String getColumnName() {
        return getTenantIdColumn();
    }

    @Override
    public Object getValue() {
        return getTenantId();
    }

    @Override
    public boolean isExistSkip() {
        return false;
    }

    @Override
    public int getInjectTypes() {
        return CONDITION|INSERT;
    }
}
