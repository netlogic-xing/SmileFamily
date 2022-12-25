package cn.smilefamily.event;

import cn.smilefamily.context.Context;

public class ContextReadyEvent extends ContextEvent{
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public ContextReadyEvent(Context source) {
        super(source);
    }
}
