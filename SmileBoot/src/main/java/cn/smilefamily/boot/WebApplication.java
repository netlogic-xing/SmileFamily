package cn.smilefamily.boot;

import cn.smilefamily.web.base.DispatchServlet;

/**
 * "Springboot"程序启动类基类
 */
public class WebApplication extends Application{
    private JettyServer jettyServer;

    public WebApplication() {
        jettyServer = new JettyServer();
        this.getApplicationContext().addBeanAndInjectDependencies(jettyServer);
        jettyServer.register("/*", DispatchServlet.class);
    }
    public void start(){
        try {
            jettyServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
