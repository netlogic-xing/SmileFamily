package cn.smilefamily.context.test;

import cn.smilefamily.annotation.Profile;

@cn.smilefamily.annotation.Bean
@Profile(Profile.PROD)
public class BeanD {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
