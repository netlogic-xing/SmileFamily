package cn.smilefamily.bootexample;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Injected;
import cn.smilefamily.annotation.core.Scope;
import cn.smilefamily.web.annotation.Request;

@Bean
@Request
public class RequestBean {
    private String name = System.currentTimeMillis()+"";
    @Injected
    private SessionBean sessionBean;

    @Override
    public String toString() {
        return "RequestBean{" +
                "name='" + name + '\'' +
                ", sessionBean=" + sessionBean +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RequestBean() {
    }
}