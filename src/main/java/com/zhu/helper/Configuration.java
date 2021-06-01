package com.zhu.helper;

import java.util.Map;

/**
 * @author heykb
 */
public class Configuration {
    private Map<String,String> resultPropertiesMap;
    private boolean isMapUnderscoreToCamelCase = true;

    public Configuration(Map<String, String> resultPropertiesMap, boolean isMapUnderscoreToCamelCase) {
        this.resultPropertiesMap = resultPropertiesMap;
        this.isMapUnderscoreToCamelCase = isMapUnderscoreToCamelCase;
    }

    public Configuration() {
    }

    public Map<String, String> getResultPropertiesMap() {
        return resultPropertiesMap;
    }

    public void setResultPropertiesMap(Map<String, String> resultPropertiesMap) {
        this.resultPropertiesMap = resultPropertiesMap;
    }

    public boolean isMapUnderscoreToCamelCase() {
        return isMapUnderscoreToCamelCase;
    }

    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
        isMapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
    }
}
