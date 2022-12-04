package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Factory;
import cn.smilefamily.annotation.Injected;

@Bean
public class C {
    private String name;
    private D d;
    @Factory
    public C(D d) {
        this.d = d;
    }
}
