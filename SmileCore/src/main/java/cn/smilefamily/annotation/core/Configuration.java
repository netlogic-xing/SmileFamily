package cn.smilefamily.annotation.core;

import java.lang.annotation.*;

/**
 * 标注一个JavaConfig类,此注解类可被继承
 */
@Target({
        ElementType.TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Configuration {

    /**
     * config name 用于标识一个context，注意如果config是被导入其他config中，则该属性被忽略。
     *
     * @return
     */
    String value() default "";
}
