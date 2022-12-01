package cn.smilefamily.bootexample;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Scope;

@Bean
@Scope("session")
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
