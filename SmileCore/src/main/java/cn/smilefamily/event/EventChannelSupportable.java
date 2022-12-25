package cn.smilefamily.event;

public interface EventChannelSupportable {
    <E> EventChannel<E> channel(Class<E> eventClass, String channel);

    default <E> EventChannel<E> channel(Class<E> eventClass) {
        return channel(eventClass, eventClass.getName());
    }

    default EventChannel<Object> channel(String channel) {
        return channel(Object.class, channel);
    }
}
