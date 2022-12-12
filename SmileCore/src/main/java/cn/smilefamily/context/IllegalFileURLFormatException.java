package cn.smilefamily.context;

public class IllegalFileURLFormatException extends RuntimeException {
    public IllegalFileURLFormatException() {
    }

    public IllegalFileURLFormatException(String message) {
        super(message);
    }

    public IllegalFileURLFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalFileURLFormatException(Throwable cause) {
        super(cause);
    }

    public IllegalFileURLFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
