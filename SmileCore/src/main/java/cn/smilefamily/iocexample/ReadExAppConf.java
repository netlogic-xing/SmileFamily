package cn.smilefamily.iocexample;

import cn.smilefamily.config.BeanConfig;

public class ReadExAppConf {
    public static void main(String[] args) {
        BeanConfig beanConfig = new BeanConfig(ExAppConf.class);
        beanConfig.buildContext();
    }
}
