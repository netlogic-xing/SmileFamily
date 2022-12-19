package cn.smilefamily.moduleexample.biz;

import cn.smilefamily.context.ApplicationManager;
import cn.smilefamily.context.BeanContext;
import cn.smilefamily.moduleexample.common.CommonConfig;
import cn.smilefamily.moduleexample.permission.PermissionConfig;
import cn.smilefamily.moduleexample.portal.PortalConfig;
import cn.smilefamily.util.SmileUtils;

public class Application {
    public static void main(String[] args) {
        BeanContext root = new BeanContext("classpath:application.properties");
        ApplicationManager.getInstance().setRootContext(root);
        ApplicationManager.getInstance().addContext(new BeanContext(CommonConfig.class));
        ApplicationManager.getInstance().addContext(new BeanContext(PortalConfig.class));
        ApplicationManager.getInstance().addContext(new BeanContext(PermissionConfig.class));
        BeanContext mainContext = new BeanContext(AppConfig.class);

        ApplicationManager.getInstance().addContext(mainContext);
        ApplicationManager.getInstance().prepare();
        CarService carService = mainContext.inject(new CarService());
        ApplicationManager.getInstance().start();
        carService.add(new Car("xl's car", "BMW"));
    }
}
