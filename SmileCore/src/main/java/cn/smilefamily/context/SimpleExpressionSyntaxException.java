package cn.smilefamily.context;

public class SimpleExpressionSyntaxException extends RuntimeException {
    public SimpleExpressionSyntaxException() {
    }

    public SimpleExpressionSyntaxException(String message) {
        super(message);
    }

    public SimpleExpressionSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public SimpleExpressionSyntaxException(Throwable cause) {
        super(cause);
    }

    public SimpleExpressionSyntaxException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
