package cn.smilefamily.bootexample.config;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Export;
import cn.smilefamily.annotation.core.ScanPackage;
import cn.smilefamily.web.annotation.WebConfiguration;
import cn.smilefamily.web.annotation.WebInit;
import cn.smilefamily.web.view.ThymeleafViewRenderer;

import javax.servlet.ServletContext;

@WebConfiguration
@ScanPackage("cn.smilefamily.bootexample")
public class WebConfig {
    @Bean
    @Export
    public ThymeleafViewRenderer thymeleafViewRenderer(ServletContext servletContext){
        return new ThymeleafViewRenderer(servletContext);
    }
    @WebInit
    public void initWeb(){

    }
}
