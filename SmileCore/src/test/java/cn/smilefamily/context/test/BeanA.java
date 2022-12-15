package cn.smilefamily.context.test;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Export;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.Value;

@Bean
@Export("This is a demo for export")
public class BeanA {
    @Injected("/family/dad/name")
    private String name;
   @Value("${bean.url}")
    private String url;
   @Value("${bean.port}")
    private int port;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
