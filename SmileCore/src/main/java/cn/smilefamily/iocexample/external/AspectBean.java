package cn.smilefamily.iocexample.external;

import cn.smilefamily.annotation.Context;
import cn.smilefamily.annotation.Value;

@Context("mainConfig")
public class AspectBean {
    private String name;
    @Value("spring.url")
    private String url;

    public AspectBean(String name) {
        this.name = name;
    }

    public void show() {
        System.out.println("in AspectBean name=" + name + ", url=" + url);
    }
}
