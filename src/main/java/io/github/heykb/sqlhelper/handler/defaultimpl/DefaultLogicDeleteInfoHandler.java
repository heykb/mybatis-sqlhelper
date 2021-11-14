package io.github.heykb.sqlhelper.handler.defaultimpl;

import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * 通过环境变量配置逻辑删除的实现类
 *
 * @author heykb
 */
@ConfigurationProperties(prefix = "sqlhelper.logic-delete")
public class DefaultLogicDeleteInfoHandler extends LogicDeleteInfoHandler {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher(".");
    private String sqlDemo;
    private String columnName;
    private String notDeletedValue;

    private Set<String> ignoreMapperIds;

    private List<String> ignoreTables;


    /**
     * Gets ignore mapper ids.
     *
     * @return the ignore mapper ids
     */
    public Set<String> getIgnoreMapperIds() {
        return ignoreMapperIds;
    }

    /**
     * Sets ignore mapper ids.
     *
     * @param ignoreMapperIds the ignore mapper ids
     */
    public void setIgnoreMapperIds(Set<String> ignoreMapperIds) {
        this.ignoreMapperIds = ignoreMapperIds;
    }

    /**
     * Gets ignore tables.
     *
     * @return the ignore tables
     */
    public List<String> getIgnoreTables() {
        return ignoreTables;
    }

    /**
     * Sets ignore tables.
     *
     * @param ignoreTables the ignore tables
     */
    public void setIgnoreTables(List<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    @Override
    public String getSqlDemo() {
        return this.sqlDemo;
    }

    @Override
    public String getNotDeletedValue() {
        return notDeletedValue;
    }

    /**
     * Sets not deleted value.
     *
     * @param notDeletedValue the not deleted value
     */
    public void setNotDeletedValue(String notDeletedValue) {
        this.notDeletedValue = notDeletedValue;
    }

    @Override
    public String getColumnName() {
        return this.columnName;
    }

    /**
     * Sets sql demo.
     *
     * @param sqlDemo the sql demo
     */
    public void setSqlDemo(String sqlDemo) {
        this.sqlDemo = sqlDemo;
    }

    /**
     * Sets column name.
     *
     * @param columnName the column name
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public boolean checkTableName(String tableName) {
        if(ignoreTables==null || ignoreTables.isEmpty()){
            return true;
        }
        return !ignoreTables.contains(tableName);
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        if(ignoreMapperIds==null || ignoreMapperIds.isEmpty()){
            return true;
        }
        for(String pattern:ignoreMapperIds){
            if(antPathMatcher.match(pattern,mapperId)) {return false;}
        }
        return true;
    }
}
