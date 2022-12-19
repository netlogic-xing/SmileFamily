package cn.smilefamily.moduleexample.common;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.Export;

@Configuration
public class CommonConfig {
    @Bean
    @Export("公共数据源")
    public Datasource datasource(){
        return new Datasource() {
            @Override
            public Connection getConnection() {
                return Datasource.super.getConnection();
            }
        };
    }
}
