package cn.spiderfamily.iocexample.external;

import cn.spiderfamily.config.BeanConfig;
import cn.spiderfamily.iocexample.AppConfig;
import cn.spiderfamily.iocexample.service.HostService;

public class Application {
    public static void main(String[] args) {
        BeanConfig bc = new BeanConfig(AppConfig.class);
        bc.buildContext();
        HostService service = (HostService) bc.getContext().getBean(HostService.class);
        service.doAction();
    }
}
