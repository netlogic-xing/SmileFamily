package cn.smilefamily.annotation.core;

import java.lang.annotation.*;

@Target({
        ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface PropertySources {
    PropertySource[] value();
}