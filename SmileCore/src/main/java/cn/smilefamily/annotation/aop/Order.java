package cn.smilefamily.annotation.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 数字越大其执行顺序越贴近被拦截的方法。即对before类advice，数字小的先执行。对于after类advice，数字大的先执行。
 */
public @interface Order {
    /**
     * @return 执行顺序
     */
    int value();
}
