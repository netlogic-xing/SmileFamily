package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.context.Context;

@Configuration(scanPackages = "cn.smilefamily.iocexample.external")
public class LoopDep {
    public static void main(String[] args) {
        Context bc = new Context(LoopDep.class);
        bc.build();
    }
}
