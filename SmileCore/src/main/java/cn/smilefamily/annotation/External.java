package cn.smilefamily.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 标注一个依赖项应该由其他模块提供
 */
public @interface External {
    //对外部依赖项的描述
    String value() default "";
}
