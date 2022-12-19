package cn.smilefamily.annotation.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 环绕执行。注意：Around Advice的执行导致order比其大的before/after advice被取消执行。
 * 所以，一般来说，仅有一个有效的around advice，其order应最大，才能保证其他advice能正确执行。
 */
public @interface Around {
    String value() default "";
}
