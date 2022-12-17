package cn.smilefamily.annotation.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@java.lang.annotation.Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetObject {
}
