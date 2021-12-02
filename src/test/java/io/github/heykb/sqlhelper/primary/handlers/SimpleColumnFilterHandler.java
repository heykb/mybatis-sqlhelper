package io.github.heykb.sqlhelper.primary.handlers;

import io.github.heykb.sqlhelper.handler.ColumnFilterInfoHandler;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author heykb
 */
public class SimpleColumnFilterHandler implements ColumnFilterInfoHandler {

    @Override
    public Collection<String> getFilterColumns() {
        return Arrays.asList("name");
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return true;
    }
}
