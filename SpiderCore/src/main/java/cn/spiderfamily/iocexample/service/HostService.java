package cn.spiderfamily.iocexample.service;

import cn.spiderfamily.annotation.Injected;
import cn.spiderfamily.annotation.Bean;
import cn.spiderfamily.annotation.PostConstruct;
import cn.spiderfamily.iocexample.external.DataSource;

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
