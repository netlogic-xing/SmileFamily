package cn.smilefamily.annotation.core;

import java.lang.annotation.*;

@Target({
        ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(ScanBeans.class)
//指定扫描哪些包，这些包下的@Bean标注的类会被自动定义为bean
public @interface ScanBean {
    String value();
}
