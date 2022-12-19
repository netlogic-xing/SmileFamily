package cn.smilefamily.iocexample;

import cn.smilefamily.annotation.aop.ScanAspect;
import cn.smilefamily.annotation.core.*;
import cn.smilefamily.iocexample.external.DataSource;
import cn.smilefamily.iocexample.external.Driver;

@Configuration()
@ScanPackage("cn.smilefamily.iocexample.model")
@ScanPackage("cn.smilefamily.iocexample.service")
@ScanPackage("cn.smilefamily.iocexample.external")
@ScanAspect("cn.smilefamily.iocexample.external.advice")
@PropertySource("classpath:application.properties")
@PropertySource("classpath:application.yml")
public class AppConfig {

    @Bean
    @Export("for test")
    public Driver driver() {
        return new Driver("dr1");
    }

    @Bean
    public DataSource dataSource(@External("for test") Driver driver) {
        DataSource ds = new DataSource("DS1");
        ds.setDriver(driver);
        return ds;
    }
}
