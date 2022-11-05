package cn.spiderfamily.config;

import java.io.IOException;

public class PropertiesLoadException extends RuntimeException {
    public PropertiesLoadException() {
    }

    public PropertiesLoadException(String message) {
        super(message);
    }

    public PropertiesLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertiesLoadException(Throwable cause) {
        super(cause);
    }

    public PropertiesLoadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
