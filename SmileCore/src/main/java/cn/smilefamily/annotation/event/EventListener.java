package cn.smilefamily.annotation.event;

import cn.smilefamily.annotation.AliasFor;
import cn.smilefamily.annotation.Attribute;
import cn.smilefamily.annotation.SameAs;
import cn.smilefamily.annotation.core.Bean;
import cn.smilefamily.event.ContextEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SameAs(value = Bean.class,
        attributes = {
                @Attribute(name = "value", always = "className")
        })
public @interface EventListener {
    @AliasFor("channel")
    String value() default "";

    @AliasFor("value")
    String channel() default "";

    Class<?> type() default ContextEvent.class;
}
