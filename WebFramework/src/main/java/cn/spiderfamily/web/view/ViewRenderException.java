package cn.spiderfamily.web.view;

import java.io.IOException;

public class ViewRenderException extends RuntimeException {
    public ViewRenderException() {
    }

    public ViewRenderException(String message) {
        super(message);
    }

    public ViewRenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ViewRenderException(Throwable cause) {
        super(cause);
    }

    public ViewRenderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
