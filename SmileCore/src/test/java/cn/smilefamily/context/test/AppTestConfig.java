package cn.smilefamily.context.test;

import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.Import;
import cn.smilefamily.annotation.core.PropertySource;
import cn.smilefamily.annotation.core.ScanBean;

@Configuration("test")
@ScanBean("cn.smilefamily.context.test")
@PropertySource("classpath:application.yml")
@PropertySource("classpath:test1.yml")
@PropertySource("classpath:test2.properties")
@Import(DevConfig.class)
public class AppTestConfig {

}
