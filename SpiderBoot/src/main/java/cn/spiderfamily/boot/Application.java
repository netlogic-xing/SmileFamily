package cn.spiderfamily.boot;

import cn.spiderfamily.config.BeanConfig;
import cn.spiderfamily.context.Context;

public class Application {
    private Context applicationContext;
    private BeanConfig applicationConfig;

    public Application() {
        applicationConfig = new BeanConfig("classpath:/application.properties");
        applicationConfig.buildContext();
        applicationContext = applicationConfig.getContext();
    }

    public BeanConfig getApplicationConfig() {
        return applicationConfig;
    }

    public Context getApplicationContext() {
        return applicationContext;
    }
}
