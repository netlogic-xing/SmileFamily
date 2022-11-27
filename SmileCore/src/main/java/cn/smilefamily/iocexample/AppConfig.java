package cn.smilefamily.iocexample;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.iocexample.external.DataSource;
import cn.smilefamily.iocexample.external.Driver;

@Configuration(scanPackages = {"cn.smilefamily.iocexample.model","cn.smilefamily.iocexample.service"}, properties = "classpath:/application.properties")
public class AppConfig {

    @Bean
    public Driver driver(){
        return new Driver("dr1");
    }

    @Bean
    public DataSource dataSource(Driver driver){
        DataSource ds = new DataSource("DS1");
        ds.setDriver(driver);
        return ds;
    }
}
