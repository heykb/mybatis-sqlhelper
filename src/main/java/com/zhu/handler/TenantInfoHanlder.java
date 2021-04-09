package com.zhu.handler;


/**
 * 实现InjectColumnInfoHandler自动注入配置接口，代表多租户自动注入配置
 * 配置多租户的字段名称、租户value获取方式
 * 使用时继承该类，并注入为一个bean
 * @author heykb
 */
public abstract class TenantInfoHanlder implements InjectColumnInfoHandler{
    /**
     * 设置代表租户字段名称
     * @return
     */
    public abstract String getTenantIdColumn();
    /**
     * 当前租户value获取方式
     * @return
     */
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

    /**
     * 为所有子查询添加租户条件  为所有插入语句注入租户id
     * @return
     */
    @Override
    public int getInjectTypes() {
        return CONDITION|INSERT;
    }
}
