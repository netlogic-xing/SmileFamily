package cn.smilefamily.boot;


import cn.smilefamily.context.Context;

public class Application {
    private Context applicationContext;
    public Application() {
        applicationContext = new Context("classpath:/application.properties");
        applicationContext.build();
    }
    public Context getApplicationContext() {
        return applicationContext;
    }
}
