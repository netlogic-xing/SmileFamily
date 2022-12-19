package cn.smilefamily.annotation.aop;

import cn.smilefamily.annotation.Alias;
import cn.smilefamily.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@java.lang.annotation.Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Arg {
    @AliasFor("value")
    String name() default "";
    @AliasFor("name")
    String value() default "";

    int index() default -1;
}
