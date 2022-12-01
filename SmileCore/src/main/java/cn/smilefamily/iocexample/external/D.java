package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Value;
@Bean
public class D {
    private String name;
    @Injected
    private A a;
    @Injected
    public D(@Value(value = "name",defaultValue = "xing") String name) {
        this.name = name;
    }
}
