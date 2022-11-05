package cn.spiderfamily.web;

public class WebConfigNotFoundException extends RuntimeException {
    public WebConfigNotFoundException() {
    }

    public WebConfigNotFoundException(String message) {
        super(message);
    }

    public WebConfigNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebConfigNotFoundException(Throwable cause) {
        super(cause);
    }

    public WebConfigNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
