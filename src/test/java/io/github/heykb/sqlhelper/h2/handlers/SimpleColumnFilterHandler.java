package io.github.heykb.sqlhelper.h2.handlers;

import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;
import org.apache.commons.compress.utils.Sets;

import java.util.Set;

/**
 * @author heykb
 */
public class SimpleColumnFilterHandler implements ColumnFilterInfoHandler {

    @Override
    public Set<String> getFilterColumns() {
        return Sets.newHashSet("name", "enable");
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return true;
    }

    @Override
    public boolean checkTableName(String tableName) {
        return true;
    }
}
