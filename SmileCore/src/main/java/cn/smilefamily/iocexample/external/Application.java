package cn.smilefamily.iocexample.external;

import cn.smilefamily.context.Context;
import cn.smilefamily.iocexample.AppConfig;
import cn.smilefamily.iocexample.service.HostService;
import cn.smilefamily.util.SmileUtils;

public class Application {
    public static void main(String[] args) {
        SmileUtils.inspectConfig(AppConfig.class);
        Context bc = new Context(AppConfig.class);
        bc.build();
        HostService service = (HostService) bc.getBean(HostService.class);
        service.doAction();
    }
}
