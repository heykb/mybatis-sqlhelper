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

    private Set<String> checkTableNames;

    private List<String> checkMapperIds;

    /**
     * Gets ignore tables.
     *
     * @return the ignore tables
     */
    public Set<String> getcheckTableNames() {
        return checkTableNames;
    }

    /**
     * Sets ignore tables.
     *
     * @param checkTableNames the ignore tables
     */
    public void setcheckTableNames(Set<String> checkTableNames) {
        this.checkTableNames = checkTableNames;
    }

    /**
     * Gets ignore mapper ids.
     *
     * @return the ignore mapper ids
     */
    public List<String> getcheckMapperIds() {
        return checkMapperIds;
    }

    /**
     * Sets ignore mapper ids.
     *
     * @param checkMapperIds the ignore mapper ids
     */
    public void setcheckMapperIds(List<String> checkMapperIds) {
        this.checkMapperIds = checkMapperIds;
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
        if(checkTableNames==null || checkTableNames.isEmpty()){
            return false;
        }
        return checkTableNames.contains(tableName);
    }

    @Override
    public boolean checkMapperId(String mapperId) {
        if(checkMapperIds==null || checkMapperIds.isEmpty()){
            return false;
        }
        for(String pattern:checkMapperIds){
            if(antPathMatcher.match(pattern,mapperId)) {return true;}
        }
        return false;
    }
}
