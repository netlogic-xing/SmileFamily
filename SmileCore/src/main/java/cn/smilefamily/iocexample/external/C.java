package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Factory;

@Bean
public class C {
    private String name;
    private D d;
    @Factory
    public C(D d) {
        this.d = d;
    }
}
