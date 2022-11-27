package cn.smilefamily.bootexample;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Configuration;
import cn.smilefamily.boot.WebApplication;
import cn.smilefamily.web.annotation.WebConfiguration;
import cn.smilefamily.web.annotation.WebInit;
import cn.smilefamily.web.view.ThymeleafViewRenderer;

import javax.servlet.ServletContext;

@WebConfiguration
@Configuration(scanPackages = "cn.smilefamily.bootexample.controllers")
public class Main extends WebApplication {
    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }
    @Bean
    public ThymeleafViewRenderer thymeleafViewRenderer(ServletContext servletContext){
        return new ThymeleafViewRenderer(servletContext);
    }
    @WebInit
    public void initWeb(){

    }
}
