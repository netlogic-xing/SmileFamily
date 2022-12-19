package cn.smilefamily.iocexample.external;

import cn.smilefamily.context.BeanContext;
import cn.smilefamily.context.Context;
import cn.smilefamily.iocexample.AppConfig;
import cn.smilefamily.iocexample.service.HostService;
import cn.smilefamily.util.SmileUtils;

public class Application {
    public static void main(String[] args) {
       // SmileUtils.inspectConfig(AppConfig.class);
        Context bc = new BeanContext(AppConfig.class);
        bc.build();
        HostService service = bc.getBean(HostService.class);
        service.doAction();
        //service.doSomething("xinglu", 7, "home");
        service.doSomething("xr", 15, null);
        AspectBean aspectBean = new AspectBean("xing");
        aspectBean.show();
    }
}
