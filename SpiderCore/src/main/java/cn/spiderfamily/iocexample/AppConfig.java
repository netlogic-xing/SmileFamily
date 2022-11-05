package cn.spiderfamily.iocexample;

import cn.spiderfamily.annotation.Bean;
import cn.spiderfamily.annotation.Configuration;
import cn.spiderfamily.iocexample.external.DataSource;
import cn.spiderfamily.iocexample.external.Driver;

@Configuration(scanPackages = {"cn.spiderfamily.iocexample.model","cn.spiderfamily.iocexample.service"}, properties = "classpath:/application.properties")
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
