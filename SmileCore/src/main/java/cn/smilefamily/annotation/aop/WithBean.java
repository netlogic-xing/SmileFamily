package cn.smilefamily.annotation.aop;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithBeans.class)
public @interface WithBean {
    //bean name
    String value();
}
