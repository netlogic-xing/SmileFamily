package cn.smilefamily.extension;

public class LoadExtensionException extends RuntimeException {
    public LoadExtensionException() {
    }

    public LoadExtensionException(String message) {
        super(message);
    }

    public LoadExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadExtensionException(Throwable cause) {
        super(cause);
    }

    public LoadExtensionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
