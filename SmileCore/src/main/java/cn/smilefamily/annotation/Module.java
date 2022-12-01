package cn.smilefamily.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 标注bean所属模块，主要用于给aspectj识别Bean所属模块使用。模块用于管理多个context。
 */
public @interface Module {
    String value() default "root";
}
