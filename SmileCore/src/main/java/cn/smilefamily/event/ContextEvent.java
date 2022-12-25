package cn.smilefamily.event;

import cn.smilefamily.context.Context;

/**
 * 代表容器事件的基类。在容器生命周期各个阶段会产生相应事件。
 * Prepared->Ready->PreDestroyed->Destroyed
 */
public class ContextEvent implements Event<Context> {
    private Context source;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public ContextEvent(Context source) {
        this.source = source;
    }

    @Override
    public Context getSource() {
        return source;
    }
}
