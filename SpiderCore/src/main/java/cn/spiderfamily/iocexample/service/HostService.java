package cn.spiderfamily.iocexample.service;

import cn.spiderfamily.annotation.Autowired;
import cn.spiderfamily.annotation.Bean;
import cn.spiderfamily.annotation.PostConstruct;
import cn.spiderfamily.iocexample.external.DataSource;

@Bean
public class HostService {
    @Autowired
    private DataSource dataSource;

    public void doAction() {
        System.out.println(dataSource);
    }

    @PostConstruct
    public void show() {
        System.out.println("post construct!");
    }
}
