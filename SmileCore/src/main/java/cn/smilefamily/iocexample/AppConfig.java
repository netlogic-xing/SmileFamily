package cn.smilefamily.iocexample;

import cn.smilefamily.annotation.core.*;
import cn.smilefamily.iocexample.external.DataSource;
import cn.smilefamily.iocexample.external.Driver;

@Configuration()
@ScanPackage("cn.smilefamily.iocexample.model")
@ScanPackage("cn.smilefamily.iocexample.service")
@ScanPackage("cn.smilefamily.iocexample.external")
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
