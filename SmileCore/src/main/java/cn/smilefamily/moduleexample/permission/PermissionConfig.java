package cn.smilefamily.moduleexample.permission;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.Export;
import cn.smilefamily.moduleexample.PermissionChecker;

@Configuration
public class PermissionConfig {
    @Bean
    @Export
    public PermissionChecker permissionChecker(){
        return new DatabasePermissionChecker();
    }
}
