package cn.smilefamily.annotation.aop;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Before {
    String value() default "";
}
