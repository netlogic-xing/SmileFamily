package cn.smilefamily.bootexample;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Injected;
import cn.smilefamily.annotation.Scope;

@Bean
@Scope("request")
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