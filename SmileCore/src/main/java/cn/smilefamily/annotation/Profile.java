package cn.smilefamily.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Profile {
    String PROFILE_KEY = "smile.profile";
    //由于从jsonNode查询
    String PROFILE_KEY_PATH = "/smile/profile";
    String ACTIVE_PROFILE_KEY = "smile.profile.active";
    String DEFAULT_PROFILE = "default";

    String value() default DEFAULT_PROFILE;
}
