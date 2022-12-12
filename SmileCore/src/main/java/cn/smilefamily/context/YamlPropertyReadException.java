package cn.smilefamily.context;

import java.io.IOException;

public class YamlPropertyReadException extends RuntimeException {
    public YamlPropertyReadException() {
    }

    public YamlPropertyReadException(String message) {
        super(message);
    }

    public YamlPropertyReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlPropertyReadException(Throwable cause) {
        super(cause);
    }

    public YamlPropertyReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
