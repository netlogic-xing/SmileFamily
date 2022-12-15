package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Factory;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.Value;
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
