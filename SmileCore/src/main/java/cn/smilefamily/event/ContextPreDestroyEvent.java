package cn.smilefamily.event;

import cn.smilefamily.context.Context;

public class ContextPreDestroyEvent extends ContextEvent{
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public ContextPreDestroyEvent(Context source) {
        super(source);
    }
}
