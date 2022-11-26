package cn.smilefamily.iocexample.external;

import cn.smilefamily.config.BeanConfig;
import cn.smilefamily.iocexample.AppConfig;
import cn.smilefamily.iocexample.service.HostService;

public class Application {
    public static void main(String[] args) {
        BeanConfig bc = new BeanConfig(AppConfig.class);
        bc.buildContext();
        HostService service = (HostService) bc.getContext().getBean(HostService.class);
        service.doAction();
    }
}
