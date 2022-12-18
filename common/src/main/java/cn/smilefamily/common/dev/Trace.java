package cn.smilefamily.common.dev;

import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Inherited
public @interface Trace {
}
