package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Injected;

@Bean
public class A {
    private String name;
    private B b;
    @Injected
    public A(B b) {
        this.b = b;
    }
}
