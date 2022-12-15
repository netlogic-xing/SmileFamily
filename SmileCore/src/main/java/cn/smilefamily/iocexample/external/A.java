package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Factory;

@Bean
public class A {
    private String name;
    private B b;
    @Factory
    public A(B b) {
        this.b = b;
    }
}
