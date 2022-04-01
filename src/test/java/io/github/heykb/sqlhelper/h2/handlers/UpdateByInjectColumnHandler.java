package io.github.heykb.sqlhelper.h2.handlers;

import io.github.heykb.sqlhelper.handler.InjectColumnInfoHandler;

public class UpdateByInjectColumnHandler implements InjectColumnInfoHandler {
    @Override
    public String getColumnName() {
        return "updated_by";
    }

    @Override
    public String getValue() {
        return "'zrc'";
    }

    @Override
    public int getInjectTypes() {
        return UPDATE|INSERT;
    }

    @Override
    public boolean isExistOverride() {
        return true;
    }

    // 排除user_info表
    @Override
    public boolean checkTableName(String tableName) {
        return !"user_info".equals(tableName);
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        return true;
    }
}