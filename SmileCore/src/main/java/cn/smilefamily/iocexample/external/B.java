package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Factory;
import cn.smilefamily.annotation.Injected;

@Bean
public class B {
    private String name;
    private C c;
    @Factory
    public B(C c) {
        this.c = c;
    }
}
