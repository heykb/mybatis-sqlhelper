package io.github.heykb.sqlhelper.config;

/**
 * @author heykb
 */
public class SqlHelperException extends RuntimeException {
    public SqlHelperException(Throwable cause) {
        super(cause);
    }

    public SqlHelperException(String message, Throwable cause) {
        super(message, cause);
    }


    public SqlHelperException(String message){
        super(message);
    }
}
