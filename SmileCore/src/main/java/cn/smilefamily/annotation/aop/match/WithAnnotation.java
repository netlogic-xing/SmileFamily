package cn.smilefamily.annotation.aop.match;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WithAnnotations.class)
public @interface WithAnnotation {
    //bean name
    Class<? extends Annotation> value();
}
