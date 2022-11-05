package cn.spiderfamily.web;

public class ParseParameterException extends RuntimeException {
    public ParseParameterException() {
    }

    public ParseParameterException(String message) {
        super(message);
    }

    public ParseParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseParameterException(Throwable cause) {
        super(cause);
    }

    public ParseParameterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
