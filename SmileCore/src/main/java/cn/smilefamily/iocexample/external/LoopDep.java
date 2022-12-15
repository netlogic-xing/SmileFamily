package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.ScanPackage;
import cn.smilefamily.context.BeanContext;
import cn.smilefamily.context.Context;

@Configuration()
@ScanPackage("cn.smilefamily.iocexample.external")
public class LoopDep {
    public static void main(String[] args) {
        Context bc = new BeanContext(LoopDep.class);
        bc.build();
    }
}
