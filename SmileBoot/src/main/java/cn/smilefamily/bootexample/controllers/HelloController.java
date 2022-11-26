package cn.smilefamily.bootexample.controllers;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.web.annotation.Controller;
import cn.smilefamily.web.annotation.Model;
import cn.smilefamily.web.annotation.RequestBody;
import cn.smilefamily.web.annotation.RequestMapping;
import cn.smilefamily.bootexample.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Bean
public class HelloController {
    @RequestMapping("/hello")
    public String hello() {
        return "/hello";
    }

    @RequestMapping("/home")
    public @Model(view = "/home") Map<String, Object> home() {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", "Chenyang");
        user.put("lastName", "Xing");
        return user;
    }

    @RequestMapping(method = "POST", value = "/user")
    public List<User> users(@RequestBody List<User> users) {
        return users;
    }
}
