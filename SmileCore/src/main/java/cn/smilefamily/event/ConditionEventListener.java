package cn.smilefamily.event;


import cn.smilefamily.annotation.event.EventListener;
import cn.smilefamily.annotation.event.OnEvent;
import cn.smilefamily.common.MiscUtils;
import cn.smilefamily.context.BeanFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static cn.smilefamily.annotation.EnhancedAnnotatedElement.wrap;

public class ConditionEventListener<E> implements ApplicationEventListener<E> {
    private String listenerBeanName;
    private Method eventHandle;

    private String channel;
    private Class<E> channelType;

    private BeanFactory factory;
    private Predicate<E> predicate;

    public static List<ConditionEventListener> constructs(BeanFactory factory, Class<?> beanType, String beanName) {
        return Arrays.stream(beanType.getMethods())
                .filter(m -> wrap(m).isAnnotationPresent(OnEvent.class))
                .map(m -> new ConditionEventListener(factory, m, beanType, beanName)).toList();
    }

    public ConditionEventListener(BeanFactory factory, Method eventHandle, Class<?> beanType, String beanBean) {
        this.listenerBeanName = beanBean;
        this.factory = factory;
        this.eventHandle = eventHandle;
        EventListener eventListener = wrap(beanType).getAnnotation(EventListener.class);
        channelType = (Class<E>) eventListener.type();
        if (eventHandle.getParameterTypes().length != 1 || !channelType.isAssignableFrom(eventHandle.getParameterTypes()[0])) {
            throw new EventHandleMethodIllegalException("Event handle " + eventHandle + " be allowed to have only one parameter and parameter type must be " + channelType + " or it's subclass.");
        }
        Class<?> paramType = eventHandle.getParameterTypes()[0];
        channel = eventListener.channel();
        OnEvent onEvent = wrap(eventHandle).getAnnotation(OnEvent.class);
        this.predicate = e -> paramType.isAssignableFrom(e.getClass())
                && (onEvent.classes().length == 0 || Arrays.stream(onEvent.classes()).anyMatch(c -> c.isAssignableFrom(e.getClass())))
                && conditionMet(onEvent.condition());
    }

    private boolean conditionMet(String condition) {
        return true;
    }

    public Predicate<E> matcher() {
        return predicate;
    }

    @Override
    public void onEvent(E event) {
        MiscUtils.invoke(eventHandle, factory.getBean(listenerBeanName), event);
    }
}
