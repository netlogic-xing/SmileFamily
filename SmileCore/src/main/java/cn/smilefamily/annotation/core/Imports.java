package cn.smilefamily.annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
public @interface Imports {
    Import[] value();
}
