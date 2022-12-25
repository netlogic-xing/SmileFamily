package cn.smilefamily.event;

public interface ApplicationEventMulticaster<E> extends ContextEventChannel<E> {
    void multicastEvent(E event);
}
