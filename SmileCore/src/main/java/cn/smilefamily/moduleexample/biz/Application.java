package cn.smilefamily.moduleexample.biz;

import cn.smilefamily.context.ApplicationManager;
import cn.smilefamily.context.BeanContext;
import cn.smilefamily.event.ApplicationEventListener;
import cn.smilefamily.event.ContextEvent;
import cn.smilefamily.event.ContextReadyEvent;
import cn.smilefamily.moduleexample.common.CommonConfig;
import cn.smilefamily.moduleexample.permission.PermissionConfig;
import cn.smilefamily.moduleexample.portal.PortalConfig;

public class Application {
    public static void main(String[] args) {
        BeanContext root = new BeanContext("classpath:application.properties");
        ApplicationManager.getInstance().setRootContext(root);
        ApplicationManager.getInstance().addContext(new BeanContext(CommonConfig.class));
        ApplicationManager.getInstance().addContext(new BeanContext(PortalConfig.class));
        ApplicationManager.getInstance().addContext(new BeanContext(PermissionConfig.class));
        BeanContext mainContext = new BeanContext(AppConfig.class);
        mainContext.addEventListener(ContextReadyEvent.class, event -> {
            System.out.println(event.getClass());
        });

        mainContext.channel(String.class,"test1").addEventListener(e->{
            System.out.println(e);
        });

        mainContext.channel(Object.class).addEventListener(e->{
            System.out.println("---"+e);
        });

        mainContext.channel("default type channel").addEventListener(e->{
            System.out.println("===e"+e);
        });

        ApplicationManager.getInstance().addContext(mainContext);
        ApplicationManager.getInstance().prepare();
        CarService carService = mainContext.inject(new CarService());
        ApplicationManager.getInstance().start();
        carService.add(new Car("xl's car", "BMW"));
        mainContext.channel(String.class, "test1").getEventPublisher().publishEvent("test string channel");
        mainContext.channel(Object.class).getEventPublisher().publishEvent("xxxx");
        mainContext.channel("default type channel").getEventPublisher().publishEvent("in default type channel");

    }
}
