package cn.smilefamily.iocexample.service;

import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.PostConstruct;
import cn.smilefamily.iocexample.external.BeanByFactory;
import cn.smilefamily.iocexample.external.DataSource;

@Bean
public class HostService {
    @Injected
    private DataSource dataSource;

    @Injected
    private BeanByFactory beanByFactory;
    public void doAction() {
        System.out.println(dataSource);
    }

    @PostConstruct
    public void show() {
        System.out.println("bean created by factory"+beanByFactory.getName());
        System.out.println("post construct!");
    }
}
