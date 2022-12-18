package cn.smilefamily.common;

public class ObjectInitializationException extends RuntimeException {

    public ObjectInitializationException() {
    }

    public ObjectInitializationException(String message) {
        super(message);
    }

    public ObjectInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectInitializationException(Throwable cause) {
        super(cause);
    }

    public ObjectInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
