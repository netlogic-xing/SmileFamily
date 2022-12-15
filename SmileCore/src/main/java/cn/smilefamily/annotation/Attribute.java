package cn.smilefamily.annotation;

public @interface Attribute {
    String name();

    String aliasFor() default "";

    String defaultValue() default "";

    String always() default "";
}
