package io.github.heykb.sqlhelper.dynamicdatasource;

import io.github.heykb.sqlhelper.utils.Asserts;
import lombok.Data;

import javax.sql.DataSource;
import java.util.function.Function;

/**
 * The type Logic ds meta.
 */
@Data
public class LogicDsMeta {

    /**
     * 数据源id,当已注册数据源中存在相同id时会复用数据源，并且会触发数据源升级回调方法如果存在的话。
     * 使用同一个数据源id的不同逻辑数据源可以设置不同的subspace子空间。如mysql支持同一个连接切换不同数据库
     */
    private String datasourceId;
    /**
     * 设置期望的子空间类型（不是所有的数据库都支持同一个连接进行切换)
     * 主要作用在于当用户期望和软件支持不匹配能快速失败及时报错
     */
    private ConnectionSubspaceTypeEnum expectedSubspaceType;
    /**
     * 当逻辑数据源使用时将连接切换到指定的子空间。子空间的名称。仅当数据库类型支持子空间时有效
     * nullable
     */
    private String subspace;
    private String table_prefix;
    /**
     * 当数据源id不存在时，使用此回调创建新的数据源
     */
    private Function<Void, DataSource> createFunc;

    private LogicDsMeta() {
    }

    /**
     * Builder logic ds meta builder.
     *
     * @return the logic ds meta builder
     */
    public static final LogicDsMetaBuilder builder() {
        return new LogicDsMetaBuilder();
    }

    /**
     * The type Logic ds meta builder.
     */
    public static final class LogicDsMetaBuilder {
        private String datasourceId;
        private ConnectionSubspaceTypeEnum expectedSubspaceType;
        private String subspace;
        private String table_prefix;
        private Function<Void, DataSource> createFunc;

        private LogicDsMetaBuilder() {
        }

        /**
         * A logic ds meta logic ds meta builder.
         *
         * @return the logic ds meta builder
         */
        public static LogicDsMetaBuilder aLogicDsMeta() {
            return new LogicDsMetaBuilder();
        }

        /**
         * 数据源id,当已注册数据源中存在相同id时会复用数据源，并且会触发数据源升级回调方法如果存在的话。
         * 使用同一个数据源id的不同逻辑数据源可以设置不同的subspace子空间。如mysql支持同一个连接使用过程中切换数据库
         *
         * @param datasourceId the datasource id
         * @return the logic ds meta builder
         */
        public LogicDsMetaBuilder datasourceId(String datasourceId) {
            this.datasourceId = datasourceId;
            return this;
        }

        /**
         * 设置期望的子空间类型（不是所有的数据库都支持同一个连接进行切换)
         * 主要作用在于当用户期望和软件支持不匹配能快速失败及时报错
         *
         * @param expectedSubspaceType the expected subspace type
         * @return the logic ds meta builder
         */
        public LogicDsMetaBuilder expectedSubspaceType(ConnectionSubspaceTypeEnum expectedSubspaceType) {
            this.expectedSubspaceType = expectedSubspaceType;
            return this;
        }

        /**
         * 当逻辑数据源使用时将连接切换到指定的子空间。子空间的名称。仅当数据库类型支持子空间时有效
         * nullable
         * @param subspace the subspace
         * @return the logic ds meta builder
         */
        public LogicDsMetaBuilder subspace(String subspace) {
            this.subspace = subspace;
            return this;
        }

        /**
         * Table prefix logic ds meta builder.
         *
         * @param table_prefix the table prefix
         * @return the logic ds meta builder
         */
        public LogicDsMetaBuilder table_prefix(String table_prefix) {
            this.table_prefix = table_prefix;
            return this;
        }

        /**
         * 当数据源id不存在时，使用此回调创建新的数据源
         *
         * @param createFunc the create func
         * @return the logic ds meta builder
         */
        public LogicDsMetaBuilder createFunc(Function<Void, DataSource> createFunc) {
            this.createFunc = createFunc;
            return this;
        }

        /**
         * Build logic ds meta.
         *
         * @return the logic ds meta
         */
        public LogicDsMeta build() {
            Asserts.notNull(datasourceId, "datasourceId");
            Asserts.notNull(expectedSubspaceType, "expectedSubspaceType");
            LogicDsMeta logicDsMeta = new LogicDsMeta();
            logicDsMeta.setDatasourceId(datasourceId);
            logicDsMeta.setExpectedSubspaceType(expectedSubspaceType);
            logicDsMeta.setSubspace(subspace);
            logicDsMeta.setTable_prefix(table_prefix);
            logicDsMeta.setCreateFunc(createFunc);
            return logicDsMeta;
        }
    }
}
