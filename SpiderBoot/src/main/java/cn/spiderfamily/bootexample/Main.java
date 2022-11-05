package cn.spiderfamily.bootexample;

import cn.spiderfamily.annotation.Bean;
import cn.spiderfamily.annotation.Configuration;
import cn.spiderfamily.boot.WebApplication;
import cn.spiderfamily.web.annotation.WebConfiguration;
import cn.spiderfamily.web.annotation.WebInit;
import cn.spiderfamily.web.view.ThymeleafViewRenderer;

import javax.servlet.ServletContext;

@WebConfiguration
@Configuration(scanPackages = "cn.spiderfamily.bootexample.controllers")
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
