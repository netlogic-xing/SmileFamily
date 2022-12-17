package cn.smilefamily.iocexample.external.advice;

import cn.smilefamily.annotation.aop.Aspect;
import cn.smilefamily.annotation.aop.Before;
import cn.smilefamily.annotation.aop.WithBean;
import cn.smilefamily.annotation.aop.WithMethod;

@Aspect
public class LogAspect {
    @Before
    @WithBean("bean1")
    @WithMethod("doAction")
    public void log(String name, int age) {

    }
}
