package cn.smilefamily.annotation.core;

import cn.smilefamily.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 表示自动注入依赖。标注在字段和参数上表示所标注对象自动从context中获取依赖。标注在方法上表示自动执行该方法(通过执行方法获取依赖)
 */
public @interface Injected {
    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

    boolean required() default true;
}
