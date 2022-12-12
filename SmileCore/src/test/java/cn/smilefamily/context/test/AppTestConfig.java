package cn.smilefamily.context.test;

import cn.smilefamily.annotation.Configuration;

@Configuration(name="test", files={"classpath:application.yml",
        "classpath:test1.yml", "classpath:test2.properties"}
,scanPackages = {"cn.smilefamily.context.test"})
public class AppTestConfig {

}
