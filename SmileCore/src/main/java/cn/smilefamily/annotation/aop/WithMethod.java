package cn.smilefamily.annotation.aop;

import cn.smilefamily.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithMethods.class)
public @interface WithMethod {
    @AliasFor("method")
    String value() default "";

    @AliasFor("value")
    String method() default "";

    Class<? extends Annotation> annotation() default None.class;

    @interface None {
    }
}
