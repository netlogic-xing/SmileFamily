package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Factory;
import cn.smilefamily.annotation.Injected;

@Bean
public class A {
    private String name;
    private B b;
    @Factory
    public A(B b) {
        this.b = b;
    }
}
