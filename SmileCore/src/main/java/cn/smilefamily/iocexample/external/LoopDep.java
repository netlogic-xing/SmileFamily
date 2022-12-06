package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.context.BeanContext;
import cn.smilefamily.context.Context;

@Configuration(scanPackages = "cn.smilefamily.iocexample.external")
public class LoopDep {
    public static void main(String[] args) {
        Context bc = new BeanContext(LoopDep.class);
        bc.build();
    }
}
