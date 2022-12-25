package cn.smilefamily.annotation.aop.match;

import cn.smilefamily.aop.BeanSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SelectBean {
    Class<? extends BeanSelector> value();
}
