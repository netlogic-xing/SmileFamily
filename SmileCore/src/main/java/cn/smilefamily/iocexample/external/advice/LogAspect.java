package cn.smilefamily.iocexample.external.advice;

import cn.smilefamily.annotation.aop.*;
import cn.smilefamily.iocexample.service.HostService;

@Aspect
public class LogAspect {
    @Before
    @WithType(HostService.class)
    @WithMethod("doSomething")
    public void log(String name, int age) {
        System.out.println("Hi, I am in aspect! name=" + name + ", age=" + age);
    }
}
