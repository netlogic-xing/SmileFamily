package cn.smilefamily.event;
@FunctionalInterface
public interface EventPublisher<E> {
    public void publishEvent(E event);
}
