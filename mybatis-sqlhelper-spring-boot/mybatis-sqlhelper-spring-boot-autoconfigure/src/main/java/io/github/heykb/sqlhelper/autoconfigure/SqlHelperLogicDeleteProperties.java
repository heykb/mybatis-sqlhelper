package io.github.heykb.sqlhelper.autoconfigure;

import io.github.heykb.sqlhelper.spring.PropertyLogicDeleteInfoHandler;


public class SqlHelperLogicDeleteProperties extends PropertyLogicDeleteInfoHandler {
    /**
     * Physical delete to logical delete switch
     */
    private boolean enable = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
