package cn.smilefamily.annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 标注Bean是导出的，其他的context中可见。在技术上，会将其导入到父context
 */
public @interface Export {
    //描述导出bean
    String value() default "";
}
