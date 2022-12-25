package cn.smilefamily.moduleexample.biz;

import cn.smilefamily.annotation.event.EventListener;
import cn.smilefamily.annotation.event.OnEvent;
import cn.smilefamily.event.ContextReadyEvent;

@EventListener
public class ApplicationEventListener {
    @OnEvent
    public void onContextStarted(ContextReadyEvent event) {
        System.out.println("BeanListener event: " + event.getSource().getName());
    }
}
