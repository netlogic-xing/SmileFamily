package cn.smilefamily.context.test;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.annotation.Profile;

@Configuration
@Profile(Profile.DEV)
public class DevConfig {
    @Bean(name="thePerson")
    public Person thePerson(){
        return new Person();
    }
}
