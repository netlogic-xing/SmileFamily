package cn.smilefamily.annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于定义Bean。不同于spring的@Bean注解仅用于再JavaConfig中定义，在本框架中，Bean=Spring的Bean+Component
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    //指定bean名称，如果不指定，默认使用类名
    String value() default "";
}
