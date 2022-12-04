package cn.smilefamily.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 标注一个方法将在bean成功创建后执行。注意，标注的方法可以有参数，但不算Bean依赖。
 */
public @interface PostConstruct {
}
