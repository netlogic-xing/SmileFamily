package cn.smilefamily.event;

import java.util.function.Predicate;

public interface EventChannel<E> {
    default void addEventListener(ApplicationEventListener<? extends E> applicationEventListener) {
        addEventListener(event -> true, applicationEventListener);
    }

    String getChannelName();

    Class<E> getChannelType();

    void addEventListener(Predicate<? extends E> matcher, ApplicationEventListener<? extends E> applicationEventListener);

    void removeAllEventListeners();

    void removeEventListener(Predicate<ApplicationEventListener<? extends E>> predicate);

    EventPublisher<E> getEventPublisher();
}
