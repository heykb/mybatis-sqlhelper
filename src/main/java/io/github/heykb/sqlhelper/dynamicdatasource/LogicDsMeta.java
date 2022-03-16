package io.github.heykb.sqlhelper.dynamicdatasource;

import io.github.heykb.sqlhelper.utils.Asserts;
import lombok.Data;

import javax.sql.DataSource;
import java.util.function.Function;

@Data
public class LogicDsMeta {
    private String datasourceId;
    private ConnectionSubspaceTypeEnum expectedSubspaceType;
    private String subspace;
    private String table_prefix;
    private Function<Void, DataSource> createFunc;

    private LogicDsMeta() {
    }

    public static final LogicDsMetaBuilder builder() {
        return new LogicDsMetaBuilder();
    }

    public static final class LogicDsMetaBuilder {
        private String datasourceId;
        private ConnectionSubspaceTypeEnum expectedSubspaceType;
        private String subspace;
        private String table_prefix;
        private Function<Void, DataSource> createFunc;

        private LogicDsMetaBuilder() {
        }

        public static LogicDsMetaBuilder aLogicDsMeta() {
            return new LogicDsMetaBuilder();
        }

        public LogicDsMetaBuilder datasourceId(String datasourceId) {
            this.datasourceId = datasourceId;
            return this;
        }

        public LogicDsMetaBuilder expectedSubspaceType(ConnectionSubspaceTypeEnum expectedSubspaceType) {
            this.expectedSubspaceType = expectedSubspaceType;
            return this;
        }

        public LogicDsMetaBuilder subspace(String subspace) {
            this.subspace = subspace;
            return this;
        }

        public LogicDsMetaBuilder table_prefix(String table_prefix) {
            this.table_prefix = table_prefix;
            return this;
        }

        public LogicDsMetaBuilder createFunc(Function<Void, DataSource> createFunc) {
            this.createFunc = createFunc;
            return this;
        }

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
