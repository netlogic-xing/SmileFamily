package cn.smilefamily.event;

public class EventHandleMethodIllegalException extends RuntimeException {
    public EventHandleMethodIllegalException() {
    }

    public EventHandleMethodIllegalException(String message) {
        super(message);
    }

    public EventHandleMethodIllegalException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventHandleMethodIllegalException(Throwable cause) {
        super(cause);
    }

    public EventHandleMethodIllegalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
