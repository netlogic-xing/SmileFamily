package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.core.Context;
import cn.smilefamily.annotation.core.Value;

@Context
public class AspectBean {
    private String name;
    @Value("${spring.url}")
    private String url;

    public AspectBean(String name) {
        this.name = name;
    }

    public void show() {
        System.out.println("in AspectBean name=" + name + ", url=" + url);
    }
}
