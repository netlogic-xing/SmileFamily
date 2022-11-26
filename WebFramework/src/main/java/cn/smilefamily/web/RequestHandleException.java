package cn.smilefamily.web;

public class RequestHandleException extends RuntimeException {
    public RequestHandleException() {
    }

    public RequestHandleException(String message) {
        super(message);
    }

    public RequestHandleException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestHandleException(Throwable cause) {
        super(cause);
    }

    public RequestHandleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
