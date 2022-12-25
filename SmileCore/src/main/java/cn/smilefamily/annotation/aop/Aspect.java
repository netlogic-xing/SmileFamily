package cn.smilefamily.annotation.aop;

import cn.smilefamily.annotation.Attribute;
import cn.smilefamily.annotation.SameAs;
import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.annotation.core.Scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SameAs(Bean.class)
public @interface Aspect {
    String value() default "";
}
