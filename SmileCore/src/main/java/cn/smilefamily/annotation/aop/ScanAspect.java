package cn.smilefamily.annotation.aop;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ScanAspects.class)
public @interface ScanAspect {
    String value();
}
