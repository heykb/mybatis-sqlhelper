package io.github.heykb.sqlhelper.spring.dynamicds;

import org.springframework.core.InfrastructureProxy;

import javax.sql.DataSource;
import java.util.function.Function;

class SqlHelperDynamicDataSourceProxy extends io.github.heykb.sqlhelper.dynamicdatasource.SqlHelperDynamicDataSourceProxy implements InfrastructureProxy {
    public SqlHelperDynamicDataSourceProxy(DataSource primaryDs) {
        super(primaryDs);
    }

    public SqlHelperDynamicDataSourceProxy(DataSource primaryDs, Function<DataSource, DataSource> dsUpgradeCallback) {
        super(primaryDs, dsUpgradeCallback);
    }

    /**
     * 适应spring事务，做到数据源切换后再开启的事务为一个新的事务，保证正常切换。
     * @See    org.springframework.transaction.support.TransactionSynchronizationUtils#unwrapResourceIfNecessary()
     * @return
     */
    @Override
    public Object getWrappedObject() {
        return getDatasource();
    }
}
