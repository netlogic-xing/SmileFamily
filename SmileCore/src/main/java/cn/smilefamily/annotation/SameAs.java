package cn.smilefamily.annotation;

import java.lang.annotation.Annotation;

public @interface SameAs {
    Class<? extends Annotation> value();

    Attribute[] attributes() default {};
}