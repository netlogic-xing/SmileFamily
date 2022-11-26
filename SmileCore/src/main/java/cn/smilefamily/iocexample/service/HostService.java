package cn.smilefamily.iocexample.service;

import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.PostConstruct;
import cn.smilefamily.iocexample.external.DataSource;

@Bean
public class HostService {
    @Injected
    private DataSource dataSource;

    public void doAction() {
        System.out.println(dataSource);
    }

    @PostConstruct
    public void show() {
        System.out.println("post construct!");
    }
}
