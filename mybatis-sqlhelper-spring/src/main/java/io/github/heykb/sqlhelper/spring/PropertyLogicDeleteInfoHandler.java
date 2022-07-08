package io.github.heykb.sqlhelper.spring;

import io.github.heykb.sqlhelper.handler.abstractor.LogicDeleteInfoHandler;
import io.github.heykb.sqlhelper.utils.CommonUtils;
import org.springframework.util.AntPathMatcher;

import java.util.List;

public class PropertyLogicDeleteInfoHandler extends LogicDeleteInfoHandler {
    static final AntPathMatcher antPathMatcher = new AntPathMatcher(".");
    private String deleteSqlDemo = "UPDATE a SET is_deleted = 'Y'";
    private String columnName = "is_deleted";
    private String notDeletedValue = "'N'";

    private List<String> mapperIds;

    private List<String> tables;

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

    public List<String> getMapperIds() {
        return mapperIds;
    }

    public void setMapperIds(List<String> mapperIds) {
        this.mapperIds = mapperIds;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
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
        if(tables!=null && !wildcardMatchAny(this.tables,tableName)){
            return false;
        }
        if(ignoreTables!=null && wildcardMatchAny(ignoreTables,tableName)){
            return false;
        }
        return true;
    }


    @Override
    public boolean checkMapperId(String mapperId) {
        if(mapperIds!=null && !pathMatchAny(this.mapperIds,mapperId)){
            return false;
        }
        if(ignoreMapperIds!=null && pathMatchAny(ignoreMapperIds,mapperId)){
            return false;
        }
        return true;
    }

    boolean wildcardMatchAny(List<String> patterns,String target){
        for(String pattern:patterns){
            if(CommonUtils.wildcardMatch(pattern,target)){
                return true;
            }
        }
        return false;
    }

    boolean pathMatchAny(List<String> patterns,String target){
        for(String pattern:patterns){
            if(antPathMatcher.match(pattern,target)){
                return true;
            }
        }
        return false;
    }

}

