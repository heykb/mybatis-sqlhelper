package io.github.heykb.sqlhelper.spring;

import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import org.springframework.util.AntPathMatcher;

import java.util.List;

public class PropertyLogicDeleteInfoHandler extends LogicDeleteInfoHandler {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher(".");
    private String deleteSqlDemo = "UPDATE a SET is_deleted = 'Y'";
    private String columnName = "is_deleted";
    private String notDeletedValue = "'N'";

    private List<String> ignoreMapperIds;

    private List<String> ignoreTables;


    /**
     * Gets ignore mapper ids.
     *
     * @return the ignore mapper ids
     */
    public List<String> getIgnoreMapperIds() {
        return ignoreMapperIds;
    }

    /**
     * Sets ignore mapper ids.
     *
     * @param ignoreMapperIds the ignore mapper ids
     */
    public void setIgnoreMapperIds(List<String> ignoreMapperIds) {
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
    public String getDeleteSqlDemo() {
        return this.deleteSqlDemo;
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

    public void setDeleteSqlDemo(String deleteSqlDemo) {
        this.deleteSqlDemo = deleteSqlDemo;
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

