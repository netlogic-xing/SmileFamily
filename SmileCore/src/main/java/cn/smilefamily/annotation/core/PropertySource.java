package cn.smilefamily.annotation.core;

import java.lang.annotation.*;

@Target({
        ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(PropertySources.class)
//指定引入的配置文件，配置文件的内容可以在@Value中使用
public @interface PropertySource {
    String value();
}
