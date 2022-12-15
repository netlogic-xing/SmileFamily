package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Factory;

@Bean
public class B {
    private String name;
    private C c;
    @Factory
    public B(C c) {
        this.c = c;
    }
}
