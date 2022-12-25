package cn.smilefamily.event;

import java.util.EventListener;

@FunctionalInterface
public interface ApplicationEventListener<E> extends EventListener {
    void onEvent(E event);
}
