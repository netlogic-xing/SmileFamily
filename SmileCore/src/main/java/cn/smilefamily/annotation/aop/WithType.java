package cn.smilefamily.annotation.aop;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithTypes.class)
public @interface WithType {
    //bean name
    Class<?> value();
}