package cn.smilefamily.iocexample.external.advice;

import cn.smilefamily.annotation.aop.*;
import cn.smilefamily.iocexample.service.HostService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Aspect
public class LogAspect {
    @Before
    @Order(20)
    @WithType(HostService.class)
    @WithMethod("doSomething")
    public void log(String name, int age, @OrderValue int order) {
        System.out.println(order + "---->Hi, I am in aspect! name=2222=" + name + ", age=" + age);
    }
    @Before
    @Order(10)
    @WithType(HostService.class)
    @WithMethod("doSomething")
    public void check(String source, String name, int age, @OrderValue int order) {
        if(source == null){
            System.out.println( order + "---->source =111= null" + ", " + name+", " + age);
        }
    }
    @After
    @Order(10)
    @WithType(HostService.class)
    @WithMethod("doSomething")
    public void audit(@BeanName String name, @Return String result, @OrderValue int order) {
        System.out.println(order + "---->"+name+"-11-" + result);
    }

    @After
    @Order(20)
    @WithType(HostService.class)
    @WithMethod("doSomething")
    public void audit2(@BeanName String name, @Return String result, @OrderValue int order) {
        System.out.println(order + "---->"+name+"-22-" + result);
    }

    @Around
    @WithType(HostService.class)
    @WithMethod("doSomething")
    @Order(0)
    public Object transaction(@TargetObject Object target,  @TargetMethod Method targetMethod, @Args Object[] args, @OrderValue int order){
        System.out.println(order + ".... replace do something");
        try {
            System.out.println("execute method: " + targetMethod.getName());
            return targetMethod.invoke(target, args);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }finally {
            System.out.println(order + "....end...");
        }

    }
    @Around
    @WithType(HostService.class)
    @WithMethod("doSomething")
    @Order(12)
    public Object transaction2(@TargetObject Object target,  @TargetMethod Method targetMethod, @Args Object[] args, @OrderValue int order){
        System.out.println(order + "transaction2");
        try {
            System.out.println("execute method: " + targetMethod.getName());
            return targetMethod.invoke(target, args);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }finally {
            System.out.println(order + "....end...");
        }

    }
}
