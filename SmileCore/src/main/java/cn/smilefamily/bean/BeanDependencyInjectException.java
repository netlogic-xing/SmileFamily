package cn.smilefamily.bean;

public class BeanDependencyInjectException extends RuntimeException {
    public BeanDependencyInjectException() {
    }

    public BeanDependencyInjectException(String message) {
        super(message);
    }

    public BeanDependencyInjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanDependencyInjectException(Throwable cause) {
        super(cause);
    }

    public BeanDependencyInjectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
