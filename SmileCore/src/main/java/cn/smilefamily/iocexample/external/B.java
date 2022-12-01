package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Injected;

@Bean
public class B {
    private String name;
    private C c;
    @Injected
    public B(C c) {
        this.c = c;
    }
}
