package cn.smilefamily.event;

import cn.smilefamily.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class ContextEventMulticaster<E> implements ApplicationEventMulticaster<E> {
    private static final Logger logger = LoggerFactory
            .getLogger(ContextEventMulticaster.class);
    private String channelName;
    private Class<E> channelType;
    private Context context;
    private List<ListenerPair> listeners = new CopyOnWriteArrayList<>();
    public ContextEventMulticaster(Class<E> eventClass, Context context) {
        this.context = context;
        this.channelType = eventClass;
        this.channelName = context.getName();
    }

    public ContextEventMulticaster(Class<E> eventClass, String channelName, Context context) {
        this.channelName = channelName;
        this.channelType = eventClass;
        this.context = context;
    }

    @Override
    public void addEventListener(Predicate<? extends E> matcher, ApplicationEventListener<? extends E> applicationEventListener) {
        listeners.add(new ListenerPair(matcher, applicationEventListener));
    }

    @Override
    public void removeAllEventListeners() {

    }

    @Override
    public void removeEventListener(Predicate<ApplicationEventListener<? extends E>> predicate) {

    }

    @Override
    public EventPublisher<E> getEventPublisher() {
        return e -> this.multicastEvent(e);
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    @Override
    public Class<E> getChannelType() {
        return channelType;
    }

    @Override
    public void multicastEvent(E event) {
        //简单实现，不考虑并发
        listeners.stream().filter(pair -> pair.predicate.test(event))
                .map(pair -> pair.listener).forEach(l -> {
                            l.onEvent(event);
                        }
                );
    }

    @Override
    public void removeEventListener(String listenerBeanName) {

    }

    @Override
    public Context getContext() {
        return context;
    }

    private record ListenerPair(Predicate predicate, ApplicationEventListener listener) {
    }
}
