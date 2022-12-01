package cn.smilefamily.iocexample.external;

import cn.smilefamily.context.Context;
import cn.smilefamily.iocexample.AppConfig;
import cn.smilefamily.iocexample.service.HostService;

public class Application {
    public static void main(String[] args) {
        Context bc = new Context(AppConfig.class);
        bc.build();
        HostService service = (HostService) bc.getBean(HostService.class);
        service.doAction();
    }
}
