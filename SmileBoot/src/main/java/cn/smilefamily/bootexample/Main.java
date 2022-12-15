package cn.smilefamily.bootexample;

import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Configuration;
import cn.smilefamily.annotation.core.ScanPackage;
import cn.smilefamily.boot.WebApplication;
import cn.smilefamily.web.annotation.WebConfiguration;
import cn.smilefamily.web.annotation.WebInit;
import cn.smilefamily.web.view.ThymeleafViewRenderer;

import javax.servlet.ServletContext;

@WebConfiguration
@ScanPackage("cn.smilefamily.bootexample")
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
