package io.github.heykb.sqlhelper.h2.handlers;

import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;

public class SimpleLogicDeleteInfoHandler extends LogicDeleteInfoHandler {
    @Override
    public String getDeleteSqlDemo() {
        return "UPDATE xx SET is_deleted = 'Y'";
    }

    @Override
    public String getNotDeletedValue() {
        return "'N'";
    }

    @Override
    public String getColumnName() {
        return "is_deleted";
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return !mapperId.contains("noPlugin");
    }
}