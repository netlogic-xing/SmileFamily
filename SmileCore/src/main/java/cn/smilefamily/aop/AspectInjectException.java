package cn.smilefamily.aop;

public class AspectInjectException extends RuntimeException {
    public AspectInjectException() {
    }

    public AspectInjectException(String message) {
        super(message);
    }

    public AspectInjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public AspectInjectException(Throwable cause) {
        super(cause);
    }

    public AspectInjectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
