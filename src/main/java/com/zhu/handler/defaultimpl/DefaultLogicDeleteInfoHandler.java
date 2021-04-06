package com.zhu.handler.defaultimpl;

import com.zhu.handler.LogicDeleteInfoHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * @author heykb
 */
@ConfigurationProperties(prefix = "sqlhelper.logic-delete")
public class DefaultLogicDeleteInfoHandler extends LogicDeleteInfoHandler {
    private static final AntPathMatcher antPathMatcher;
    static {
        antPathMatcher = new AntPathMatcher();
        antPathMatcher.setPathSeparator(".");
    }
    private String sqlDemo;
    private String columnName;
    private Object notDeletedValue;

    private Set<String> ignoreTables;

    private List<String> ignoreMapperIds;

    public Set<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    public List<String> getIgnoreMapperIds() {
        return ignoreMapperIds;
    }

    public void setIgnoreMapperIds(List<String> ignoreMapperIds) {
        this.ignoreMapperIds = ignoreMapperIds;
    }

    @Override
    public String getSqlDemo() {
        return this.sqlDemo;
    }

    @Override
    public Object getNotDeletedValue() {
        return notDeletedValue;
    }

    public void setNotDeletedValue(Object notDeletedValue) {
        this.notDeletedValue = notDeletedValue;
    }

    @Override
    public String getColumnName() {
        return this.columnName;
    }

    public void setSqlDemo(String sqlDemo) {
        this.sqlDemo = sqlDemo;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if(ignoreTables==null || ignoreTables.isEmpty()){
            return false;
        }
        return ignoreTables.contains(tableName);
    }

    @Override
    public boolean ignoreMapperId(String mapperId) {
        if(ignoreMapperIds==null || ignoreMapperIds.isEmpty()){
            return false;
        }
        for(String pattern:ignoreMapperIds){
            if(antPathMatcher.match(pattern,mapperId)) return true;
        }
        return false;
    }
}
