package cn.smilefamily.bootexample.controllers;

import cn.smilefamily.annotation.Bean;
import cn.smilefamily.annotation.Injected;
import cn.smilefamily.bootexample.PrototypeBean;
import cn.smilefamily.bootexample.RequestBean;
import cn.smilefamily.bootexample.SessionBean;
import cn.smilefamily.bootexample.User;
import cn.smilefamily.web.annotation.Controller;
import cn.smilefamily.web.annotation.Model;
import cn.smilefamily.web.annotation.RequestBody;
import cn.smilefamily.web.annotation.RequestMapping;
import cn.smilefamily.web.base.BeanContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@Bean
public class HelloController {
    @Injected
    private PrototypeBean prototypeBean;
    @Injected
    private PrototypeBean prototypeBean2;
    @Injected
    private RequestBean requestBean;
    @Injected
    private SessionBean sessionBean;

    @RequestMapping("/hello")
    public String hello() {
//        BeanContextHolder.getContext().createScope("request", new ConcurrentHashMap<>());
//        BeanContextHolder.getContext().createScope("session", new ConcurrentHashMap<>());
        System.out.println("proto1=" + prototypeBean+ "@" + System.identityHashCode(prototypeBean));
        System.out.println("proto2=" + prototypeBean2 + "@" + System.identityHashCode(prototypeBean2));
        System.out.println("requestBean=" + requestBean + "@" + System.identityHashCode(requestBean));
        System.out.println("sessionBean=" + sessionBean + "@" + System.identityHashCode(sessionBean));

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
