package cn.smilefamily.event;

import cn.smilefamily.context.Context;

public class ContextPreparedEvent extends ContextEvent{
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public ContextPreparedEvent(Context source) {
        super(source);
    }
}
