package cn.smilefamily.web.annotation;

import cn.smilefamily.annotation.SameAs;
import cn.smilefamily.annotation.core.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SameAs(Configuration.class)
public @interface WebConfiguration {
    String value() default "webConfig";
}
