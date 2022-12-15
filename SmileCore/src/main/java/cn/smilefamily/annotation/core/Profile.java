package cn.smilefamily.annotation.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Profile {
    String KEY = "smile.profile";
    //由于从jsonNode查询
    String KEY_PATH = "/smile/profile";
    String ACTIVE_KEY = "smile.profile.active";
    String TEST = "test";
    String DEV = "dev";
    String PROD = "prod";

    String value();
}
