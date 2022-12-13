package cn.smilefamily.annotation;

import java.lang.annotation.*;

/**
 * 定义Bean别名，用于不同模块依赖相同的bean，但名字不同的情况
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Aliases.class)
public @interface Alias {
    String value();
}
