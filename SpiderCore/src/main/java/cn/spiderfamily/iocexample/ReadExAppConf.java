package cn.spiderfamily.iocexample;

import cn.spiderfamily.config.BeanConfig;

public class ReadExAppConf {
    public static void main(String[] args) {
        BeanConfig beanConfig = new BeanConfig(ExAppConf.class);
        beanConfig.buildContext();
    }
}
