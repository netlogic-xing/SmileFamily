package cn.smilefamily.context.test;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Profile;

@Bean
@Profile(Profile.DEV)
public class BeanC {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
