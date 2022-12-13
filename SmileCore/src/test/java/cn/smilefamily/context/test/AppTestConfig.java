package cn.smilefamily.context.test;

import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.annotation.Import;

@Configuration(name = "test", files = {"classpath:application.yml",
        "classpath:test1.yml", "classpath:test2.properties"}
        , scanPackages = {"cn.smilefamily.context.test"})
@Import(DevConfig.class)
public class AppTestConfig {

}
