package cn.smilefamily.bootexample;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Scope;

import java.util.Random;

@Bean
@Scope("prototype")
public class PrototypeBean {
    private String name = System.currentTimeMillis()+"";

    @Override
    public String toString() {
        return "PrototypeBean{" +
                "name='" + name + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PrototypeBean() {
    }
}