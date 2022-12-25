package cn.smilefamily.iocexample.service;

import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.PostConstruct;
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

    @CustomizedName("actionAlias")
    public void doSomething(String name, int age, String source){
        System.out.println("In normal method: name = " + name + ", age = " + age + ", source = " +source);
    }

    @PostConstruct
    public void show() {
        System.out.println("bean created by factory"+beanByFactory.getName());
        System.out.println("post construct!");
    }
}
