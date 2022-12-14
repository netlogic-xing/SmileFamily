package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Factory;
import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Value;
@Bean
public class D {
    private String name;
    @Injected
    private A a;
    @Factory
    public D(@Value(value = "${name:xingchenyang}") String name) {
        this.name = name;
    }
}
