package cn.smilefamily.boot;


import cn.smilefamily.context.BeanContext;

public class Application {
    private BeanContext applicationBeanContext;
    public Application() {
        applicationBeanContext = new BeanContext("classpath:/application.properties");
        applicationBeanContext.build();
    }
    public BeanContext getApplicationContext() {
        return applicationBeanContext;
    }
}
