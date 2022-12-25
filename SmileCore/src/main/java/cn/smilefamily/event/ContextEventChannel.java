package cn.smilefamily.event;

import cn.smilefamily.context.BeanFactory;
import cn.smilefamily.context.Context;

import java.util.function.Predicate;

public interface ContextEventChannel<E> extends EventChannel<E> {

    void removeEventListener(String listenerBeanName);
    default void addEventListener(Class<? extends E> eventClass, ApplicationEventListener<? extends E> applicationEventListener) {
        addEventListener(event -> eventClass.isAssignableFrom(event.getClass()), applicationEventListener);
    }


    Context getContext();
}
