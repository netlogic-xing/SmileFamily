package cn.smilefamily.web.annotation;

import cn.smilefamily.annotation.Attribute;
import cn.smilefamily.annotation.SameAs;
import cn.smilefamily.annotation.core.Scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SameAs(value = Scope.class,
        attributes = {
                @Attribute(name = "value", always = Session.SESSION)
        })
public @interface Session {
    String SESSION = "session";
}
