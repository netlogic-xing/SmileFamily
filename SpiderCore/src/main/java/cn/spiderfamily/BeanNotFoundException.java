package cn.spiderfamily;

public class BeanNotFoundException extends RuntimeException {
    public BeanNotFoundException() {
    }

    public BeanNotFoundException(String message) {
        super(message);
    }

    public BeanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanNotFoundException(Throwable cause) {
        super(cause);
    }

    public BeanNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
