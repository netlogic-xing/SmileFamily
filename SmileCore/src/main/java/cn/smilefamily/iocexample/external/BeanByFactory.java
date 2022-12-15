package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Factory;

import java.util.UUID;

@Bean
public class BeanByFactory {
    private String name = UUID.randomUUID().toString();

    private BeanByFactory() {
    }

    public String getName() {
        return name;
    }
    @Factory
    public static BeanByFactory getInstance(){
        return new BeanByFactory();
    }
    public void setName(String name) {
        this.name = name;
    }
}
