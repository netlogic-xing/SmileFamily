package cn.smilefamily.web;

public class ControllerConfigException extends RuntimeException {
    public ControllerConfigException() {
    }

    public ControllerConfigException(String message) {
        super(message);
    }

    public ControllerConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ControllerConfigException(Throwable cause) {
        super(cause);
    }

    public ControllerConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
