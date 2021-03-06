package io.github.heykb.sqlhelper.test;

import com.alibaba.druid.DbType;
import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;

public interface SqlTest {
    String name();
    DbType db();
    String origin();
    String target();
    InjectColumnInfoHandler injectColumnInfoHandler();
}
