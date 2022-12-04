package cn.smilefamily.iocexample;

import cn.smilefamily.context.Context;

public class ReadExAppConf {
    public static void main(String[] args) {
        Context beanConfig = new Context(ExAppConf.class);
        beanConfig.build();
    }
}
