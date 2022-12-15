package cn.smilefamily.context.test;

import cn.smilefamily.annotation.Alias;
import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.Profile;

@Configuration
@Profile(Profile.DEV)
public class DevConfig {
    @Bean("thePerson")
    @Alias("superman")
    @Alias("ironman")
    public Person thePerson(){
        return new Person();
    }
}
