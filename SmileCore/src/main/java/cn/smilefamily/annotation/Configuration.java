package cn.smilefamily.annotation;

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
    //指定扫描哪些包，这些包下的@Bean标注的类会被自动定义为bean
    String[] scanPackages();
    //指定引入的配置文件，配置文件的内容可以在@Value中使用
    String[] properties() default "";

    /**
     * config name 用于标识一个context，注意如果config是被导入其他config中，则该属性被忽略。
     * @return
     */
    String name() default "";
}
