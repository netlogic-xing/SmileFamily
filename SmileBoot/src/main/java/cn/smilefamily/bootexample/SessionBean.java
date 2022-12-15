package cn.smilefamily.bootexample;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Scope;
import cn.smilefamily.web.annotation.Session;

@Bean
@Session
public class SessionBean {
    private String name = System.currentTimeMillis()+"";

    @Override
    public String toString() {
        return "SessionBean{" +
                "name='" + name + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SessionBean() {
    }
}
